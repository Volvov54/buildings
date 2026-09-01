// Автоматичне завантаження щомісячних Excel-звітів з порталу ДКВ м. Києва
// (https://dkv.kyivcity.gov.ua) у ../../data/input/.
//
// Важливо: використовуються НЕзвітні сторінки (розділи "Майно (Об'єкти)" / "Оренда"),
// напр. /Balans/BalansObjects.aspx БЕЗ ?reportform=1. На звітних сторінках вибір колонок
// живе один рендер і не потрапляє в експорт; на цих — зберігається постійно (перевірено).
//
// Скрипт для кожного з трьох звітів:
//   • вхід (сесія зберігається в .chrome-profile/, перший раз — вручну)
//   • відкриття сторінки, увімкнення "Дані ДПЗ" (якщо є й вимкнено)
//   • зняття фільтра, якщо на панелі лишився ("Очистити")
//   • проставляння в "Додаткових Колонках" усіх колонок, які читає програма, і "Вибрати"
//   • "Зберегти у Файлі" -> "XLS - Microsoft Excel", перехоплення файлу
//   • перевірку заголовків і перенесення у data/input/
//     (старий файл не чіпається, якщо якоїсь колонки бракує)
//
// Запуск:  npm run fetch                     (усі три)
//          npm run fetch -- --only=balans     (balans | orenda | freespace)
//
// Перший запуск: увійдіть у порталі у вікні браузера вручну (сесія збережеться).

import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import { dirname, resolve, join } from 'node:path';
import { mkdirSync, existsSync, renameSync, rmSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import readline from 'node:readline/promises';

const __dirname = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = resolve(__dirname, '..', '..');
const INPUT_DIR = join(REPO_ROOT, 'data', 'input');
const PROFILE_DIR = join(__dirname, '.chrome-profile');
const DOWNLOAD_DIR = join(__dirname, '.downloads');

const BASE = 'https://dkv.kyivcity.gov.ua';
const ONLY = (process.argv.find((a) => a.startsWith('--only=')) || '').split('=')[1]; // balans|orenda|freespace
const DOWNLOAD_TIMEOUT_MS = 10 * 60 * 1000;

// Колонки, які читає програма — значення *Index.header з
// src/main/kotlin/com/vva/buildings/{Balans,Orenda,FreeSpace}Index.kt.
// Апостроф U+2019 (') навмисний у "ID об’єктів за договором" та "Загальна площа об’єкта".
const REPORTS = [
  {
    key: 'balans',
    // Не /Balans/BalansObjects.aspx?reportform=1 (розділ "Звіти") — там вибір колонок
    // ефемерний і НЕ потрапляє в експорт. Ця сторінка (розділ "Майно (Об'єкти)") зберігає
    // стан колонок постійно — перевірено вручну.
    url: '/Balans/BalansObjects.aspx',
    output: 'Баланс.xlsx',
    headerRows: [1],
    // columnsToTick = весь required: сторінка сама вирішує, що вже позначено (preselectColumns
    // пропускає вже позначені), тож простіше не гадати про дефолтний набір цієї сторінки.
    required: [
      "ID об'єкту",
      "Назва Об'єкту",
      "Вид Об'єкту відповідно Класифікатора майна",
      "Тип Об'єкту",
      'Призначення',
      'Балансоутримувач - Повна Назва',
      'Балансоутримувач - Код ЄДРПОУ',
      "Вид Об'єкту відповідно Класифікатора майна (код)",
      "Вид Об'єкту відповідно Класифікатора майна (назва)",
      'Загальна Площа будинку (кв.м.)',
      'Поштовий індекс',
      'Район',
      'Назва Вулиці',
      "Реєстрація у Державному реєстрі (Реєстраційний номер об'єкту нерухомого майна)",
      "Стан Об'єкту",
      'Дата Актуальності',
      'Група Призначення',
      'Номер Будинку',
      'Сфера діяльності',
    ],
  },
  {
    key: 'orenda',
    // Так само — не звітна сторінка, а /Arenda/RentAgreements.aspx з розділу "Оренда"
    // (там же може лишитись фільтр від попереднього перегляду — clearDefaultFilter його зніме).
    url: '/Arenda/RentAgreements.aspx',
    output: 'Оренда.xlsx',
    headerRows: [1],
    required: [
      'ID договору',
      'ID об’єктів за договором',
      'Унікальний код обєкту у ЕТС Прозорро-продажі',
      'Площа що орендується, кв.м',
      'Оціночна вартість приміщень за договором, грн',
      "Дата, на яку проведена оцінка об'єкту",
      'Номер Договору Оренди',
      'Дата укладання договору',
      'Стан договору',
      'Балансоутримувач - Повна Назва',
      'Балансоутримувач - Код ЄДРПОУ',
      'Дата початку використання приміщення',
      'Закінчення Оренди',
      'Місячна орендна плата, грн.',
      'Орендар - Повна Назва',
      'Орендар - Код ЄДРПОУ',
      'Номер Будинку',
      'Дата Актуальності',
      'Фактичне Закінчення Оренди',
    ],
  },
  {
    key: 'freespace',
    // НЕзвітна сторінка (Реєстри / Оренда -> Реєстр вільних приміщень) — вона зберігає
    // вибір колонок в експорт (звітна — ні). Групові колонки (площа/комунікації) йдуть самі.
    // Проблема: "ID об'єкту" тут у чеклисті числиться позначеним, але не з'являється —
    // forceRecheck робить uncheck+check, щоб зняти "фантомний" стан.
    url: '/Reports1NF/Report1NFFreeSquare.aspx',
    output: 'ВільніПлощі.xlsx',
    headerRows: [1, 2],
    forceRecheck: true,
    columnsToTick: ["Реєстра-ційний №", "ID об'єкту", 'Унікальний код обєкту у ЕТС Прозорро-продажі', 'Номер Будинку'],
    required: [
      'Реєстра-ційний №',
      "ID об'єкту",
      'Унікальний код обєкту у ЕТС Прозорро-продажі',
      'Загальна площа об’єкта',
      'Водопостачання',
      'Теплопостачання',
      'Потужність електромережі',
      'Газопостачання',
      'Номер Будинку',
    ],
  },
];

const log = (...a) => console.log(new Date().toISOString().slice(11, 19), ...a);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const rl = readline.createInterface({ input: process.stdin, output: process.stdout });

async function ensureLoggedIn(page) {
  await page.goto(BASE + '/', { waitUntil: 'domcontentloaded', timeout: 60000 });
  await sleep(1500);
  if (await page.locator('a[href*="Logout.aspx"]').first().isVisible().catch(() => false)) {
    log('Сесія активна, вхід не потрібен.');
    return;
  }
  const user = process.env.DKV_USERNAME;
  const pass = process.env.DKV_PASSWORD;
  if (user && pass) {
    log('Автологін за DKV_USERNAME / DKV_PASSWORD...');
    await page.goto(BASE + '/Account/Login.aspx', { waitUntil: 'domcontentloaded', timeout: 60000 });
    await page.fill('#MainContent_LoginUser_UserName', user);
    await page.fill('#MainContent_LoginUser_Password', pass);
    await page.check('#MainContent_LoginUser_RememberMe').catch(() => {});
    await Promise.all([
      page.waitForNavigation({ timeout: 30000 }).catch(() => {}),
      page.click('#MainContent_LoginUser_LoginButton_I'),
    ]);
    await sleep(1500);
    if (!(await page.locator('a[href*="Logout.aspx"]').first().isVisible().catch(() => false))) {
      throw new Error('Автологін не вдався — перевірте креденшали.');
    }
    log('Автологін успішний.');
    return;
  }
  log('=> Увійдіть у порталі у вікні браузера. Скрипт чекає...');
  await page.waitForSelector('a[href*="Logout.aspx"]', { timeout: 300000 });
  log('Вхід виконано.');
}

async function waitGridReady(page) {
  await page.waitForSelector('td.dxgvHeader_DevEx', { timeout: 60000 });
  await page
    .waitForFunction(() => /Всього рядків|No data/i.test(document.body.innerText), null, { timeout: 45000 })
    .catch(() => {});
  await sleep(2500);
}

async function gridRowCount(page) {
  return page.evaluate(() => {
    const m = document.body.innerText.match(/Всього рядків:\s*([\d\s]+)/);
    return m ? Number(m[1].replace(/\s/g, '')) : null;
  });
}

async function visibleGridHeaders(page) {
  return page.evaluate(() => {
    const seen = new Set();
    const out = [];
    document.querySelectorAll('td.dxgvHeader_DevEx').forEach((el) => {
      const t = (el.innerText || '').replace(/\s+/g, ' ').trim();
      const r = el.getBoundingClientRect();
      if (t && r.width > 3 && r.height > 3 && el.offsetParent && !seen.has(t)) {
        seen.add(t);
        out.push(t);
      }
    });
    return out;
  });
}

// "Дані ДПЗ" — перемикач вгорі сторінки звіту (не на всіх звітах є), який суттєво міняє
// набір колонок сітки. За словами користувача, у нього завжди увімкнений.
async function ensureDpzMode(page) {
  const label = page.getByText('Дані ДПЗ', { exact: true });
  if (!(await label.count())) return;

  const state = await page.evaluate(() => {
    const el = [...document.querySelectorAll('*')].find(
      (e) => e.childElementCount === 0 && (e.textContent || '').trim() === 'Дані ДПЗ',
    );
    if (!el) return null;
    let p = el;
    for (let i = 0; i < 8 && p; i++) {
      const cb = p.querySelector && p.querySelector('span[class*="dxICheckBox"], input[type=checkbox]');
      if (cb) {
        const cn = (cb.className || '').toString();
        const checked = cb.type === 'checkbox' ? cb.checked : /Checked_DevEx/.test(cn) && !/Unchecked/.test(cn);
        return { found: true, checked };
      }
      p = p.parentElement;
    }
    return { found: false };
  });

  if (!state || !state.found) {
    log('  !! Бачу напис "Дані ДПЗ", але не знайшов чекбокс поряд — перевірте вручну.');
    return;
  }
  log(`  "Дані ДПЗ": ${state.checked ? 'вже увімкнено' : 'вимкнено, вмикаю...'}`);
  if (!state.checked) {
    await label.first().click();
    await sleep(5000);
    await waitGridReady(page);
  }
}

async function clearDefaultFilter(page) {
  const clear = page.locator('a[href="javascript:;"]', { hasText: 'Очистити' });
  if (await clear.count()) {
    const before = await gridRowCount(page);
    log(`  Знімаю фільтр за замовчуванням ("Очистити"), було рядків: ${before ?? '?'}...`);
    await clear.first().click();
    await sleep(4000);
    await waitGridReady(page);
    log(`  Стало рядків: ${(await gridRowCount(page)) ?? '?'}`);
  } else {
    log('  Панель фільтра порожня — знімати нічого.');
  }
}

function columnsToTick(report) {
  return report.columnsToTick ?? report.required;
}

async function preselectColumns(page, report) {
  log('  Відкриваю "Додаткові Колонки" і проставляю галочки...');
  await page.evaluate(() => window.PopupFieldChooser && window.PopupFieldChooser.Show());
  await page.waitForFunction(
    () => window.ListBoxGridColumns && window.ListBoxGridColumns.GetItemCount && window.ListBoxGridColumns.GetItemCount() > 5,
    null,
    { timeout: 30000 },
  );
  await sleep(1000);

  const res = await page.evaluate(
    ({ names, forceRecheck }) => {
      const lb = window.ListBoxGridColumns;
      const n = lb.GetItemCount();
      const indexOf = (name) => {
        for (let i = 0; i < n; i++) if ((lb.GetItem(i).text || '').trim() === name) return i;
        return -1;
      };
      const checked = new Set(lb.GetSelectedItems().map((x) => x.text.trim()));
      const idx = [];
      const notFound = [];
      const already = [];
      for (const name of names) {
        const found = indexOf(name);
        if (found === -1) notFound.push(name);
        else if (checked.has(name)) already.push(name);
        else idx.push(found);
      }
      if (forceRecheck) {
        // зняти "фантомний" стан: спершу зняти позначку з тих, що вже "позначені", потім усе позначити
        const alreadyIdx = already.map(indexOf).filter((i) => i >= 0);
        if (alreadyIdx.length && lb.UnselectIndices) lb.UnselectIndices(alreadyIdx);
        const allIdx = names.map(indexOf).filter((i) => i >= 0);
        lb.SelectIndices(allIdx);
      } else if (idx.length) {
        lb.SelectIndices(idx);
      }
      const d = document.getElementById('MainContent_PopupFieldChooser_FieldChooser1_CPGridColumns_ListBoxGridColumns_D');
      if (d) {
        d.scrollTop = 0;
        d.dispatchEvent(new Event('scroll'));
      }
      return { toggled: idx.length, already: already.length, notFound };
    },
    { names: columnsToTick(report), forceRecheck: !!report.forceRecheck },
  );

  if (res.notFound.length) {
    log('  !! Немає в чеклисті (можливо, у постійних групах сітки):', res.notFound.join(', '));
  }
  log(`  Проставлено галочок: ${res.toggled} (вже було позначено: ${res.already}).`);

  // застосувати — справжній клік по видимому "_CD" кнопки "Вибрати". На цих сторінках
  // вибір колонок ЗБЕРІГАЄТЬСЯ (перевірено), на відміну від звітного режиму.
  await page.locator('#MainContent_PopupFieldChooser_FieldChooser1_ButtonFieldChooserApply_CD').click();
  await sleep(6000);
  await waitGridReady(page);

  // контроль: усі колонки, що мали бути (крім групових), — на екрані
  const onScreen = await visibleGridHeaders(page);
  const stillMissing = columnsToTick(report).filter((h) => !onScreen.includes(h));
  if (stillMissing.length) {
    log(`  !! Після "Вибрати" на екрані немає: ${stillMissing.join(', ')} (перевірка у файлі покаже точно)`);
  }
}

function validateHeaders(filePath, report) {
  const args = [join(__dirname, 'check_headers.py'), filePath, report.headerRows.join(','), ...report.required];
  try {
    return { ok: true, text: execFileSync('python', args, { encoding: 'utf-8' }).trim() };
  } catch (e) {
    return { ok: false, text: ((e.stdout || '') + (e.stderr || '')).trim() || e.message };
  }
}

async function exportXlsx(page, report) {
  log('  Експортую ("Зберегти у Файлі" -> XLS)...');
  // Справжній клік Playwright по видимому "_CD" — синтетичний el.click() не відкриває
  // попап форматів DevExpress (він на mousedown).
  await page.locator('[id$="_SaveAs_CD"]').first().click();

  const xls = page.locator('[id$="_CD"]').filter({ hasText: 'XLS - Microsoft Excel' }).first();
  await xls.waitFor({ state: 'visible', timeout: 30000 });
  await sleep(1000);
  const [download] = await Promise.all([
    page.waitForEvent('download', { timeout: DOWNLOAD_TIMEOUT_MS }),
    xls.click({ noWaitAfter: true }),
  ]);
  const tmp = join(DOWNLOAD_DIR, `${report.key}.download`);
  await download.saveAs(tmp);
  log(`  Файл отримано (${download.suggestedFilename()}).`);
  return tmp;
}

async function processReport(context, report) {
  console.log('\n' + '='.repeat(60));
  log(`${report.output}`);
  console.log('='.repeat(60));
  const page = await context.newPage();
  page.setDefaultTimeout(60000);

  try {
    await page.goto(BASE + report.url, { waitUntil: 'domcontentloaded', timeout: 60000 });
    await waitGridReady(page);
    await ensureDpzMode(page);
    await clearDefaultFilter(page);
    if (report.skipColumns) log('  Колонки не міняю (стандартного набору вистачає).');
    else await preselectColumns(page, report);
    const tmp = await exportXlsx(page, report);

    const check = validateHeaders(tmp, report);
    log('  ' + check.text.replace(/\n/g, '\n  '));
    if (!check.ok) {
      rmSync(tmp, { force: true });
      throw new Error(`Файл ${report.output}: бракує потрібних колонок — старий у data/input/ не чіпаю.`);
    }

    const dest = join(INPUT_DIR, report.output);
    if (existsSync(dest)) rmSync(dest);
    renameSync(tmp, dest);
    log(`  OK -> ${dest}`);
  } finally {
    await page.close();
  }
}

async function main() {
  mkdirSync(INPUT_DIR, { recursive: true });
  mkdirSync(DOWNLOAD_DIR, { recursive: true });
  mkdirSync(PROFILE_DIR, { recursive: true });

  const context = await chromium.launchPersistentContext(PROFILE_DIR, {
    headless: false,
    acceptDownloads: true,
    viewport: { width: 1600, height: 950 },
  });
  context.setDefaultTimeout(60000);

  try {
    const page0 = context.pages()[0] || (await context.newPage());
    await ensureLoggedIn(page0);

    const todo = ONLY ? REPORTS.filter((r) => r.key === ONLY) : REPORTS;
    for (const r of todo) {
      await processReport(context, r);
    }
    console.log('\n' + '='.repeat(60));
    log('Готово. Звіти у data/input/.');
  } finally {
    if (process.stdin.isTTY) await rl.question('\nEnter — закрити браузер...');
    rl.close();
    await context.close();
  }
}

main().catch((e) => {
  console.error('\nПОМИЛКА:', e.message);
  rl.close();
  process.exit(1);
});
