package com.vva.buildings

enum class OrendaIndex(val index: Int) {
    id(0),          // ID
    idBuilding(1),  // ID об'єкту
    etcCode(2), //
    quantity(3),    // Площа приміщення, що використовується, кв.м
    valueAmount(4), // Оціночна вартість приміщень за договором, грн
    valuationDate(5), // Дата, на яку проведена оцінка об'єкту
    contractNumber(6), // Номер Договору Оренди
    contractDateSigned(7), // Дата укладання договору
    contractStatus(8), // Стан договору
    contractCustodianName(9), // Балансоутримувач - Повна Назва
    contractCustodianId(10), // Балансоутримувач - Код ЄДРПОУ
    contractPeriodStartDate(11), // Початок оренди
    contractPeriodEndDate(12), // Закінченя оренди
    contractValueAmount(13), // Місячна орендна плата, грн.
    contractUserName(14), // Орендар - Повна Назва
    contractUserId(15), // Орендар - Код ЄДРПОУ
    addressLocatorDesignator(16), // Номер будинку
    validityDate(17), // Дата Актуальності
    contractFactPeriodEndDate(18), // Фактичне закінченя оренди
}