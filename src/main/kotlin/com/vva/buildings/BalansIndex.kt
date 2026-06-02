package com.vva.buildings

enum class BalansIndex(val index: Int) {
    id(0),                    // ID об'єкту
    title(1),                 // Назва об'єкту
    kind(2),                  // Вид Об'єкту відповідно Класифікатора майна
    type(3),                  // Тип Об'єкту
    description(4),           // Призначення
    balanceHolderName(5),     // Балансоутримувач - Повна Назва
    balanceHolderId(6),       // Балансоутримувач - Код ЄДРПОУ
    dk018classId(7),          // Вид Об'єкту відповідно Класифікатора майна (код)
    dk018classDescription(8), // Вид Об'єкту відповідно Класифікатора майна (назва)
    area(9),                  // Загальна Площа будинку (кв.м.)
    addressPostCode(10),      // Поштовий індекс
    addressPostDistrict(11),  // Район
    addressThoroughfare(12),  // Назва Вулиці
    registrationId(13),       // Реєстрація у Державному реєстрі (Реєстраційний номер об'єкту нерухомого майна)
    condition(14),            // Стан об'єкту
    validityDate(15),         // Дата Актуальності
    destinationGroup(16),     // Група призначення
    addressLocatorDesignator(17), // Номер будинку
    fieldOfActivity(18),     // Сфера діяльності
}