#  Grade Calculator — Desktop App

A Kotlin + Compose Multiplatform desktop application that reads a student marks spreadsheet, computes letter grades and GPAs, and exports a formatted report as either **Excel** or **PDF**.

---

##  Features

| Feature | Detail |
|---|---|
| Excel import | `.xlsx` and `.xls` via Apache POI |
| Grade engine | 12-tier grading scale (A+ → F) with 4.0 GPA |
| Excel export | Styled two-sheet workbook (Results + Summary) |
| PDF export | Paginated, colour-coded landscape PDF |
| Native GUI | Compose Multiplatform — no Electron, no browser |
| Unit tests | Full test suite for all core business logic |

---

##  Grading Scale

| Range | Grade | GPA |
|-------|-------|-----|
| 95 – 100 | A+ | 4.0 |
| 90 – 94.9 | A | 4.0 |
| 85 – 89.9 | A- | 3.7 |
| 80 – 84.9 | B+ | 3.3 |
| 75 – 79.9 | B | 3.0 |
| 70 – 74.9 | B- | 2.7 |
| 65 – 69.9 | C+ | 2.3 |
| 60 – 64.9 | C | 2.0 |
| 55 – 59.9 | C- | 1.7 |
| 50 – 54.9 | D+ | 1.3 |
| 45 – 49.9 | D | 1.0 |
| 0 – 44.9 | F | 0.0 |

---

##  Excel Input Format

The app expects the **first sheet** of the workbook to be structured as:

| Column A | Column B |
|----------|----------|
| Student Name | Mark (0–100) |
| Alice | 87.5 |
| Bob | 63 |

> **Row 1** is treated as a header and skipped automatically.

---

##  Project Structure

```
grade-calculator/
├── core/                            # Pure Kotlin business logic
│   └── src/main/kotlin/
│       ├── model/
│       │   ├── GradeLevel.kt        # Enum — all grade tiers + GPA points
│       │   ├── Student.kt           # Input data class
│       │   ├── GradeResult.kt       # Per-student output
│       │   └── ProcessingReport.kt  # Full class report
│       ├── service/                 # Interfaces (the public API)
│       │   ├── GradeCalculatorService.kt
│       │   ├── ExcelReaderService.kt
│       │   ├── ExcelExporterService.kt
│       │   └── PdfExporterService.kt
│       └── impl/                    # Concrete implementations
│           ├── GradeCalculatorServiceImpl.kt
│           ├── ExcelReaderServiceImpl.kt
│           ├── ExcelExporterServiceImpl.kt
│           └── PdfExporterServiceImpl.kt
│
├── app/                             # Compose Desktop UI
│   └── src/main/kotlin/
│       ├── Main.kt                  # Entry point
│       ├── App.kt                   # Root composable + all screens
│       ├── AppViewModel.kt          # State management
│       └── ui/components/
│           └── Components.kt        # Reusable UI components
│
├── build.gradle.kts                 # Root Gradle config
├── settings.gradle.kts
└── README.md
```

---

##  Local Setup & Run

### Prerequisites

| Tool | Minimum Version | Check |
|------|----------------|-------|
| JDK  | 17             | `java -version` |
| Git  | any            | `git --version` |

> No Gradle installation needed — the project uses the **Gradle wrapper** (`gradlew`).

### 1 — Clone the Repository

```bash
git clone https://github.com/<your-username>/grade-calculator.git
cd grade-calculator
```

### 2 — Run the App

```bash
# macOS / Linux
./gradlew :app:run

# Windows
gradlew.bat :app:run
```

Gradle will download all dependencies on the first run (requires internet access).

### 3 — Run the Tests

```bash
./gradlew :core:test
```

Test reports are written to `core/build/reports/tests/test/index.html`.

---

##  Building a Native Installer

Compose Multiplatform can package the app as a native installer for the target OS.

### macOS — `.dmg`

```bash
./gradlew :app:packageDmg
# Output: app/build/compose/binaries/main/dmg/GradeCalculator-1.0.0.dmg
```

### Windows — `.msi`

```bash
gradlew.bat :app:packageMsi
# Output: app\build\compose\binaries\main\msi\GradeCalculator-1.0.0.msi
```

### Linux — `.deb`

```bash
./gradlew :app:packageDeb
# Output: app/build/compose/binaries/main/deb/gradecalculator_1.0.0_amd64.deb
```

>  **Cross-compilation is not supported.** You must build each installer on the target OS (e.g. `.msi` can only be built on Windows).

---

##  GitHub Actions — CI/CD

Create `.github/workflows/ci.yml` to automatically run tests on every push:

```yaml
name: CI

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - name: Run tests
        run: ./gradlew :core:test
      - name: Upload test report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-report
          path: core/build/reports/tests/test/
```

### Multi-platform Builds (optional)

To build native installers for all three platforms automatically on every release tag:

```yaml
name: Release Build

on:
  push:
    tags: ['v*']

jobs:
  build:
    strategy:
      matrix:
        include:
          - os: ubuntu-latest
            task: packageDeb
            artifact: "*.deb"
          - os: windows-latest
            task: packageMsi
            artifact: "*.msi"
          - os: macos-latest
            task: packageDmg
            artifact: "*.dmg"

    runs-on: ${{ matrix.os }}

    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - name: Build installer
        run: ./gradlew :app:${{ matrix.task }}
      - name: Upload installer
        uses: actions/upload-artifact@v4
        with:
          name: installer-${{ matrix.os }}
          path: app/build/compose/binaries/main/**/${{ matrix.artifact }}
```

---

##  Extending the App

### Swap the Grading Scale

Edit the `GradeLevel` enum in `core/src/main/kotlin/model/GradeLevel.kt`. Each entry defines its own `minMark` / `maxMark`, so no other file needs to change.

### Add a New Export Format (e.g. CSV)

1. Create `service/CsvExporterService.kt` (interface).
2. Create `impl/CsvExporterServiceImpl.kt` (implementation).
3. Add `ExportFormat.CSV` to the enum in `AppViewModel.kt`.
4. Wire it into the `exportReport()` `when` branch.

### Add a Database Backend

Inject a repository interface into `AppViewModel`. The `core` module is entirely independent of the UI — no Compose imports — so a persistence layer can be added without touching any UI code.

---

##  Tech Stack

| Layer | Library | Version |
|-------|---------|---------|
| UI | Compose Multiplatform | 1.6.1 |
| Language | Kotlin JVM | 1.9.22 |
| Excel I/O | Apache POI | 5.2.5 |
| PDF generation | Apache PDFBox | 3.0.1 |
| Build tool | Gradle (Kotlin DSL) | wrapper |
| Testing | kotlin.test | bundled |

---

## Contributing

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/my-feature`.
3. Commit your changes: `git commit -m "feat: add my feature"`.
4. Push and open a Pull Request.

Please make sure `./gradlew :core:test` passes before submitting.

---

##  License

MIT License — see [LICENSE](LICENSE) for details.
