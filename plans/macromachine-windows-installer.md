________________ DEVELOPER REQUIREMENTS: _______________________________________

# MacroMachine Windows installer

Status: implemented in simplified form, see installer/windows/README.md. The agent plan below is kept for reference only.

## Goal

Publish `MacroMachine-Setup-X.Y.Z.exe`, built with Inno Setup and containing the release's shaded plugin JAR. Users install it once and continue opening WorldPainter with their existing shortcuts and `.world` file associations.

### Story
AS A user with no technical skills
I WANT to download and install macromachine plugin into my worldpainter
AND the installation process has to be as easy as possible
BECAUSE i am not able to read documentation or follow textual instructions.

### Requirements

- Detect WorldPainter automatically; offer a folder picker when necessary.
- Put MacroMachine on WorldPainter's application classpath without modifying WorldPainter's JARs.
- Older MacroMachine plugin installations must be disabled or removed, they were not installed using the installer of this plan.
- the existence of java21 must be checked and the user must be told if its not 21 or later. its the users responsibilty to then act.
- Preserve unrelated JVM settings, plugins, files, shortcuts, and associations.
- Back up changes and support updates, repair by rerunning setup, and uninstall.
- Start unelevated and request administrator permission only when the selected installation requires it.
- Never install/download Java, change Java selection, or edit `JAVA_HOME`/`PATH`. Java 21+ is the user's responsibility.
- Report a confirmed incompatible WorldPainter runtime; do not mistake PATH Java for the runtime WorldPainter actually uses.
- Require WorldPainter to be closed. Do not kill it or risk unsaved worlds.
- Keep the installer as simple as possible.

________ AGENT PLAN: ___________________________________________________________

## Implementation scope

Windows only. Ship one Inno Setup EXE containing the final shaded JAR. Do not add a replacement WorldPainter launcher, background service, scheduled task, automatic updater, Java dependency for the installer, or general-purpose configuration framework.

Keep the story and requirements above as the product contract. "As easy as possible" means the supported happy path needs no documentation, command line, or file editing. Java setup, Windows security approval, and unavailable administrator credentials remain outside the installer's control. All blocked cases must explain the immediate next action inside the wizard, not send the user to a README to understand the failure.

### Supported happy path

1. User downloads and opens `MacroMachine-Setup-X.Y.Z.exe`.
2. Setup finds one WorldPainter installation, checks Java, and shows the detected target.
3. User clicks **Install**. Recognized legacy MacroMachine installs are automatically disabled as part of this operation, with that change disclosed on the review page.
4. Windows asks for administrator approval only if access to the selected installation requires it.
5. Setup finishes with **Open WorldPainter**. Existing shortcuts and `.world` associations continue working.

Only show extra choices when necessary: WorldPainter selection when several installations exist, Browse when detection fails, Retry when WorldPainter is running, or a prerequisite/access error. Use short messages and one clear primary action; no per-file migration decisions on the happy path.

## Existing contracts and proposed implementation API

### Existing repository/WorldPainter contracts

- `pom.xml` currently targets Java 21 and WorldPainter `2.26.3`; current plugin version is `0.4.18`.
- Payload: `target/Macro-Machine-Plugin-${project.version}.jar`, after shading. Do not package `original-*` artifacts.
- Descriptor: `org.pepsoft.worldpainter.plugins`; registered class: `org.ironsight.wpplugin.macromachine.MacroMachinePlugin`.
- WorldPainter's `WPPluginManager.initialise(UUID, WPContext)` discovers plugins through the system classloader.
- install4j's `worldpainter.vmoptions` supports `-classpath/a <classpath>`; append the plugin without replacing WorldPainter's classpath.
- `deploy-worldpainter.ps1` disables every plugin JAR and requires Maven. It is not the installer implementation and must not be reused for legacy cleanup.

### Minimal proposed files

```text
installer/windows/MacroMachine.iss       wizard, detection, editing, lifecycle
installer/windows/InstallerCore.iss      shared logic only where needed for tests
installer/windows/build-installer.ps1   validate final JAR/version; invoke compiler
installer/windows/tests/                fixtures and production-core test harness
.github/workflows/build-release.yml     build and publish installer
README.md                              small user-facing installation update
```

Start with these files, not separate application modules or a new runtime/helper program. Keep a shared include only to let tests execute the same logic as the installer.

Proposed Pascal contracts, to be implemented after the feasibility checks:

```pascal
type
  TWorldPainterInstallation = record
    InstallDir: String;
    VmOptionsPath: String;
    ManagedDir: String;
  end;

  TJavaCheckResult = record
    FoundJava21OrLater: Boolean;
    WorldPainterRuntimeKnown: Boolean;
    WorldPainterRuntimeMajor: Integer;
    Message: String;
  end;

function ValidateWorldPainter(const InstallDir: String): String;
function CheckJava(const Installation: TWorldPainterInstallation): TJavaCheckResult;
function NeedsElevation(const Installation: TWorldPainterInstallation): Boolean;
function IsLegacyMacroMachineJar(const FileName: String): Boolean;
function ApplyInstallation(): Boolean;
function RollBackInstallation(): Boolean;
function RemoveOwnedIntegration(): Boolean;

procedure InitializeWizard;
function PrepareToInstall(var NeedsRestart: Boolean): String;
procedure CurStepChanged(CurStep: TSetupStep);
function InitializeUninstall: Boolean;
procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
```

Inno hooks use `TSetupStep` and `TUninstallStep`. `PrepareToInstall` returns actionable errors; lifecycle hooks coordinate the file changes and their reversal. These are proposed interfaces, not existing repository code.

## 1. Detect WorldPainter and check prerequisites

- Look for an existing MacroMachine registration first, then WorldPainter uninstall records in HKCU/HKLM and both registry views, then standard install locations. Validate paths rather than executing registry command strings. Do not recursively search disks.
- Validate `worldpainter.exe` and a supported install4j layout. Target the selected installation's `worldpainter.vmoptions`.
- If no installation is found, show **WorldPainter not found** with Browse, Retry, and Exit. If a selected folder is unsupported, explain that without changing it. Do not install WorldPainter.
- If several installations exist, require an explicit selection; never silently pick one.
- Detect WorldPainter processes/file locks. Require closure before changing files; never kill the application or defer a partial installation until reboot.
- Support one selected WorldPainter installation per installer registration. Switching targets requires uninstalling the old integration first.

### Java check and user-visible outcomes

Check two separate facts: whether a suitable Java 21+ runtime exists, and whether WorldPainter is known to use a compatible runtime. Check WorldPainter's bundled/recorded runtime, standard Java registry entries, and relevant user environment locations. A runtime bundled with WorldPainter can satisfy the existence check. Validate candidates through their runtime metadata or a vetted version query; do not trust a registry version label or PATH alone.

| Result | Wizard behavior |
| --- | --- |
| No verified Java 21+ candidate found | Block changes; show **Java 21 or newer required**, offer recheck and an optional official download-page link. User installs it themselves. |
| Java 21+ exists, but WorldPainter is confirmed to use an older runtime | Block changes; explain **WorldPainter is using Java N; Java 21 or newer is required**, and offer Retry after the user fixes it. |
| Java 21+ exists; WorldPainter's selected runtime cannot be established | Show a short explicit warning before Install. Installation may proceed, but completion must say **Files installed; WorldPainter's Java version was not verified**. |
| WorldPainter is confirmed to use Java 21+ | Continue normally. Successful file installation still does not prove plugin loading. |

Do not use stale logs as proof of current runtime selection. Never download/install Java, edit runtime selection/cache files, change environment variables, or run Java-detection executables elevated when unelevated inspection suffices. Missing/old Java must not prevent uninstall.

## 2. Elevate only when required

- Start with `PrivilegesRequired=lowest` and allow explicit command-line scope overrides. Determine need from access to the actual selected config/payload locations, not administrator-group membership alone.
- Existing ownership/registration scope takes precedence over newly observed write permissions. Discover registrations across user/machine scope; never create a second registration because the current user or ACLs changed. Reuse the existing scope or stop with a clear error.
- Explain the reason for UAC immediately before requesting it. Canceling UAC leaves WorldPainter and plugin files unchanged.
- Preserve the original user's configuration location/identity through alternate-admin credentials. Legacy cleanup must not switch to the administrator's profile; optional WorldPainter launch must run as the original user.
- Revalidate resolved target paths and ownership metadata after elevation and before privileged writes. Reject unsafe junction/reparse-point redirection, swapped paths, or tampered handoff data. User-writable state is not authority for privileged writes.
- Never loosen ACLs. Do not scan or change other users' profiles. Explain that a protected machine-wide WorldPainter launcher is shared, while legacy cleanup covers the selected installation and original user's plugin directory.

### Required elevation feasibility check

Both Inno `Exec` and `ShellExec` prohibit starting Setup itself before installation starts. Do not assume `ShellExec('runas', ...)` from an early wizard page is supported.

First test a minimal two-pass design using the same setup EXE: an unelevated preflight pass starts the installation phase without registering/copying the plugin, then requests an elevated `/ALLUSERS` pass only if necessary. Ensure the bootstrap pass performs no product registration/configuration writes, cannot show false success, and validates any state passed to the privileged pass. For writable targets, continue without relaunch or UAC. Reuse the appropriate scope for existing installs/uninstalls.

This is a feasibility gate, not permission to add an elaborate launcher. If the same-EXE flow cannot meet the constraints safely, document the smallest viable alternative before implementing the full wizard. Test original-user launch/context explicitly; do not assume a self-elevated child retains it.

## 3. Install one stable payload and disable old installs

Use an installer-owned directory:

```text
<WorldPainter>/lib/macromachine/macromachine.jar
<WorldPainter>/lib/macromachine/<uninstaller and small ownership record>
<WorldPainter>/lib/macromachine/backups/<unique operation id>/...
```

Expected effective classpath addition:

```text
-classpath/a lib\macromachine\macromachine.jar
```

Verify path resolution through the real launcher, including launches from unrelated working directories. Use a correctly escaped absolute path only if a supported launcher requires it. Do not change WorldPainter JARs, shortcuts, or `.world` associations.

### Mandatory legacy migration

- Inspect the original user's WorldPainter plugin directory, recognized legacy locations inside the selected installation, and relevant classpath entries. This includes installations predating this installer, not just its own registrations.
- Recognize MacroMachine through its descriptor/class identity; known filenames are discovery hints, not sufficient authorization to disable an unrelated JAR. Test old descriptor formats against actual historical releases during feasibility.
- Inspect JAR metadata using verified Inno/archive facilities, not a dependency on users having JDK tools or PowerShell available to run commands.
- The review page discloses **Replace older MacroMachine installations** as part of Install. Do not offer to keep active duplicates: disable all positively identified old MacroMachine JARs and remove their obsolete classpath elements.
- Move recognized old JARs to unique backup locations outside scanned JAR folders; never delete them outright. Never disable unrelated plugins or touch macro/project/data directories.
- If a suspected active conflicting install cannot be identified or disabled safely, stop before making changes with a specific in-wizard explanation. Do not claim migration succeeded.

## 4. Safe classpath edits, backups, and rollback

- Preserve unrelated options, classpath elements, comments, blank lines, encoding/BOM, and line endings. Ensure the final newline required by install4j.
- Support verified common WorldPainter classpath syntax, including quoted paths, semicolon-separated entries, relative paths, and historical mixed-version entries. Merge/add exactly one managed reference; remove only positively identified old MacroMachine references.
- Do not build a universal install4j parser. Detect unsupported/ambiguous syntax, conflicting external includes, or unrecognized encodings and refuse safely rather than rewriting unrelated settings.
- Before changing anything, save the original VM options, old managed JAR if present, legacy files/original locations, and a small versioned ownership/change record. Record file absence and hashes where needed. Keep backups out of automatic uninstaller deletion.
- Stage the new payload/configuration; recheck that originals have not changed; apply changes with safe replacement. Verify the managed reference and payload before committing the install record.
- On any failure, reverse changes from this operation only. Explicitly handle custom Pascal file edits; Inno's own rollback does not automatically undo them.
- Keep one small pending-operation record so a rerun can detect/recover an interrupted install. Do not build a generalized transaction engine. If recovery cannot be completed safely, leave backups intact and display their location.

## 5. Update, repair, and uninstall

- Stable AppId, managed filename, install record, and registration scope. Check both scopes before install/update/uninstall, including runs by a different user with admin credentials.
- Update replaces the managed JAR, retains ownership history, and stays idempotent. Reject accidental downgrades clearly.
- Rerunning the same installer repairs missing payload/classpath integration and rechecks legacy conflicts. WorldPainter updates can overwrite VM options; repair is manual by rerunning setup, not a background feature.
- Uninstall requires WorldPainter closed and appropriate permission, but not Java 21.
- Remove only the managed element from the CURRENT configuration; never overwrite later user edits with an old backup. Remove an installer-created VM-options file only if no subsequent user content remains.
- If classpath cleanup cannot be done safely, stop before deleting the managed payload and explain the problem.
- Remove only owned installer/payload files. Keep user data and recovery backups. Do not automatically reactivate legacy plugins; that could cause duplicate loading or incompatibility. Remove an owned directory only when empty.
- Provide normal Windows Apps uninstall registration. No recursive deletion of WorldPainter's `lib`/plugins directories.

## 6. Build and publish

- Pin an Inno compiler version and verified installation source/checksum. Build with Java 21 on CI; end users need no Maven/JDK/compiler tools for setup.
- Build the shaded plugin once, validate its descriptor/version/resources, and embed exactly those bytes in the EXE. Compare SHA-256 with the JAR published alongside it; do not separately rebuild the embedded payload.
- Read Maven version once and pass version/payload/output paths to the build script. Keep installer display/numeric version fields consistent.
- Publish `MacroMachine-Setup-X.Y.Z.exe`, the same JAR, checksums, and existing default-macros ZIP on the existing release tag.
- Correct the current `build-release.yml` mismatch: it triggers on `push` but reads `github.event.workflow_run.*`. Use successful completed release-branch `Maven Test` runs, check out the tested SHA, and align conditions/concurrency/release metadata.
- Preserve the existing master version gate -> release update -> release-test dispatch -> build/publish chain. Do not introduce a competing tag workflow or bump the version as incidental scaffolding.
- Preserve the existing user changes to `README.md`; later add only narrowly scoped installer instructions. Do not modify `deploy-worldpainter.ps1` or plugin logic incidentally.

## Acceptance tests

Use synthetic fixtures and disposable Windows VMs, never the developer's actual WorldPainter without explicit permission.

| Scenario | Required result |
| --- | --- |
| Clean supported WorldPainter, Java 21+, one detected target | User downloads, installs, and launches normally without documentation, commands, or file editing; no unnecessary pages. |
| WorldPainter missing, several installs, or unsupported folder | In-wizard Browse/selection/retry or a safe stop; no wrong-target writes. |
| Java absent/too old | No plugin/configuration changes; correct short message and recheck action; no Java installation. |
| PATH Java 21 but WorldPainter uses 17 | Explain the actual host mismatch; do not report compatibility or install a startup-breaking plugin. |
| Java 21 exists but host runtime unknown | Explicit warning and unverified completion state; never claim plugin loaded. |
| Writable per-user WorldPainter | Install/update/uninstall without elevation. |
| Protected installation | Request elevation only when required; canceled UAC changes nothing; missing credentials fail clearly. |
| Alternate administrator credentials | Cleanup remains in the original user's profile, launch uses that user, no duplicate registration. |
| Changed permissions or existing registration in another scope | Retain/reconcile the existing scope or stop; never create competing ownership records. |
| Legacy MacroMachine before this installer, multiple versions/locations, renamed recognized JAR | All identified active old copies/references are disabled; one new version loads; unrelated plugins/data survive. |
| Ambiguous suspected legacy JAR or conflicting include | Refuse before mutation; no unsafe generic deletion/rewrite. |
| VM options with comments, memory flags, mixed quoted paths, encodings/newlines | Supported inputs preserve unrelated content; unsupported inputs fail without modification. |
| Install -> update -> repair -> uninstall | Exactly one managed reference while installed; no accumulated JAR versions; uninstall preserves later user edits and user data. |
| Missing Java at uninstall | Uninstall still works. |
| Running WorldPainter, locks, failed backup/write, concurrent edit | No forced termination or false success; rollback preserves the previous state. |
| Interrupted transaction | Rerun recovers or stops safely with backups intact. |
| Redirected target/tampered handoff or ownership record | Reject unsafe privileged writes. |
| Launch via existing shortcut and `.world` association, unrelated working directory | Real WorldPainter log shows exactly one expected MacroMachine version; smoke-test tools/preview for classpath conflicts. |
| WorldPainter update overwrites VM options | Rerunning setup restores the managed integration without overwriting new unrelated settings. |
| Release artifacts | EXE embeds the exact published JAR hash and correct version, built from the tested revision. |

Automate configuration/migration/lifecycle cases against the same Pascal core used in production; manually verify UAC, real launcher behavior, archive inspection, original-user context, and the nontechnical happy path. Run Maven verification for packaging/release changes. Do not advise disabling Windows security to run an unsigned build.

## Implementation order

1. **Feasibility gate:** minimal Inno EXE; test native classpath syntax, built-in JAR inspection/legacy recognition, Java discovery, and same-EXE conditional elevation/original-user handling in disposable environments. No full installer framework first.
2. **Safe core:** implement bounded configuration edits, mandatory legacy disabling, simple backups/ownership/rollback, and shared fixture tests.
3. **User flow/lifecycle:** wire automatic detection, minimal pages, prerequisite messages, install/update/repair/uninstall, conditional permission handling, and truthful completion states.
4. **Release/docs/acceptance:** pin compiler, integrate the existing release pipeline, publish matching artifacts, complete the acceptance matrix, and narrowly update documentation.

Done means the supported nontechnical happy path needs no external instructions; normal WorldPainter launching loads one correct MacroMachine version; older active installs are disabled; Java is checked but never installed/selected by setup; elevation is conditional; updates/repair are idempotent; and failed operations/uninstall preserve unrelated settings and user data.

## Mixed

### WorldPainter Plugin Installer — Unhappy Paths

#### Installation
- **WorldPainter not found:** Explain the requirement; allow manual path selection.
- **Multiple or nonstandard installations:** Let the user choose; validate the selected installation.
- **WorldPainter still running:** Ask the user to save and close it; offer Retry.
- **Insufficient permissions or disk space:** Stop safely and explain how to resolve the issue.
- **Installer blocked by security software:** Report the failure without claiming installation succeeded.

#### `.vmoptions`
- **File missing, read-only, or inaccessible:** Validate before editing; fail safely.
- **Custom settings present:** Preserve unrelated options and file formatting.
- **Required options already present:** Avoid duplicates.
- **Outdated or conflicting plugin options:** Detect and migrate only plugin-owned entries.
- **Invalid JAR path or JVM argument:** Validate paths and syntax before committing.
- **Write interrupted:** Use a temporary file and backup to prevent corruption.

#### Updates
- **Same version installed:** Offer repair or exit without changes.
- **Newer version installed:** Warn before downgrading.
- **Download interrupted or corrupted:** Verify the JAR before replacing anything.
- **Host or Java version incompatible:** Warn and block unsupported updates.
- **Old JAR remains:** Remove only known obsolete plugin files; avoid duplicate loading.
- **Concurrent installers:** Prevent simultaneous modifications.
- **JAR updated but `.vmoptions` edit fails:** Roll back both changes.

#### Uninstallation
- **Settings changed since installation:** Remove only safely identifiable plugin-owned options.
- **Options shared with another plugin:** Do not remove shared settings blindly.
- **JAR or configuration missing:** Handle gracefully without damaging other files.
- **WorldPainter running:** Require it to close before removal.

#### After Installation
- **WorldPainter fails to start:** Provide clear recovery or rollback instructions.
- **Plugin not detected or crashes:** Offer diagnostics and a repair option.
- **Wrong WorldPainter instance launched:** Show the installation path used.
- **Missing dependency or plugin conflict:** Report actionable diagnostic information.

## Dev specs
### Common Pitfalls to handle
1. **Hardcoded paths:** WorldPainter may be installed in a custom directory or have multiple installations.
2. **Non-idempotent operations:** Running the installer twice duplicates JVM arguments or leaves multiple JARs.
3. **Partial installations:** The JAR updates successfully but `.vmoptions` fails, leaving an inconsistent state.
4. **Unsafe configuration edits:** Overwriting `.vmoptions` destroys user customizations or breaks WorldPainter startup.
5. **Poor running-process detection:** Updating while WorldPainter is running can cause file conflicts or inconsistent behavior.
6. **No rollback mechanism:** A failed update leaves the user with a broken installation.
7. **Incorrect version comparison:** Comparing versions as strings can incorrectly treat 1.10 as older than 1.9.
8. **Unverified downloads:** Corrupted or tampered JARs may be installed.
9. **Fragile uninstall logic:** Removing shared JVM arguments or restoring old configuration files can break other plugins.
10. **Insufficient testing:** Everything works on the developer's machine but fails on clean or customized installations.

## Agent

### Minimum Safeguards
1. Validate paths, compatibility, permissions, and process state before changes.
2. Back up the existing JAR and `.vmoptions` configuration.
3. Make repeated installs safe and duplicate-free.
4. Apply JAR and configuration changes as one recoverable operation.
5. Verify results; report partial failures honestly.
6. Provide repair, uninstall, and a concise installation log.

## References

- [install4j VM options/classpath](https://ej-technologies.com/resources/install4j/help/doc/concepts/vmParameters.html)
- [WorldPainter advanced settings/update caveat](https://www.worldpainter.net/trac/wiki/AdvancedSettings)
- [Inno privilege overrides](https://jrsoftware.org/ishelp/topic_setup_privilegesrequiredoverridesallowed.htm)
- [Inno event hooks](https://jrsoftware.org/ishelp/topic_scriptevents.htm)
- [Inno ShellExec restrictions](https://jrsoftware.org/ishelp/topic_isxfunc_shellexec.htm)
