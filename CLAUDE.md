# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build the project
./gradlew build

# Build without tests
./gradlew build -x test

# Run the application (opens Swing UI window)
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.vva.buildings.InputValidatorTest"

# Force re-run tests (bypass UP-TO-DATE cache)
./gradlew cleanTest test
```

The app reads Excel files from `data/input/` and writes CSV files to `data/output/`. Both directories must exist before running.

## Architecture Overview

**Buildings** is a Spring Boot Kotlin batch application that transforms Ukrainian municipal building registry data from Excel into normalized CSV exports. There is no REST API and no database — it's a pure data pipeline triggered via a Swing GUI (`MainFrame`).

### Entry Point & UI

`main()` in `BuildingsApplication.kt` starts the Spring context, then opens a `MainFrame` (Swing). The **Старт** button calls `BuildingsApplication.process()` on a background thread. Log output is piped to the UI via `TextAreaAppender` (a custom Logback appender). Logback config is in `logback-spring.xml` with `additivity="false"` on the `com.vva.buildings` logger to prevent duplicate log lines.

### Data Flow

`BuildingsApplication.process()` orchestrates three sequential steps:

1. **Balans** (`Balans.kt`) — reads `Баланс.xlsx`, validates headers, builds `tabBuildings: Map<String, List<String>>` keyed by building ID
2. **FreeSpace** (`FreeSpace.kt`) — reads `ВільніПлощі.xlsx`, validates headers, cross-references `tabBuildings`, returns `FreeSpaceData(prozorro, buildings)`
3. **Orenda** (`Orenda.kt`) — reads `Оренда.xlsx`, validates headers, cross-references `tabBuildings`, returns `OrendaData(list, prozorro)`

All processors are Kotlin `object` singletons (not Spring beans). State is returned as data classes — no shared mutable fields.

### Outputs (5 CSV files in `data/output/`)

| File | Source | Content |
|---|---|---|
| `buildings.csv` | Balans | Full building directory (excl. dest. group 634) |
| `buildingsRentable.csv` | FreeSpace | Free spaces without Prozorro code |
| `listProzorroSales_buildingsRentable.csv` | FreeSpace | Free spaces with Prozorro auction code |
| `list.csv` | Orenda | Active rental contracts |
| `listProzorroSales_rented.csv` | Orenda | Active rentals with Prozorro auction code |

### Domain Model

Each source file has a paired enum defining its column indices:

- `BalansIndex` (19 fields) — column positions in `Баланс.xlsx`
- `FreeSpaceIndex` (13 fields) — column positions in `ВільніПлощі.xlsx`
- `OrendaIndex` (19 fields) — column positions in `Оренда.xlsx`
- `BuildingIndex` (36 fields) — canonical in-memory building record shared across all three processors

When Excel columns shift, update the relevant `*Index` enum and `InputValidator` together.

### Input Validation (`InputValidator.kt`)

Called at the start of each file's processing. Reads row index 1 (second row) and compares each cell against hardcoded expected header strings. Throws `IllegalStateException` on mismatch, which surfaces as "Помилка!" in the UI.

**Important:** `ВільніПлощі.xlsx` has merged cells in row 1, so only 6 non-empty columns are validated. `Оренда.xlsx` column `[1]` uses Unicode U+2019 (`'` right single quotation mark) instead of U+0027 — this is already encoded correctly in the source.

### Key Filtering Rules

- **Exclude balance holder ID `22991617`** from all outputs (hardcoded in `Balans.kt`)
- **Exclude field of activity `Невизначені`** from `buildings.csv`
- **Destination group `634`**: excluded from `buildings.csv` but used as filter in FreeSpace and Orenda via `Utils.is634()` / `Utils.is634m()`
- **Orenda**: only rows with `contractStatus == "Договір діє"` and `validityDate >= "2020-01-01"` are included
- **`Utils.isBalanceHolderClosed()`** — hardcoded set of 10 closed Kyiv utility entities (defined but not yet applied in processing)

### Key Utilities (`Utils.kt`)

| Function | Purpose |
|---|---|
| `setQuotation()` / `getQuotationString()` | CSV escaping — doubles internal `"`, wraps in quotes, returns `null` for blank |
| `getCurrencyValue()` | Formats numeric cell as `#,##0.00` with `.` decimal |
| `getDt8601()` | Converts Excel date cell → ISO 8601 string; returns `"null"` for blank |
| `formatToId()` | Formats numeric cell as integer string (no decimal) |
| `getNotPrivate()` | Masks 10-digit IDs as `XXXXXXXXXX`; longer IDs (legal entities) pass through |
| `getEtcCode()` / `getUrl()` | Normalises Prozorro codes; LL/UA prefix → auction URL, RG → planning URL |
| `isNotKyiv()` | True if district is not in `kyivDistricts` list (10 Kyiv districts) |

### Logging

`logback-spring.xml` routes `com.vva.buildings` (INFO+) to both FILE (`logs/buildings.log`, rolling 10 MB/10 days) and SWING appender. Root logger is WARN-only to FILE. Use SLF4J `LoggerFactory` directly — Lombok is a `compileOnly` dependency, so `@Slf4j` works in Java but not in Kotlin.
