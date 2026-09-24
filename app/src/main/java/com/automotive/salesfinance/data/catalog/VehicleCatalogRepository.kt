package com.automotive.salesfinance.data.catalog

import com.automotive.salesfinance.model.VehicleType

class VehicleCatalogRepository {

    fun getMakesForVehicleType(vehicleType: VehicleType, searchQuery: String = ""): List<String> {
        val makes = VehicleCatalog.allMakes
            .filter { it.vehicleType == vehicleType }
            .map { it.name }
            .distinct()

        val query = searchQuery.trim()
        if (query.isBlank()) {
            return makes
        }
        val filtered = makes.filter { it.contains(query, ignoreCase = true) }
        return if (filtered.isEmpty()) {
            listOf(VehicleCatalog.OTHER)
        } else {
            filtered
        }
    }

    fun getModelsForMake(vehicleType: VehicleType, makeName: String, searchQuery: String = ""): List<String> {
        val trimmedMake = makeName.trim()
        if (trimmedMake.isBlank()) {
            return emptyList()
        }
        if (trimmedMake.equals(VehicleCatalog.OTHER, ignoreCase = true)) {
            return listOf(VehicleCatalog.OTHER)
        }

        val modelsForMake = VehicleCatalog.allModels
            .filter { it.vehicleType == vehicleType && it.makeName.equals(trimmedMake, ignoreCase = true) }
            .map { it.name }
            .distinct()

        val rawModels = if (modelsForMake.isEmpty()) {
            listOf(VehicleCatalog.OTHER)
        } else {
            if (!modelsForMake.any { it.equals(VehicleCatalog.OTHER, ignoreCase = true) }) {
                modelsForMake + VehicleCatalog.OTHER
            } else {
                modelsForMake
            }
        }

        val query = searchQuery.trim()
        if (query.isBlank()) {
            return rawModels
        }

        val filtered = rawModels.filter { it.contains(query, ignoreCase = true) }
        return if (filtered.isEmpty()) {
            listOf(VehicleCatalog.OTHER)
        } else {
            filtered
        }
    }

    fun isValidMakeForVehicleType(vehicleType: VehicleType, makeName: String): Boolean {
        val trimmed = makeName.trim()
        if (trimmed.isBlank()) return false
        if (trimmed.equals(VehicleCatalog.OTHER, ignoreCase = true)) return true
        return VehicleCatalog.allMakes.any {
            it.vehicleType == vehicleType && it.name.equals(trimmed, ignoreCase = true)
        }
    }

    fun isValidModelForMake(vehicleType: VehicleType, makeName: String, modelName: String): Boolean {
        val trimmedModel = modelName.trim()
        val trimmedMake = makeName.trim()
        if (trimmedModel.isBlank()) return false
        if (trimmedModel.equals(VehicleCatalog.OTHER, ignoreCase = true)) return true
        if (trimmedMake.equals(VehicleCatalog.OTHER, ignoreCase = true)) return true

        val isCatalogMake = VehicleCatalog.allMakes.any {
            it.vehicleType == vehicleType && it.name.equals(trimmedMake, ignoreCase = true)
        }

        if (!isCatalogMake) {
            return true
        }

        return VehicleCatalog.allModels.any {
            it.vehicleType == vehicleType &&
                it.makeName.equals(trimmedMake, ignoreCase = true) &&
                it.name.equals(trimmedModel, ignoreCase = true)
        }
    }
}
