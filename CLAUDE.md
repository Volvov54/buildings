# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.vva.buildings.BuildingsApplicationTests"

# Build without tests
./gradlew build -x test
```

The app reads Excel files from `data/input/` and writes CSV files to `data/output/`. Both directories must exist before running.

## Architecture Overview

**Buildings** is a Spring Boot Kotlin batch application that transforms Ukrainian municipal building registry data from Excel into normalized CSV exports. There is no REST API, no database, and no AI — it's a pure data pipeline run as a `CommandLineRunner`.

### Data Flow

`BuildingsApplication.kt` orchestrates three sequential processors:

1. **Balans** — reads `Баланс.xlsx`, builds `tabBuildings: Map<String, MutableList<String>>` (keyed by building ID)
2. **FreeSpace** — reads `ВільніПлощі.xlsx`, cross-references against `tabBuildings`, splits into Prozorro vs. non-Prozorro outputs
3. **Orenda** — reads `Оренда.xlsx`, cross-references against `tabBuildings`, splits into Prozorro vs. non-Prozorro rental outputs

Each processor is a Spring `@Service`. All state flows through shared maps passed as function parameters — no persistence layer.

### Outputs (5 CSV files in `data/output/`)

| File | Source | Content |
|---|---|---|
| `buildings.csv` | Balans | Full building directory (excl. dest. group 634) |
| `buildingsRentable.csv` | FreeSpace | Free spaces without Prozorro code |
| `listProzorroSales_buildingsRentable.csv` | FreeSpace | Free spaces with Prozorro auction code |
| `list.csv` | Orenda | Active rental contracts |
| `listProzorroSales_rented.csv` | Orenda | Active rentals with Prozorro auction code |

### Domain Model

Each domain area has a paired enum defining Excel column indices:

- `BuildingIndex` (36 fields) — canonical building record written to CSV
- `BalansIndex` (19 fields) — column positions in `Баланс.xlsx`
- `FreeSpaceIndex` (13 fields) — column positions in `ВільніПлощі.xlsx`
- `OrendaIndex` (19 fields) — column positions in `Оренда.xlsx`

When adding/renaming Excel columns, update the relevant `*Index` enum and adjust the processor that reads it.

### Key Filtering Rules

- **Exclude balance holder ID `22991617`** from all outputs
- **Exclude field of activity `Невизначені`** (undefined) from `buildings.csv`
- **Destination group `634`**: excluded from `buildings.csv` but used as a *filter criterion* in FreeSpace and Orenda (see `Utils.is634()`, `Utils.is634m()`)
- **Orenda validity date**: must be >= `2020-01-01`; skip row if date is missing
- **Contract status**: only `"Договір діє"` (active) contracts are included
- **Closed balance holders**: hardcoded list of 12 entities in `Utils.isBalanceHolderClosed()` (Kyiv Metro, Water Utility, etc.)

### Key Utilities (`Utils.kt`)

| Function | Purpose |
|---|---|
| `setQuotation()` / `getQuotationString()` | CSV escaping (doubles internal quotes) |
| `getCurrencyValue()` | Numeric formatting with UAH locale |
| `getDt8601()` | Parses Ukrainian date formats → ISO 8601 |
| `getNotPrivate()` | Masks 10-digit IDs as `XXXXXXXXXX` (private persons); legal entity IDs (longer) pass through |
| `getEtcCode()` / `getUrl()` | Prozorro auction URL generation (LL/UA prefix → auction site, RG prefix → planning site) |
| `isNotKyiv()` | True if address is not in one of 8 known Kyiv districts |

### Hardcoded Paths

All input/output paths are hardcoded in `BuildingsApplication.kt`. Input files use Ukrainian filenames (`Баланс.xlsx`, `ВільніПлощі.xlsx`, `Оренда.xlsx`). If paths change, update that file.

### Logging

Configured in `application.properties`: level `root=info`, output to `logs/buildings.log`. Use SLF4J `LoggerFactory` — Lombok `@Slf4j` is available (compileOnly).

### Testing

Currently only a context-load smoke test exists. Business logic (filtering rules, date parsing, CSV escaping) is untested. New logic should be unit-tested in `src/test/kotlin/com/vva/buildings/`.
