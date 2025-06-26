package com.vva.buildings

enum class FreeSpaceIndex(index: Int) {
    idSpace(0),       // Реєстра-ційний №
    buildingId(1),    // ID об'єкту
    etcCode(2),       // Унікальний код обєкту у ЕТС Прозорро-продажі
    area(4),          // Площа вільного простору (кв.м.)
}