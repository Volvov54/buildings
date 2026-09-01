package com.vva.buildings

/**
 * Логічні поля файлу Баланс.xlsx. `header` — назва колонки у рядку заголовків (індекс 1),
 * за якою ColumnResolver знаходить фізичний номер колонки. Порядок оголошення значення не має.
 */
enum class BalansIndex(val header: String) {
    id("ID об'єкту"),
    title("Назва Об'єкту"),
    kind("Вид Об'єкту відповідно Класифікатора майна"),
    type("Тип Об'єкту"),
    description("Призначення"),
    balanceHolderName("Балансоутримувач - Повна Назва"),
    balanceHolderId("Балансоутримувач - Код ЄДРПОУ"),
    dk018classId("Вид Об'єкту відповідно Класифікатора майна (код)"),
    dk018classDescription("Вид Об'єкту відповідно Класифікатора майна (назва)"),
    area("Загальна Площа будинку (кв.м.)"),
    addressPostCode("Поштовий індекс"),
    addressPostDistrict("Район"),
    addressThoroughfare("Назва Вулиці"),
    registrationId("Реєстрація у Державному реєстрі (Реєстраційний номер об'єкту нерухомого майна)"),
    condition("Стан Об'єкту"),
    validityDate("Дата Актуальності"),
    destinationGroup("Група Призначення"),
    addressLocatorDesignator("Номер Будинку"),
    fieldOfActivity("Сфера діяльності"),
}
