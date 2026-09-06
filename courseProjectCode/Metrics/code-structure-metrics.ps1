$repoRoot = Join-Path $PSScriptRoot "../.." -Resolve

$clocOutput = Join-Path $PSScriptRoot "production-java-cloc.csv"
$moduleOutput = Join-Path $PSScriptRoot "production-java-modules.csv"

$clocInstalled = Get-Command "cloc" -ErrorAction SilentlyContinue

if (!$clocInstalled) {
	throw "cloc is required. Install it and ensure it is on PATH."
}

$productionJavaDirs = Get-ChildItem -Path $repoRoot -Recurse -Directory -Filter java |
    Where-Object {
        $_.FullName -like "*\src\main\java" -and
        $_.FullName -notlike "*\benchmarks\src\main\java"
    } |
    Select-Object -ExpandProperty FullName
	
cloc $productionJavaDirs `
    --include-lang=Java `
    --by-file `
    --csv `
    --out="$clocOutput"
	
if ($LASTEXITCODE -ne 0) {
	throw "cloc failed."
}
	
$clocFiles = Import-Csv $clocOutput | 
	Where-Object { $_.language -eq "Java" } |
    ForEach-Object {
        $relativePath = [System.IO.Path]::GetRelativePath(
            $repoRoot,
            $_.filename
        ).Replace("\", "/")

        $module = ($relativePath -split "/src/main/java/", 2)[0]

        [pscustomobject]@{
            Module  = $module
            Blank   = [int]$_.blank
            Comments = [int]$_.comment
            LOC     = [int]$_.code
        }
    }
	
$groups = $clocFiles | Group-Object Module

$result = foreach ($group in $groups) {
    $blank = [int](($group.Group | Measure-Object -Property Blank -Sum).Sum)
    $comments = [int](($group.Group | Measure-Object -Property Comments -Sum).Sum)
    $loc = [int](($group.Group | Measure-Object -Property LOC -Sum).Sum)

    $totalLines = $blank + $comments + $loc

    if ($totalLines -gt 0) {
        $commentDensityPct = [math]::Round(
            ($comments / $totalLines) * 100,
            2
        )
    } else {
        $commentDensityPct = 0
    }

    [pscustomobject]@{
        Module            = $group.Name
        Files             = $group.Count
        Blank             = $blank
        Comments          = $comments
        LOC               = $loc
        TotalLines        = $totalLines
        CommentDensityPct = $commentDensityPct
    }
}

$result |
    Sort-Object -Property LOC -Descending |
    Export-Csv $moduleOutput -NoTypeInformation