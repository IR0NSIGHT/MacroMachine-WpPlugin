; MacroMachine for WorldPainter - Windows setup.
;
; What it does:
;   1. Find WorldPainter (or let the user browse to it).
;   2. Check the Java version WorldPainter uses (block if < 21, warn if unknown).
;   3. Copy the plugin to %APPDATA%\WorldPainter\plugins\MacroMachine-Plugin\Macro-Machine-Plugin-X.Y.Z.jar.
;      The subfolder matters: WorldPainter checks the signature of every *.jar
;      directly in plugins\ and rejects this unsigned one ("not official").
;   4. Add one line to worldpainter.vmoptions:  -classpath/a <that JAR>
;      (the plugin is unsigned, so WorldPainter only loads it from the classpath).
;   5. Rename other/old MacroMachine JARs in the plugins folder to *.ignored.
; Uninstall removes the JAR and that one vmoptions line. Nothing else is touched.
;
; Setup runs without admin rights. Only if worldpainter.vmoptions is not
; writable (e.g. Program Files) does it restart itself once with UAC.

#ifndef AppVersion
  #error AppVersion must be supplied by build-installer.ps1
#endif
#ifndef PluginJar
  #error PluginJar must be supplied by build-installer.ps1
#endif
#ifndef OutputDirectory
  #define OutputDirectory "."
#endif
; Tests override these so they never touch a real MacroMachine registration.
#ifndef AppGuid
  #define AppGuid "50D4657B-C92A-4C2A-9D6D-C136EB0B42A9"
#endif
#ifndef UninstallDir
  #define UninstallDir "{autoappdata}\MacroMachine"
#endif

[Setup]
AppId={{{#AppGuid}}
AppName=MacroMachine
AppVersion={#AppVersion}
AppPublisher=IR0NSIGHT
AppPublisherURL=https://github.com/IR0NSIGHT/MacroMachine-WpPlugin
UninstallDisplayName=MacroMachine for WorldPainter
VersionInfoVersion={#AppVersion}.0
VersionInfoProductVersion={#AppVersion}.0
; {app} is the WorldPainter folder itself.
DefaultDirName={code:DefaultWorldPainterDir}
UsePreviousAppDir=no
AppendDefaultDirName=no
DirExistsWarning=no
DisableDirPage=no
DisableWelcomePage=yes
DisableProgramGroupPage=yes
DisableReadyPage=yes
; Keep the uninstaller outside WorldPainter's folder so it survives a
; WorldPainter reinstall/removal and can always clean up its registration.
UninstallFilesDir={#UninstallDir}
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=commandline
; Always start without admin rights, even when updating an install that needed them.
; Otherwise setup could start elevated as another account and use that account's plugins folder.
UsePreviousPrivileges=no
; Never offer to close WorldPainter for the user (unsaved worlds).
CloseApplications=no
SetupLogging=yes
WizardStyle=modern
; Generated from imgs/MacroMachine_Icon.png by make-icons.ps1.
SetupIconFile=MacroMachine.ico
WizardSmallImageFile=wizard-small.png
WizardImageFile=wizard-large.png
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
OutputDir={#OutputDirectory}
OutputBaseFilename=MacroMachine-Setup-{#AppVersion}
Compression=lzma2
SolidCompression=yes

[Messages]
WizardSelectDir=WorldPainter
SelectDirDesc=Where is WorldPainter installed?
SelectDirBrowseLabel=WorldPainter folder. If this is wrong, click Browse and select the folder that contains worldpainter.exe. Then click Install.
FinishedLabel=MacroMachine is installed. Start WorldPainter as usual; MacroMachine is loaded automatically.
FinishedLabelNoIcons=MacroMachine is installed. Start WorldPainter as usual; MacroMachine is loaded automatically.

[Files]
Source: "{#PluginJar}"; DestDir: "{code:GetJarDir}"; DestName: "Macro-Machine-Plugin-{#AppVersion}.jar"; Flags: ignoreversion

[InstallDelete]
; Previous versions installed by this setup.
Type: files; Name: "{code:GetJarDir}\Macro-Machine-Plugin-*.jar"

[Run]
Filename: "{app}\worldpainter.exe"; Description: "Open WorldPainter"; Flags: postinstall nowait skipifsilent runasoriginaluser; Check: not IsElevatedRelaunch
; After the UAC restart, let the (unelevated) Explorer start WorldPainter so it
; does not run as administrator.
Filename: "{win}\explorer.exe"; Parameters: """{app}\worldpainter.exe"""; Description: "Open WorldPainter"; Flags: postinstall nowait skipifsilent; Check: IsElevatedRelaunch

[Code]
const
  RequiredJava = 21;
  VmOptionsName = 'worldpainter.vmoptions';
  JarSubfolder = 'MacroMachine-Plugin';
  JarName = 'Macro-Machine-Plugin-{#AppVersion}.jar';

var
  ElevatedRelaunch: Boolean;
  ClosingForRelaunch: Boolean;
  PluginsDir: String;

function ShellExecute(Wnd: HWND; Operation, FileName, Parameters, Directory: String;
  ShowCmd: Integer): Integer;
  external 'ShellExecuteW@shell32.dll stdcall';

function GetJarDir(Param: String): String;
begin
  Result := AddBackslash(PluginsDir) + JarSubfolder;
end;

{ Old plugin JARs are recognised by name, e.g. Macro-Machine-Plugin-0.4.17.jar or MacroMachine.jar. }
function IsMacroMachineJarName(const FileName: String): Boolean;
var
  Name: String;
begin
  Name := Lowercase(FileName);
  Result := (ExtractFileExt(Name) = '.jar') and
    ((Pos('macromachine', Name) > 0) or (Pos('macro-machine', Name) > 0));
end;

function IsElevatedRelaunch: Boolean;
begin
  Result := ElevatedRelaunch;
end;

{ ---------- WorldPainter detection ---------- }

function IsWorldPainterDir(const Dir: String): Boolean;
begin
  Result := (Dir <> '') and FileExists(AddBackslash(Dir) + 'worldpainter.exe');
end;

function FromRegistry(const Root: Integer; const Key, Value: String): String;
begin
  if not RegQueryStringValue(Root, Key, Value, Result) then Result := '';
  Result := RemoveBackslashUnlessRoot(Trim(Result));
  if not IsWorldPainterDir(Result) then Result := '';
end;

function FromUninstallRecords(const Root: Integer): String;
var
  Names: TArrayOfString;
  I: Integer;
  Base, Name: String;
begin
  Result := '';
  Base := 'Software\Microsoft\Windows\CurrentVersion\Uninstall';
  if not RegGetSubkeyNames(Root, Base, Names) then Exit;
  for I := 0 to GetArrayLength(Names) - 1 do
    if RegQueryStringValue(Root, Base + '\' + Names[I], 'DisplayName', Name) and
       (Pos('worldpainter', Lowercase(Trim(Name))) = 1) then begin
      Result := FromRegistry(Root, Base + '\' + Names[I], 'InstallLocation');
      if Result <> '' then Exit;
    end;
end;

function DefaultWorldPainterDir(Param: String): String;
var
  OwnKey: String;
begin
  { 1. Where MacroMachine was installed before. }
  OwnKey := 'Software\Microsoft\Windows\CurrentVersion\Uninstall\{' + '{#AppGuid}' + '}_is1';
  Result := FromRegistry(HKCU, OwnKey, 'Inno Setup: App Path');
  if Result = '' then Result := FromRegistry(HKLM64, OwnKey, 'Inno Setup: App Path');
  { 2. WorldPainter's own uninstall record. }
  if Result = '' then Result := FromUninstallRecords(HKLM64);
  if Result = '' then Result := FromUninstallRecords(HKLM32);
  if Result = '' then Result := FromUninstallRecords(HKCU);
  { 3. Usual locations. The fallback does not need to exist; Next validates it. }
  if Result = '' then begin
    Result := ExpandConstant('{localappdata}\Programs\WorldPainter');
    if not IsWorldPainterDir(Result) then Result := ExpandConstant('{commonpf64}\WorldPainter');
  end;
end;

{ ---------- Java check ---------- }

function FirstLine(const FileName: String): String;
var
  Lines: TArrayOfString;
begin
  Result := '';
  if LoadStringsFromFile(FileName, Lines) and (GetArrayLength(Lines) > 0) then
    Result := RemoveBackslashUnlessRoot(RemoveQuotes(Trim(Lines[0])));
end;

{ Same order the install4j launcher uses: preferred JRE, bundled JRE, JRE found at install time. }
function WorldPainterJavaHome(const Dir: String): String;
begin
  Result := FirstLine(AddBackslash(Dir) + '.install4j\pref_jre.cfg');
  if (Result = '') and FileExists(AddBackslash(Dir) + 'jre\release') then
    Result := AddBackslash(Dir) + 'jre';
  if Result = '' then Result := FirstLine(AddBackslash(Dir) + '.install4j\inst_jre.cfg');
end;

function JavaVersionOf(const Home: String): String;
var
  Lines: TArrayOfString;
  I: Integer;
  Line: String;
begin
  Result := '';
  if (Home = '') or not LoadStringsFromFile(AddBackslash(Home) + 'release', Lines) then Exit;
  for I := 0 to GetArrayLength(Lines) - 1 do begin
    Line := Trim(Lines[I]);
    if Pos('JAVA_VERSION=', Line) = 1 then begin
      Result := RemoveQuotes(Copy(Line, 14, Length(Line)));
      Exit;
    end;
  end;
end;

{ '21.0.6' -> 21, '1.8.0_402' -> 8, '' -> 0 }
function JavaMajor(Version: String): Integer;
var
  I: Integer;
begin
  if Copy(Version, 1, 2) = '1.' then Delete(Version, 1, 2);
  I := 1;
  while (I <= Length(Version)) and (Version[I] >= '0') and (Version[I] <= '9') do I := I + 1;
  Result := StrToIntDef(Copy(Version, 1, I - 1), 0);
end;

{ Returns an error message if WorldPainter uses Java older than 21.
  Unknown is set when the version could not be determined. }
function JavaProblem(const Dir: String; var Unknown: Boolean): String;
var
  Home, Version: String;
begin
  Result := '';
  Home := WorldPainterJavaHome(Dir);
  Version := JavaVersionOf(Home);
  Unknown := JavaMajor(Version) = 0;
  Log('WorldPainter Java: home="' + Home + '" version="' + Version + '"');
  if not Unknown and (JavaMajor(Version) < RequiredJava) then
    Result := 'WorldPainter uses Java ' + Version + ':' + #13#10 + Home + #13#10#13#10 +
      'MacroMachine needs Java 21 or newer. Install a newer Java (for example from adoptium.net), ' +
      'then run this setup again. If WorldPainter still uses the old Java afterwards, reinstall WorldPainter.';
end;

{ ---------- Environment checks ---------- }

{ True if worldpainter.exe from Dir is running. A process whose path Windows
  will not tell us (e.g. elevated) counts as running, to be safe. }
function WorldPainterRunning(const Dir: String): Boolean;
var
  Locator, Service, Found: Variant;
  I: Integer;
  Path: String;
begin
  Result := False;
  try
    Locator := CreateOleObject('WbemScripting.SWbemLocator');
    Service := Locator.ConnectServer('.', 'root\CIMV2');
    Found := Service.ExecQuery('SELECT ExecutablePath FROM Win32_Process WHERE Name = ''worldpainter.exe''');
    for I := 0 to Found.Count - 1 do begin
      Path := '';
      if not VarIsNull(Found.ItemIndex(I).ExecutablePath) then Path := Found.ItemIndex(I).ExecutablePath;
      if (Path = '') or (CompareText(Path, AddBackslash(Dir) + 'worldpainter.exe') = 0) then begin
        Result := True;
        Exit;
      end;
    end;
  except
    Log('Could not check whether WorldPainter is running: ' + GetExceptionMessage);
  end;
end;

function CanWrite(const Dir: String): Boolean;
var
  Probe: String;
begin
  Probe := AddBackslash(Dir) + 'macromachine-setup.tmp';
  Result := SaveStringToFile(Probe, '', False) and DeleteFile(Probe);
  { Appending nothing checks write access without changing the file. }
  if Result and FileExists(AddBackslash(Dir) + VmOptionsName) then
    Result := SaveStringToFile(AddBackslash(Dir) + VmOptionsName, '', True);
end;

{ Admin rights did not help, so asking for them again would loop. }
function CannotWriteAsAdmin(const Dir: String): String;
begin
  Result := 'Setup cannot change ' + AddBackslash(Dir) + VmOptionsName + '.' + #13#10#13#10 +
    'The file is read-only or open in another program. Close programs that may use it, ' +
    'or clear "Read-only" in the file''s Properties, then click Install again.';
end;

{ ---------- worldpainter.vmoptions ---------- }

{ A single-entry -classpath/a line for a MacroMachine JAR: ours, from any
  earlier version or location. Rewritten on every install, removed on uninstall. }
function IsOurLine(const Line: String): Boolean;
var
  S: String;
begin
  S := Trim(Line);
  Result := (Pos('-classpath/a', Lowercase(S)) = 1) and (Pos(';', S) = 0) and
    IsMacroMachineJarName(ExtractFileName(S));
end;

{ Works on the raw bytes so every other line stays byte-for-byte identical. }
function RemoveOurLines(const Text: AnsiString): AnsiString;
var
  Start, I: Integer;
  Line: AnsiString;
begin
  Result := '';
  Start := 1;
  while Start <= Length(Text) do begin
    I := Start;
    while (I <= Length(Text)) and (Text[I] <> #10) do I := I + 1;
    Line := Copy(Text, Start, I - Start + 1);
    if not IsOurLine(String(Line)) then Result := Result + Line;
    Start := I + 1;
  end;
end;

function EnableInVmOptions(const Dir: String): String;
var
  FileName: String;
  Text, Updated: AnsiString;
begin
  Result := '';
  FileName := AddBackslash(Dir) + VmOptionsName;
  Text := '';
  if FileExists(FileName) and not LoadStringFromFile(FileName, Text) then begin
    Result := 'Setup cannot read ' + FileName + '. Nothing was changed.';
    Exit;
  end;
  Updated := RemoveOurLines(Text);
  if (Updated <> '') and (Updated[Length(Updated)] <> #10) then Updated := Updated + #13#10;
  Updated := Updated + AnsiString('-classpath/a ' + AddBackslash(GetJarDir('')) + JarName) + #13#10;
  if Updated = Text then Exit;
  { One-time copy of the file as it was before MacroMachine ever touched it. }
  if FileExists(FileName) and not FileExists(FileName + '.macromachine-backup') then
    FileCopy(FileName, FileName + '.macromachine-backup', True);
  if SaveStringToFile(FileName, Updated, False) then
    Log('Added MacroMachine to ' + FileName)
  else
    Result := 'Setup cannot write ' + FileName + '. Nothing was changed.';
end;

{ ---------- Old plugin JARs ---------- }

function DisableOldJars: String;
var
  Find: TFindRec;
  Path, Target: String;
  N: Integer;
begin
  Result := '';
  if not DirExists(PluginsDir) then Exit;
  if FindFirst(AddBackslash(PluginsDir) + '*.jar', Find) then
    try
      repeat
        if ((Find.Attributes and FILE_ATTRIBUTE_DIRECTORY) = 0) and IsMacroMachineJarName(Find.Name) then begin
          Path := AddBackslash(PluginsDir) + Find.Name;
          Target := Path + '.ignored';
          N := 1;
          while FileExists(Target) do begin
            Target := Path + '.' + IntToStr(N) + '.ignored';
            N := N + 1;
          end;
          if RenameFile(Path, Target) then Log('Disabled ' + Path)
          else Result := Result + #13#10 + Path;
        end;
      until not FindNext(Find);
    finally
      FindClose(Find);
    end;
end;

{ ---------- Elevation (only when the WorldPainter folder is protected) ---------- }

procedure RelaunchElevated(const Dir: String);
var
  Params: String;
begin
  if MsgBox('WorldPainter is installed in a protected folder:' + #13#10 + Dir + #13#10#13#10 +
     'To enable MacroMachine, Setup must add one line to WorldPainter''s settings file ' +
     '(worldpainter.vmoptions) there. Windows will now ask for administrator permission.',
     mbInformation, MB_OKCANCEL) <> IDOK then Exit;
  Params := '/ALLUSERS /MMELEVATED=1 /DIR="' + Dir + '" /MMPLUGINS="' + PluginsDir + '"';
  if ShellExecute(WizardForm.Handle, 'runas', ExpandConstant('{srcexe}'), Params, '', SW_SHOWNORMAL) > 32 then begin
    ClosingForRelaunch := True;
    WizardForm.Close;
  end else
    MsgBox('Administrator permission was not given. Nothing was changed.' + #13#10 +
      'Click Install to try again.', mbError, MB_OK);
end;

{ ---------- Setup event functions ---------- }

function InitializeSetup: Boolean;
begin
  ElevatedRelaunch := ExpandConstant('{param:MMELEVATED|0}') = '1';
  { The elevated copy may run as a different (admin) account; it gets the
    original user's plugin folder from the unelevated copy. }
  PluginsDir := ExpandConstant('{param:MMPLUGINS|}');
  if PluginsDir = '' then PluginsDir := ExpandConstant('{userappdata}\WorldPainter\plugins');
  Result := True;
end;

procedure InitializeWizard;
var
  Delta: Integer;
begin
  WizardForm.SelectDirLabel.Caption :=
    'MacroMachine will be copied to your WorldPainter plugins folder:' + #13#10 + GetJarDir('') + #13#10#13#10 +
    'It will be enabled for the WorldPainter installed in the folder below. ' +
    'Older MacroMachine plugin files will be disabled.';
  { Grow the labels to fit and move the controls below them down. }
  Delta := WizardForm.AdjustLabelHeight(WizardForm.SelectDirLabel);
  WizardForm.SelectDirBrowseLabel.Top := WizardForm.SelectDirBrowseLabel.Top + Delta;
  Delta := Delta + WizardForm.AdjustLabelHeight(WizardForm.SelectDirBrowseLabel);
  WizardForm.DirEdit.Top := WizardForm.DirEdit.Top + Delta;
  WizardForm.DirBrowseButton.Top := WizardForm.DirBrowseButton.Top + Delta;
  { Inno's free-space note refers to the folder below, which gets no files. }
  WizardForm.DiskSpaceLabel.Visible := False;
end;

function ShouldSkipPage(PageID: Integer): Boolean;
begin
  { The elevated copy already got the folder (and the user's OK) from the first copy. }
  Result := ElevatedRelaunch and (PageID = wpSelectDir);
end;

procedure CancelButtonClick(CurPageID: Integer; var Cancel, Confirm: Boolean);
begin
  if ClosingForRelaunch then Confirm := False;
end;

function NextButtonClick(CurPageID: Integer): Boolean;
var
  Dir, Problem: String;
  Unknown: Boolean;
begin
  Result := True;
  if CurPageID <> wpSelectDir then Exit;
  Result := False;
  Dir := RemoveBackslashUnlessRoot(WizardDirValue);
  if not IsWorldPainterDir(Dir) then begin
    SuppressibleMsgBox('WorldPainter was not found in this folder:' + #13#10 + Dir + #13#10#13#10 +
      'Click Browse and select the folder that contains worldpainter.exe.', mbError, MB_OK, IDOK);
    Exit;
  end;
  while WorldPainterRunning(Dir) do
    if SuppressibleMsgBox('WorldPainter is running. Save your worlds, close WorldPainter, then click Retry.',
       mbInformation, MB_RETRYCANCEL, IDCANCEL) <> IDRETRY then Exit;
  Problem := JavaProblem(Dir, Unknown);
  if Problem <> '' then begin
    SuppressibleMsgBox(Problem, mbError, MB_OK, IDOK);
    Exit;
  end;
  if Unknown and (SuppressibleMsgBox('Setup could not find out which Java version WorldPainter uses. ' +
     'MacroMachine needs Java 21 or newer.' + #13#10#13#10 + 'Install anyway?',
     mbConfirmation, MB_YESNO, IDYES) <> IDYES) then Exit;
  if not CanWrite(Dir) then begin
    if IsAdmin then
      SuppressibleMsgBox(CannotWriteAsAdmin(Dir), mbError, MB_OK, IDOK)
    else
      RelaunchElevated(Dir);
    Exit;
  end;
  Result := True;
end;

{ Last check before anything changes. A message here stops setup with nothing installed. }
function PrepareToInstall(var NeedsRestart: Boolean): String;
var
  Dir: String;
  Unknown: Boolean;
begin
  Dir := RemoveBackslashUnlessRoot(ExpandConstant('{app}'));
  if not IsWorldPainterDir(Dir) then
    Result := 'WorldPainter was not found in ' + Dir + '. Run setup again and select the folder that contains worldpainter.exe.'
  else if WorldPainterRunning(Dir) then
    Result := 'WorldPainter is running. Close WorldPainter, then run setup again.'
  else begin
    Result := JavaProblem(Dir, Unknown);
    if (Result = '') and not CanWrite(Dir) then
      if IsAdmin then
        Result := CannotWriteAsAdmin(Dir)
      else
        Result := 'Setup cannot write to ' + Dir + '. Run setup again and allow administrator permission when Windows asks.';
    { A classpath entry for a JAR that is not there (yet) is ignored by Java,
      so editing this before copying the JAR cannot break WorldPainter. }
    if Result = '' then Result := EnableInVmOptions(Dir);
  end;
  if Result <> '' then Log('Stopped: ' + Result);
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  Failed: String;
begin
  if CurStep <> ssPostInstall then Exit;
  Failed := DisableOldJars;
  if Failed <> '' then
    SuppressibleMsgBox('MacroMachine is installed, but these old MacroMachine files could not be disabled:' +
      Failed + #13#10#13#10 + 'Delete them, otherwise WorldPainter loads MacroMachine twice.',
      mbError, MB_OK, IDOK);
end;

{ ---------- Uninstall ---------- }

function InitializeUninstall: Boolean;
begin
  Result := True;
  while WorldPainterRunning(RemoveBackslashUnlessRoot(ExpandConstant('{app}'))) do
    if SuppressibleMsgBox('WorldPainter is running. Save your worlds, close WorldPainter, then click Retry.',
       mbInformation, MB_RETRYCANCEL, IDCANCEL) <> IDRETRY then begin
      Result := False;
      Exit;
    end;
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var
  FileName: String;
  Text, Updated: AnsiString;
begin
  if CurUninstallStep <> usUninstall then Exit;
  { If WorldPainter is gone, there is nothing to clean; uninstall still succeeds. }
  FileName := ExpandConstant('{app}\') + VmOptionsName;
  if not FileExists(FileName) or not LoadStringFromFile(FileName, Text) then Exit;
  Updated := RemoveOurLines(Text);
  if Updated = Text then Exit;
  if SaveStringToFile(FileName, Updated, False) then
    Log('Removed MacroMachine from ' + FileName)
  else
    SuppressibleMsgBox('Could not remove MacroMachine from ' + FileName + '.' + #13#10 +
      'WorldPainter still starts normally.', mbInformation, MB_OK, IDOK);
end;
