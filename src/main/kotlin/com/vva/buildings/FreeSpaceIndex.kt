package com.vva.buildings

/**
 * Логічні поля файлу ВільніПлощі.xlsx. `header` — назва колонки, за якою ColumnResolver
 * знаходить фізичний номер колонки. Порядок оголошення значення не має.
 *
 * Заголовок у файлі займає два рядки (індекси 1–2): над-заголовки (напр. "Наявність комунікацій")
 * і під-заголовки (напр. "Водопостачання"). ColumnResolver сплющує обидва рядки, під-заголовок
 * перемагає над-заголовок. Тому тут вказані саме під-заголовки для колонок комунікацій та площі.
 *
 * Увага: `area` у файлі використовує U+2019 (') замість звичайного апострофа U+0027.
 */
enum class FreeSpaceIndex(val header: String) {
    idSpace("Реєстра-ційний №"),
    buildingId("ID об'єкту"),
    etcCode("Унікальний код обєкту у ЕТС Прозорро-продажі"),
    area("Загальна площа об’єкта"),
    utilitiesAvailableWaterSupply("Водопостачання"),
    utilitiesAvailableHeatingSupply("Теплопостачання"),
    utilitiesAvailableElectricNetwork("Потужність електромережі"),
    utilitiesAvailableGasSupply("Газопостачання"),
    addressLocatorDesignator("Номер Будинку"),
}
