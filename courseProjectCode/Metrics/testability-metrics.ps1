$repoRoot = Join-Path $PSScriptRoot "../.." -Resolve

$output = Join-Path $PSScriptRoot "production-java-tests.csv"

$surefireReports = Get-ChildItem -Path $repoRoot -Recurse -File -Filter "TEST-*.xml" |
    Where-Object {
        $_.FullName -like "*\target\surefire-reports\TEST-*.xml"
    }
	
if (!$surefireReports) {
    throw "No Surefire reports found. Run '.\mvnw.cmd test jacoco:report' from the repository root first."
}

$reports = foreach ($report in $surefireReports) {
    $relativePath = [System.IO.Path]::GetRelativePath(
        $repoRoot,
        $report.FullName
    ).Replace("\", "/")

    $module = ($relativePath -split "/target/surefire-reports/", 2)[0]

    [xml]$xml = Get-Content $report.FullName -Raw

    $testSuite = $xml.testsuite

    [pscustomobject]@{
        Module   = $module
        Tests    = [int]$testSuite.tests
        Failures = [int]$testSuite.failures
        Errors   = [int]$testSuite.errors
        Skipped  = [int]$testSuite.skipped
    }
}

$groups = $reports | Group-Object Module

$result = foreach ($group in $groups) {
    $tests = [int](($group.Group | Measure-Object -Property Tests -Sum).Sum)
    $failures = [int](($group.Group | Measure-Object -Property Failures -Sum).Sum)
    $errors = [int](($group.Group | Measure-Object -Property Errors -Sum).Sum)
    $skipped = [int](($group.Group | Measure-Object -Property Skipped -Sum).Sum)

    $jacocoPath = Join-Path $repoRoot "$($group.Name)/target/site/jacoco/jacoco.xml"

    $coveragePct = $null

    if (Test-Path $jacocoPath) {
        [xml]$jacocoXml = Get-Content $jacocoPath -Raw

        $lineCounter = $jacocoXml.report.counter |
            Where-Object { $_.type -eq "LINE" } |
            Select-Object -First 1

        if ($lineCounter) {
            $covered = [int]$lineCounter.covered
            $missed = [int]$lineCounter.missed
            $totalLines = $covered + $missed

            if ($totalLines -gt 0) {
                $coveragePct = [math]::Round(
                    100 * $covered / $totalLines,
                    2
                )
            }
        }
    }

    [pscustomobject]@{
        Module                = $group.Name
        Suites                = $group.Count
        Cases                 = $tests
        Failures              = $failures
        Errors                = $errors
        Skipped               = $skipped
        CoveragePct           = $coveragePct
    }
}

$result |
    Sort-Object Module |
    Export-Csv $output -NoTypeInformation