# Metrics Collection

## Maintainability Metrics

### Scope

Maintainability metrics were collected over Java production source files located
under Maven `src/main/java` directories.

The `benchmarks` module, test sources, documentation, build output, and generated
artifacts were excluded.

### Tool

- cloc 2.10

### Metrics

- Lines of Code (LOC)
- Comment lines
- Comment density
- LOC per file

### Collection Procedure

Production Java source directories were identified with PowerShell:

```powershell
$productionJavaDirs = Get-ChildItem -Recurse -Directory -Filter java |
    Where-Object {
        $_.FullName -like "*\src\main\java" -and
        $_.FullName -notlike "*\benchmarks\src\main\java"
    } |
    Select-Object -ExpandProperty FullName
```    

The production Java metrics were collected with: 

```powershell
cloc $productionJavaDirs `
    --include-lang=Java `
    --by-file `
    --csv `
    --out="courseProjectCode\Metrics\production-java-cloc.csv"
```

### Baseline Results

The analyzed production Java scope contains:

- 3,586 Java source files
- 335,926 lines of code
- 116,702 comment lines
- 61,292 blank lines
- 513,920 total physical lines
- 22.7% overall comment density

The `server` module is the largest module, containing 294,639 lines of
production Java code and a comment density of 21.65%.

Per-file results are stored in `production-java-cloc.csv`.

Per-module results are stored in `production-java-modules.csv`.