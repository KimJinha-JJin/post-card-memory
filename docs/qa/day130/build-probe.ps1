param([Parameter(Mandatory=$true)][string]$Scratch)
$ErrorActionPreference = 'Stop'
$sdk = Join-Path $env:LOCALAPPDATA 'Android/Sdk'
$buildTools = Join-Path $sdk 'build-tools/37.0.0'
$androidJar = Join-Path $sdk 'platforms/android-37.0/android.jar'
if (Test-Path -LiteralPath "$Scratch/day130-probe.apk") { throw 'Refuse to overwrite existing APK' }
foreach ($asset in @('before.apk','after.apk','portrait.jpg','landscape.jpg','fur.png')) {
    if (!(Test-Path -LiteralPath "$Scratch/assets/$asset")) { throw "Missing asset: $asset" }
}
& javac -encoding UTF-8 -source 8 -target 8 -classpath $androidJar -d "$Scratch/classes" "$PSScriptRoot/ProbeActivity.java" "$PSScriptRoot/StickerExportProbe.java"
if ($LASTEXITCODE) { throw 'javac failed' }
$classFiles = @(Get-ChildItem -LiteralPath "$Scratch/classes" -Recurse -Filter '*.class' | ForEach-Object { $_.FullName })
& "$buildTools/d8.bat" --lib $androidJar --min-api 26 --output "$Scratch/dex" @classFiles
if ($LASTEXITCODE) { throw 'd8 failed' }
& "$buildTools/aapt2.exe" link -I $androidJar --manifest "$PSScriptRoot/AndroidManifest.xml" -A "$Scratch/assets" -o "$Scratch/probe-unsigned.apk"
if ($LASTEXITCODE) { throw 'aapt2 failed' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::Open("$Scratch/probe-unsigned.apk", 'Update')
try { [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,"$Scratch/dex/classes.dex",'classes.dex') | Out-Null } finally { $archive.Dispose() }
& "$buildTools/zipalign.exe" -p -f 4 "$Scratch/probe-unsigned.apk" "$Scratch/probe-aligned.apk"
if ($LASTEXITCODE) { throw 'zipalign failed' }
# Disposable key only for this new data-isolated package; never a production key.
& keytool -genkeypair -keystore "$Scratch/qa130-key.p12" -storetype PKCS12 -storepass isolated-qa-only -keypass isolated-qa-only -alias qa130 -keyalg RSA -keysize 2048 -validity 365 -dname 'CN=Day130 Isolated Export QA' -noprompt
if ($LASTEXITCODE) { throw 'keytool failed' }
& "$buildTools/apksigner.bat" sign --ks "$Scratch/qa130-key.p12" --ks-key-alias qa130 --ks-pass pass:isolated-qa-only --key-pass pass:isolated-qa-only --out "$Scratch/day130-probe.apk" "$Scratch/probe-aligned.apk"
if ($LASTEXITCODE) { throw 'signing failed' }
& "$buildTools/apksigner.bat" verify --print-certs "$Scratch/day130-probe.apk"
if ($LASTEXITCODE) { throw 'signature verification failed' }
Get-FileHash -Algorithm SHA256 -LiteralPath "$Scratch/day130-probe.apk"
