$repoRoot = Join-Path $PSScriptRoot "../.." -Resolve

$clocOutput = Join-Path $PSScriptRoot "production-java-cloc.csv"

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