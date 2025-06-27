package com.vva.buildings

enum class FreeSpaceIndex(val index: Int) {
    idSpace(0),       // Реєстра-ційний №
    buildingId(1),    // ID об'єкту
    etcCode(2),       // Унікальний код обєкту у ЕТС Прозорро-продажі
    area(4),          // Площа вільного простору (кв.м.)
    utilitiesAvailableWaterSupply(7), // Наявність водопостачання
    utilitiesAvailableHeatingSupply(8), // Наявність теплопостачання
    utilitiesAvailableElectricNetwork(9), // Наявність електромереж
    utilitiesAvailableGasSupply(10), // Наявність газопостачання
}