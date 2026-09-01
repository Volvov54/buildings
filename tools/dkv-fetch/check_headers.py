"""Перевірка заголовків завантаженого .xlsx проти списку колонок, які читає програма.

Використання:
    python check_headers.py <file.xlsx> <header_row_indices> <required_header> [<required_header> ...]

    header_row_indices — 0-based індекси рядків заголовка через кому, напр. "1" або "1,2".
    Логіка сплющування збігається з ColumnResolver: рядок з більшим індексом перемагає.

Друкує рядок стану; містить слово MISSING, якщо якоїсь колонки бракує (це сигнал скрипту
не переносити файл у data/input/).
"""

import sys
import zipfile
import xml.etree.ElementTree as ET

# Консоль Windows часто не UTF-8 — примусово, щоб кирилиця та символи не падали.
try:
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
except Exception:
    pass

NS = "{http://schemas.openxmlformats.org/spreadsheetml/2006/main}"


def col_index(cell_ref: str) -> int:
    letters = "".join(c for c in cell_ref if c.isalpha())
    n = 0
    for c in letters:
        n = n * 26 + (ord(c.upper()) - 64)
    return n - 1


def flat_headers(path: str, header_rows: set[int]) -> tuple[dict[int, str], int]:
    z = zipfile.ZipFile(path)
    shared = [
        "".join(t.text or "" for t in si.iter(NS + "t"))
        for si in ET.fromstring(z.read("xl/sharedStrings.xml")).findall(NS + "si")
    ]
    sheet = ET.fromstring(z.read("xl/worksheets/sheet1.xml"))
    rows = sheet.find(NS + "sheetData").findall(NS + "row")
    flat: dict[int, str] = {}
    for row in rows:
        ri = int(row.get("r")) - 1
        if ri not in header_rows:
            continue
        for c in row.findall(NS + "c"):
            v = c.find(NS + "v")
            val = v.text if v is not None else None
            if c.get("t") == "s" and val is not None:
                val = shared[int(val)]
            if val and str(val).strip():
                flat[col_index(c.get("r"))] = str(val).strip()
    return flat, len(rows)


def main() -> int:
    path = sys.argv[1]
    header_rows = {int(x) for x in sys.argv[2].split(",")}
    required = sys.argv[3:]

    flat, sheet_rows = flat_headers(path, header_rows)
    present = set(flat.values())
    missing = [h for h in required if h not in present]

    data_rows = sheet_rows - (max(header_rows) + 1)
    status = "OK" if not missing else "MISSING"
    print(
        f"{status}: колонок у файлі={len(flat)}, потрібних знайдено={len(required) - len(missing)}/{len(required)}, "
        f"рядків даних ~{data_rows}"
    )
    if missing:
        for h in missing:
            print(f'  MISSING: "{h}"')
        print("  --- усі заголовки у файлі ---")
        for col in sorted(flat):
            print(f"    [{col}] {flat[col]}")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
