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
./gradlew test --tests "com.vva.buildings.ColumnResolverTest"

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

1. **Balans** (`Balans.kt`) — reads `Баланс.xlsx`, resolves columns by header, builds `tabBuildings: Map<String, List<String>>` keyed by building ID
2. **FreeSpace** (`FreeSpace.kt`) — reads `ВільніПлощі.xlsx`, resolves columns by header, cross-references `tabBuildings`, returns `FreeSpaceData(prozorro, buildings)`
3. **Orenda** (`Orenda.kt`) — reads `Оренда.xlsx`, resolves columns by header, cross-references `tabBuildings`, returns `OrendaData(list, prozorro)`

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

Each source file has a paired enum mapping logical fields to **expected header text** (not fixed column positions):

- `BalansIndex` (19 fields) — `header` = column name in `Баланс.xlsx`
- `FreeSpaceIndex` (9 fields) — `header` = column name in `ВільніПлощі.xlsx`
- `OrendaIndex` (19 fields) — `header` = column name in `Оренда.xlsx`
- `BuildingIndex` (36 fields) — canonical in-memory building record shared across all three processors; still positional (`index`), it is an internal layout, not tied to Excel

When Excel headers are reworded, update the `header` string of the relevant `*Index` entry. Column **order** and extra/unknown columns no longer matter.

### Column Resolution (`ColumnResolver.kt`)

The first step of each file's processing calls `ColumnResolver.resolve*(sheet)`, which scans the header row(s) and returns a `Map<*Index, Int>` (logical field → physical column). Completeness is enforced: if any `*Index` entry's `header` is not found, it throws `IllegalStateException` listing the missing columns, which surfaces as "Помилка!" in the UI. Matching is strict — the file's header must equal the enum `header` after `trim()`; a reworded header is a deliberate error, not a silent skip.

Header rows scanned: `Баланс`/`Оренда` — row index 1 only; `ВільніПлощі` — rows 1–2 flattened (its header spans two rows: super-headers like "Наявність комунікацій" plus sub-headers like "Водопостачання"), the lower row wins per column, so `FreeSpaceIndex` stores the sub-headers.

`Оренда.xlsx` column `ID об’єктів за договором` and `ВільніПлощі.xlsx` column `Загальна площа об’єкта` use Unicode U+2019 (`'`) instead of U+0027 — encoded correctly in the enum `header` strings.

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
