$repoRoot = Join-Path $PSScriptRoot "../.." -Resolve

$clocOutput = Join-Path $PSScriptRoot "production-java-cloc.csv"

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
	
$clocFiles = Import-Csv $clocOutput | Where-Object { $_.language -eq "Java" }

$result = foreach ($file in $clocFiles) {
	$comment = [int]$file.comment
	$code = [int]$file.code
	$blank = [int]$file.blank
	
	$totalLines = ($comment + $code + $blank)
	
	if ($totalLines -gt 0) {
		$commentDensity = (100 * $comment) / ($totalLines)
	} else {
		$commentDensity = 0
	}
	
	[pscustomobject]@{
		filename = $file.filename
		comment = $comment
		code = $code
		commentDensityPercent = [math]::Round($commentDensity, 2)
	}
}

$densityOutput = Join-Path $PSScriptRoot "production-java-density.csv"

$result | Export-Csv $densityOutput -NoTypeInformation