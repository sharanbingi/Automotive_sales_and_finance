package com.automotive.salesfinance.model

data class Vehicle(
    val vehicleId: String = "",
    val dealershipId: String = "",
    val vehicleType: VehicleType = VehicleType.BIKE,
    val chassisNumber: String = "",
    val engineNumber: String = "",
    val make: String = "",
    val model: String = "",
    val year: Int = 2024,
    val variant: String = "",
    val fuelType: FuelType = FuelType.PETROL,
    val transmission: TransmissionType = TransmissionType.MANUAL,
    val registrationNumber: String = "",
    val color: String = "",
    val odometerKm: Int = 0,
    val costPrice: Double = 0.0,
    val listedPrice: Double = 0.0,
    val stateCode: String = "",
    val storeId: String = "",
    val storeLocation: String = "",
    val status: BikeStatus = BikeStatus.AVAILABLE,
    val inwardTimestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val imageUrl: String = ""
) {
    val effectiveStoreId: String
        get() = storeId.ifBlank { storeLocation }
}

fun Bike.toVehicle(): Vehicle = Vehicle(
    vehicleId = bikeId,
    dealershipId = dealershipId,
    vehicleType = VehicleType.BIKE,
    chassisNumber = chassisNumber,
    engineNumber = engineNumber,
    make = make,
    model = model,
    year = year,
    variant = "",
    fuelType = FuelType.PETROL,
    transmission = TransmissionType.MANUAL,
    registrationNumber = registrationNumber,
    color = color,
    odometerKm = 0,
    costPrice = costPrice,
    listedPrice = listedPrice,
    stateCode = stateCode,
    storeId = storeLocation,
    storeLocation = storeLocation,
    status = status,
    inwardTimestamp = inwardTimestamp,
    createdAt = inwardTimestamp,
    updatedAt = inwardTimestamp,
    imageUrl = imageUrl
)

fun Vehicle.toBike(): Bike = Bike(
    bikeId = vehicleId,
    dealershipId = dealershipId,
    chassisNumber = chassisNumber,
    engineNumber = engineNumber,
    make = make,
    model = model,
    year = year,
    color = color,
    registrationNumber = registrationNumber,
    costPrice = costPrice,
    listedPrice = listedPrice,
    stateCode = stateCode,
    storeLocation = effectiveStoreId,
    status = status,
    inwardTimestamp = inwardTimestamp,
    imageUrl = imageUrl
)
