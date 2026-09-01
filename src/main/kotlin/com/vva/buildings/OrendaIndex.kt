package com.vva.buildings

/**
 * Логічні поля файлу Оренда.xlsx. `header` — назва колонки у рядку заголовків (індекс 1),
 * за якою ColumnResolver знаходить фізичний номер колонки. Порядок оголошення значення не має.
 *
 * Увага: `idBuilding` у файлі використовує U+2019 (') замість звичайного апострофа U+0027.
 */
enum class OrendaIndex(val header: String) {
    id("ID договору"),
    idBuilding("ID об’єктів за договором"),
    etcCode("Унікальний код обєкту у ЕТС Прозорро-продажі"),
    quantity("Площа що орендується, кв.м"),
    valueAmount("Оціночна вартість приміщень за договором, грн"),
    valuationDate("Дата, на яку проведена оцінка об'єкту"),
    contractNumber("Номер Договору Оренди"),
    contractDateSigned("Дата укладання договору"),
    contractStatus("Стан договору"),
    contractCustodianName("Балансоутримувач - Повна Назва"),
    contractCustodianId("Балансоутримувач - Код ЄДРПОУ"),
    contractPeriodStartDate("Дата початку використання приміщення"),
    contractPeriodEndDate("Закінчення Оренди"),
    contractValueAmount("Місячна орендна плата, грн."),
    contractUserName("Орендар - Повна Назва"),
    contractUserId("Орендар - Код ЄДРПОУ"),
    addressLocatorDesignator("Номер Будинку"),
    validityDate("Дата Актуальності"),
    contractFactPeriodEndDate("Фактичне Закінчення Оренди"),
}
