# diagnostic: grades the sample once per worker count and reports time, scores and timeouts
# usage (from the repo folder, after compile.bat):
#   powershell -NoProfile -ExecutionPolicy Bypass -File diag-workers.ps1
# optional: -Workers 1,2,6,10
param([int[]]$Workers = @(1, 2, 6, 10))

$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot
$jar = 'target\auto-grading-system-1.0-SNAPSHOT.jar'
$expected = @('20.00', '11.50', '10.50', '9.88', '0.00', '0.50')
$out = 'diag-workers-result.txt'

if (-not (Test-Path $jar)) { Write-Error "$jar not found. Put this script in the repo folder and run compile.bat first." }
if (-not (Test-Path 'config.properties')) { Write-Error 'config.properties not found. Put this script in the repo folder.' }

function Log([string]$line) { Write-Host $line; Add-Content -Path $out -Value $line }
Set-Content -Path $out -Value ''

Log "date: $(Get-Date -Format s)"
Log "os: $([Environment]::OSVersion.VersionString)"
Log "logical processors: $([Environment]::ProcessorCount)"
Log "cpu: $((Get-CimInstance Win32_Processor | Select-Object -First 1).Name)"
Log "folder: $PSScriptRoot"
$java = (cmd /c 'java -version 2>&1') -join ' | '
Log "java: $java"
$commit = (cmd /c 'git log -1 --oneline 2>&1') -join ' '
Log "commit: $commit"
Log ''

$baseline = $null
foreach ($n in $Workers) {
    Log "=== workers $n ==="
    $before = @(Get-ChildItem 'build\grading' -Directory -Filter 'run-*' -ErrorAction SilentlyContinue | ForEach-Object FullName)
    $log = "diag-workers-$n.log"
    $sw = [Diagnostics.Stopwatch]::StartNew()
    $p = Start-Process -FilePath 'java' -ArgumentList @('-jar', $jar, '--workers', "$n", 'config.properties') `
        -NoNewWindow -Wait -PassThru -RedirectStandardOutput $log -RedirectStandardError "$log.err"
    $sw.Stop()
    Log ("exit code: {0}  wall time: {1:N1} s" -f $p.ExitCode, $sw.Elapsed.TotalSeconds)
    Get-Content "$log.err" | Where-Object { $_ -match 'worker|cap' } | ForEach-Object { Log "stderr: $_" }

    $run = Get-ChildItem 'build\grading' -Directory -Filter 'run-*' |
        Where-Object { $before -notcontains $_.FullName } |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $run) { Log 'no new run folder found; see the log files'; Log ''; continue }
    Log "run folder: $($run.FullName)"

    $scores = @(Import-Csv (Join-Path $run.FullName 'scorereport.csv') |
        ForEach-Object { $_.'Calculated Final Grade Numerator' })
    $match = (($scores -join ',') -eq ($expected -join ','))
    Log "scores: $($scores -join ', ')  expected: $($expected -join ', ')  match: $match"

    $cases = @(Import-Csv (Join-Path $run.FullName 'cases.csv'))
    $timedOut = @($cases | Where-Object { $_.Status -eq 'TIMED_OUT' } | ForEach-Object { "$($_.Username) $($_.Case)" })
    Log "cases: $($cases.Count)  TIMED_OUT: $($timedOut.Count)"

    $verdicts = @{}
    foreach ($c in $cases) { $verdicts["$($c.Username) $($c.Case)"] = $c.Status }
    if ($null -eq $baseline) {
        $baseline = $verdicts
        Log "(baseline for verdict comparison: workers $n)"
    } else {
        $changed = @($verdicts.Keys | Where-Object { $baseline[$_] -ne $verdicts[$_] } | Sort-Object)
        Log "verdicts changed vs baseline: $($changed.Count)"
        foreach ($k in $changed) { Log "  $k : $($baseline[$k]) -> $($verdicts[$k])" }
    }
    Log ''
}
Log "done. send $out to Boey"
