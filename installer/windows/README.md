# MacroMachine Windows setup

One Inno Setup script, [`MacroMachine.iss`](MacroMachine.iss), builds `MacroMachine-Setup-X.Y.Z.exe`.

## What setup does

1. Finds WorldPainter: the previous MacroMachine target, then WorldPainter's uninstall record, then the usual folders. The user can always Browse.
2. Checks the Java that WorldPainter uses: `.install4j/pref_jre.cfg`, then a bundled `jre`, then `.install4j/inst_jre.cfg`, and reads `JAVA_VERSION` from that runtime's `release` file. Below 21 it stops with a message. If the version can't be found, it asks the user whether to install anyway. It never installs or changes Java.
3. Asks the user to close that WorldPainter if it is running (Retry/Cancel). It never closes WorldPainter itself.
4. Copies the plugin to `%APPDATA%\WorldPainter\plugins\MacroMachine-Plugin\Macro-Machine-Plugin-X.Y.Z.jar` and deletes older versions from that subfolder. The subfolder matters: WorldPainter checks the signature of every `*.jar` directly in `plugins\` (not in subfolders) and rejects the unsigned plugin as "not official or has been tampered with".
5. Adds one line to `worldpainter.vmoptions`: `-classpath/a <that JAR>`, written as the full path. The plugin is unsigned, so WorldPainter only loads it from the classpath. MacroMachine lines from earlier versions or locations are replaced; all other lines stay byte-for-byte the same. A one-time copy is kept as `worldpainter.vmoptions.macromachine-backup`.
6. Renames old `*macromachine*.jar` / `*macro-machine*.jar` files directly in `plugins\` to `*.ignored`, so they are disabled but not deleted.

Uninstall (Windows Settings → Apps → *MacroMachine for WorldPainter*) removes the JAR and that one line. Everything else stays, including user edits made after installing. The uninstaller is kept outside the WorldPainter folder, so it still works after WorldPainter was removed.

Setup starts without admin rights. Only if `worldpainter.vmoptions` is not writable (for example `C:\Program Files\WorldPainter`) does it explain why and restart itself once through UAC. If the user declines, nothing changes and they can click Install again.

Rerunning setup repairs or updates the install, for example after a WorldPainter update overwrote `worldpainter.vmoptions`. A setup log is written to `%TEMP%\Setup Log *.txt`.

## Build and test

```powershell
mvn --batch-mode --no-transfer-progress verify
$iscc = & .\installer\windows\get-inno-setup.ps1          # pinned Inno Setup 6.7.4, checksum-verified
& .\installer\windows\tests\run-tests.ps1 -IsccPath $iscc -PluginJar .\target\Macro-Machine-Plugin-0.4.20.jar -Version 0.4.20
& .\installer\windows\build-installer.ps1 -IsccPath $iscc -PluginJar .\target\Macro-Machine-Plugin-0.4.20.jar -Version 0.4.20
```

The test installs into a temporary fake WorldPainter (per-user, with its own AppId) and checks the following: Java 17 is blocked with nothing changed; install, repair and uninstall work; later user edits are kept; and uninstall works after WorldPainter was deleted. The release workflow runs the same test before publishing.

Not covered by the test (check manually once): the UAC restart for `Program Files`, and that WorldPainter really loads the plugin from the classpath line.
