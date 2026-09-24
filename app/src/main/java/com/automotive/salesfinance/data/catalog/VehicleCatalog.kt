package com.automotive.salesfinance.data.catalog

import com.automotive.salesfinance.model.VehicleType

data class VehicleMake(
    val name: String,
    val vehicleType: VehicleType
)

data class VehicleModel(
    val name: String,
    val makeName: String,
    val vehicleType: VehicleType
)

object VehicleCatalog {
    const val OTHER = "Other"

    val bikeMakes: List<VehicleMake> = listOf(
        VehicleMake("Honda", VehicleType.BIKE),
        VehicleMake("Hero", VehicleType.BIKE),
        VehicleMake("TVS", VehicleType.BIKE),
        VehicleMake("Bajaj", VehicleType.BIKE),
        VehicleMake("Royal Enfield", VehicleType.BIKE),
        VehicleMake("Yamaha", VehicleType.BIKE),
        VehicleMake("Suzuki", VehicleType.BIKE),
        VehicleMake("KTM", VehicleType.BIKE),
        VehicleMake("Ather", VehicleType.BIKE),
        VehicleMake("Ola Electric", VehicleType.BIKE),
        VehicleMake("Revolt", VehicleType.BIKE),
        VehicleMake("Triumph", VehicleType.BIKE),
        VehicleMake("BMW", VehicleType.BIKE),
        VehicleMake("Jawa", VehicleType.BIKE),
        VehicleMake("Yezdi", VehicleType.BIKE),
        VehicleMake("Kawasaki", VehicleType.BIKE),
        VehicleMake("Aprilia", VehicleType.BIKE),
        VehicleMake(OTHER, VehicleType.BIKE)
    )

    val bikeModels: List<VehicleModel> = listOf(
        // Honda
        VehicleModel("Activa 6G", "Honda", VehicleType.BIKE),
        VehicleModel("Activa 125", "Honda", VehicleType.BIKE),
        VehicleModel("Shine 125", "Honda", VehicleType.BIKE),
        VehicleModel("SP 125", "Honda", VehicleType.BIKE),
        VehicleModel("Unicorn", "Honda", VehicleType.BIKE),
        VehicleModel("CB350", "Honda", VehicleType.BIKE),
        VehicleModel("Dio", "Honda", VehicleType.BIKE),
        VehicleModel("Hornet 2.0", "Honda", VehicleType.BIKE),
        VehicleModel("CB200X", "Honda", VehicleType.BIKE),
        VehicleModel(OTHER, "Honda", VehicleType.BIKE),

        // Hero
        VehicleModel("Splendor Plus", "Hero", VehicleType.BIKE),
        VehicleModel("HF Deluxe", "Hero", VehicleType.BIKE),
        VehicleModel("Passion Plus", "Hero", VehicleType.BIKE),
        VehicleModel("Glamour", "Hero", VehicleType.BIKE),
        VehicleModel("Xpulse 200 4V", "Hero", VehicleType.BIKE),
        VehicleModel("Xtreme 160R", "Hero", VehicleType.BIKE),
        VehicleModel("Mavrick 440", "Hero", VehicleType.BIKE),
        VehicleModel("Xoom", "Hero", VehicleType.BIKE),
        VehicleModel(OTHER, "Hero", VehicleType.BIKE),

        // TVS
        VehicleModel("Jupiter", "TVS", VehicleType.BIKE),
        VehicleModel("Apache RTR 160", "TVS", VehicleType.BIKE),
        VehicleModel("Apache RTR 200 4V", "TVS", VehicleType.BIKE),
        VehicleModel("Raider 125", "TVS", VehicleType.BIKE),
        VehicleModel("Ntorq 125", "TVS", VehicleType.BIKE),
        VehicleModel("XL100", "TVS", VehicleType.BIKE),
        VehicleModel("iQube", "TVS", VehicleType.BIKE),
        VehicleModel("Ronin", "TVS", VehicleType.BIKE),
        VehicleModel(OTHER, "TVS", VehicleType.BIKE),

        // Bajaj
        VehicleModel("Pulsar 150", "Bajaj", VehicleType.BIKE),
        VehicleModel("Pulsar NS200", "Bajaj", VehicleType.BIKE),
        VehicleModel("Pulsar N160", "Bajaj", VehicleType.BIKE),
        VehicleModel("Pulsar N250", "Bajaj", VehicleType.BIKE),
        VehicleModel("Chetak", "Bajaj", VehicleType.BIKE),
        VehicleModel("Platina 110", "Bajaj", VehicleType.BIKE),
        VehicleModel("Avenger Street 160", "Bajaj", VehicleType.BIKE),
        VehicleModel("Dominar 400", "Bajaj", VehicleType.BIKE),
        VehicleModel(OTHER, "Bajaj", VehicleType.BIKE),

        // Royal Enfield
        VehicleModel("Classic 350", "Royal Enfield", VehicleType.BIKE),
        VehicleModel("Bullet 350", "Royal Enfield", VehicleType.BIKE),
        VehicleModel("Hunter 350", "Royal Enfield", VehicleType.BIKE),
        VehicleModel("Meteor 350", "Royal Enfield", VehicleType.BIKE),
        VehicleModel("Himalayan 450", "Royal Enfield", VehicleType.BIKE),
        VehicleModel("Continental GT 650", "Royal Enfield", VehicleType.BIKE),
        VehicleModel("Interceptor 650", "Royal Enfield", VehicleType.BIKE),
        VehicleModel(OTHER, "Royal Enfield", VehicleType.BIKE),

        // Yamaha
        VehicleModel("FZ-S FI", "Yamaha", VehicleType.BIKE),
        VehicleModel("MT-15 V2", "Yamaha", VehicleType.BIKE),
        VehicleModel("R15 V4", "Yamaha", VehicleType.BIKE),
        VehicleModel("RayZR 125", "Yamaha", VehicleType.BIKE),
        VehicleModel("Fascino 125", "Yamaha", VehicleType.BIKE),
        VehicleModel("Aerox 155", "Yamaha", VehicleType.BIKE),
        VehicleModel("FZ-X", "Yamaha", VehicleType.BIKE),
        VehicleModel(OTHER, "Yamaha", VehicleType.BIKE),

        // Suzuki
        VehicleModel("Access 125", "Suzuki", VehicleType.BIKE),
        VehicleModel("Burgman Street", "Suzuki", VehicleType.BIKE),
        VehicleModel("Gixxer SF 150", "Suzuki", VehicleType.BIKE),
        VehicleModel("Gixxer 250", "Suzuki", VehicleType.BIKE),
        VehicleModel("V-Strom SX", "Suzuki", VehicleType.BIKE),
        VehicleModel(OTHER, "Suzuki", VehicleType.BIKE),

        // KTM
        VehicleModel("Duke 200", "KTM", VehicleType.BIKE),
        VehicleModel("Duke 250", "KTM", VehicleType.BIKE),
        VehicleModel("Duke 390", "KTM", VehicleType.BIKE),
        VehicleModel("RC 200", "KTM", VehicleType.BIKE),
        VehicleModel("RC 390", "KTM", VehicleType.BIKE),
        VehicleModel("Adventure 390", "KTM", VehicleType.BIKE),
        VehicleModel(OTHER, "KTM", VehicleType.BIKE),

        // Ather
        VehicleModel("450X", "Ather", VehicleType.BIKE),
        VehicleModel("450S", "Ather", VehicleType.BIKE),
        VehicleModel("Rizta", "Ather", VehicleType.BIKE),
        VehicleModel(OTHER, "Ather", VehicleType.BIKE),

        // Ola Electric
        VehicleModel("S1 Pro", "Ola Electric", VehicleType.BIKE),
        VehicleModel("S1 X", "Ola Electric", VehicleType.BIKE),
        VehicleModel("S1 Air", "Ola Electric", VehicleType.BIKE),
        VehicleModel(OTHER, "Ola Electric", VehicleType.BIKE),

        // Revolt
        VehicleModel("RV400", "Revolt", VehicleType.BIKE),
        VehicleModel("RV400 BRZ", "Revolt", VehicleType.BIKE),
        VehicleModel(OTHER, "Revolt", VehicleType.BIKE),

        // Triumph
        VehicleModel("Speed 400", "Triumph", VehicleType.BIKE),
        VehicleModel("Scrambler 400X", "Triumph", VehicleType.BIKE),
        VehicleModel(OTHER, "Triumph", VehicleType.BIKE),

        // BMW
        VehicleModel("G 310 R", "BMW", VehicleType.BIKE),
        VehicleModel("G 310 GS", "BMW", VehicleType.BIKE),
        VehicleModel("G 310 RR", "BMW", VehicleType.BIKE),
        VehicleModel(OTHER, "BMW", VehicleType.BIKE),

        // Jawa
        VehicleModel("Jawa 350", "Jawa", VehicleType.BIKE),
        VehicleModel("42", "Jawa", VehicleType.BIKE),
        VehicleModel("Perak", "Jawa", VehicleType.BIKE),
        VehicleModel(OTHER, "Jawa", VehicleType.BIKE),

        // Yezdi
        VehicleModel("Roadster", "Yezdi", VehicleType.BIKE),
        VehicleModel("Scrambler", "Yezdi", VehicleType.BIKE),
        VehicleModel("Adventure", "Yezdi", VehicleType.BIKE),
        VehicleModel(OTHER, "Yezdi", VehicleType.BIKE),

        // Kawasaki
        VehicleModel("Ninja 300", "Kawasaki", VehicleType.BIKE),
        VehicleModel("Ninja 400", "Kawasaki", VehicleType.BIKE),
        VehicleModel("Z900", "Kawasaki", VehicleType.BIKE),
        VehicleModel("ZX-10R", "Kawasaki", VehicleType.BIKE),
        VehicleModel(OTHER, "Kawasaki", VehicleType.BIKE),

        // Aprilia
        VehicleModel("RS 457", "Aprilia", VehicleType.BIKE),
        VehicleModel("SR 160", "Aprilia", VehicleType.BIKE),
        VehicleModel("Storm 125", "Aprilia", VehicleType.BIKE),
        VehicleModel(OTHER, "Aprilia", VehicleType.BIKE),

        // Other Make
        VehicleModel(OTHER, OTHER, VehicleType.BIKE)
    )

    val carMakes: List<VehicleMake> = listOf(
        VehicleMake("Maruti Suzuki", VehicleType.CAR),
        VehicleMake("Hyundai", VehicleType.CAR),
        VehicleMake("Tata Motors", VehicleType.CAR),
        VehicleMake("Mahindra", VehicleType.CAR),
        VehicleMake("Toyota", VehicleType.CAR),
        VehicleMake("Kia", VehicleType.CAR),
        VehicleMake("Honda", VehicleType.CAR),
        VehicleMake("Volkswagen", VehicleType.CAR),
        VehicleMake("Skoda", VehicleType.CAR),
        VehicleMake("MG Motor", VehicleType.CAR),
        VehicleMake("Renault", VehicleType.CAR),
        VehicleMake("Nissan", VehicleType.CAR),
        VehicleMake("Jeep", VehicleType.CAR),
        VehicleMake("BMW", VehicleType.CAR),
        VehicleMake("Mercedes-Benz", VehicleType.CAR),
        VehicleMake("Audi", VehicleType.CAR),
        VehicleMake(OTHER, VehicleType.CAR)
    )

    val carModels: List<VehicleModel> = listOf(
        // Maruti Suzuki
        VehicleModel("Swift", "Maruti Suzuki", VehicleType.CAR),
        VehicleModel("Baleno", "Maruti Suzuki", VehicleType.CAR),
        VehicleModel("Brezza", "Maruti Suzuki", VehicleType.CAR),
        VehicleModel("Dzire", "Maruti Suzuki", VehicleType.CAR),
        VehicleModel("WagonR", "Maruti Suzuki", VehicleType.CAR),
        VehicleModel("Ertiga", "Maruti Suzuki", VehicleType.CAR),
        VehicleModel("Fronx", "Maruti Suzuki", VehicleType.CAR),
        VehicleModel("Grand Vitara", "Maruti Suzuki", VehicleType.CAR),
        VehicleModel("Alto K10", "Maruti Suzuki", VehicleType.CAR),
        VehicleModel("XL6", "Maruti Suzuki", VehicleType.CAR),
        VehicleModel("Ciaz", "Maruti Suzuki", VehicleType.CAR),
        VehicleModel("Jimny", "Maruti Suzuki", VehicleType.CAR),
        VehicleModel("Invicto", "Maruti Suzuki", VehicleType.CAR),
        VehicleModel(OTHER, "Maruti Suzuki", VehicleType.CAR),

        // Hyundai
        VehicleModel("Creta", "Hyundai", VehicleType.CAR),
        VehicleModel("Venue", "Hyundai", VehicleType.CAR),
        VehicleModel("i20", "Hyundai", VehicleType.CAR),
        VehicleModel("Grand i10 Nios", "Hyundai", VehicleType.CAR),
        VehicleModel("Aura", "Hyundai", VehicleType.CAR),
        VehicleModel("Verna", "Hyundai", VehicleType.CAR),
        VehicleModel("Alcazar", "Hyundai", VehicleType.CAR),
        VehicleModel("Tucson", "Hyundai", VehicleType.CAR),
        VehicleModel("Ioniq 5", "Hyundai", VehicleType.CAR),
        VehicleModel("Exter", "Hyundai", VehicleType.CAR),
        VehicleModel(OTHER, "Hyundai", VehicleType.CAR),

        // Tata Motors
        VehicleModel("Nexon", "Tata Motors", VehicleType.CAR),
        VehicleModel("Punch", "Tata Motors", VehicleType.CAR),
        VehicleModel("Harrier", "Tata Motors", VehicleType.CAR),
        VehicleModel("Safari", "Tata Motors", VehicleType.CAR),
        VehicleModel("Tiago", "Tata Motors", VehicleType.CAR),
        VehicleModel("Tigor", "Tata Motors", VehicleType.CAR),
        VehicleModel("Curvv", "Tata Motors", VehicleType.CAR),
        VehicleModel("Altroz", "Tata Motors", VehicleType.CAR),
        VehicleModel("Nexon EV", "Tata Motors", VehicleType.CAR),
        VehicleModel("Punch EV", "Tata Motors", VehicleType.CAR),
        VehicleModel(OTHER, "Tata Motors", VehicleType.CAR),

        // Mahindra
        VehicleModel("Thar", "Mahindra", VehicleType.CAR),
        VehicleModel("Scorpio-N", "Mahindra", VehicleType.CAR),
        VehicleModel("Scorpio Classic", "Mahindra", VehicleType.CAR),
        VehicleModel("XUV700", "Mahindra", VehicleType.CAR),
        VehicleModel("XUV300", "Mahindra", VehicleType.CAR),
        VehicleModel("XUV400 EV", "Mahindra", VehicleType.CAR),
        VehicleModel("Bolero", "Mahindra", VehicleType.CAR),
        VehicleModel("Bolero Neo", "Mahindra", VehicleType.CAR),
        VehicleModel("Thar Roxx", "Mahindra", VehicleType.CAR),
        VehicleModel(OTHER, "Mahindra", VehicleType.CAR),

        // Toyota
        VehicleModel("Innova Crysta", "Toyota", VehicleType.CAR),
        VehicleModel("Innova Hycross", "Toyota", VehicleType.CAR),
        VehicleModel("Fortuner", "Toyota", VehicleType.CAR),
        VehicleModel("Urban Cruiser Taisor", "Toyota", VehicleType.CAR),
        VehicleModel("Glanza", "Toyota", VehicleType.CAR),
        VehicleModel("Hilux", "Toyota", VehicleType.CAR),
        VehicleModel("Camry", "Toyota", VehicleType.CAR),
        VehicleModel(OTHER, "Toyota", VehicleType.CAR),

        // Kia
        VehicleModel("Seltos", "Kia", VehicleType.CAR),
        VehicleModel("Sonet", "Kia", VehicleType.CAR),
        VehicleModel("Carens", "Kia", VehicleType.CAR),
        VehicleModel("EV6", "Kia", VehicleType.CAR),
        VehicleModel("EV9", "Kia", VehicleType.CAR),
        VehicleModel(OTHER, "Kia", VehicleType.CAR),

        // Honda
        VehicleModel("City", "Honda", VehicleType.CAR),
        VehicleModel("Amaze", "Honda", VehicleType.CAR),
        VehicleModel("Elevate", "Honda", VehicleType.CAR),
        VehicleModel(OTHER, "Honda", VehicleType.CAR),

        // Volkswagen
        VehicleModel("Taigun", "Volkswagen", VehicleType.CAR),
        VehicleModel("Virtus", "Volkswagen", VehicleType.CAR),
        VehicleModel("Tiguan", "Volkswagen", VehicleType.CAR),
        VehicleModel(OTHER, "Volkswagen", VehicleType.CAR),

        // Skoda
        VehicleModel("Kushaq", "Skoda", VehicleType.CAR),
        VehicleModel("Slavia", "Skoda", VehicleType.CAR),
        VehicleModel("Kodiaq", "Skoda", VehicleType.CAR),
        VehicleModel(OTHER, "Skoda", VehicleType.CAR),

        // MG Motor
        VehicleModel("Hector", "MG Motor", VehicleType.CAR),
        VehicleModel("Astor", "MG Motor", VehicleType.CAR),
        VehicleModel("ZS EV", "MG Motor", VehicleType.CAR),
        VehicleModel("Comet EV", "MG Motor", VehicleType.CAR),
        VehicleModel("Windsor EV", "MG Motor", VehicleType.CAR),
        VehicleModel("Gloster", "MG Motor", VehicleType.CAR),
        VehicleModel(OTHER, "MG Motor", VehicleType.CAR),

        // Renault
        VehicleModel("Kwid", "Renault", VehicleType.CAR),
        VehicleModel("Triber", "Renault", VehicleType.CAR),
        VehicleModel("Kiger", "Renault", VehicleType.CAR),
        VehicleModel(OTHER, "Renault", VehicleType.CAR),

        // Nissan
        VehicleModel("Magnite", "Nissan", VehicleType.CAR),
        VehicleModel(OTHER, "Nissan", VehicleType.CAR),

        // Jeep
        VehicleModel("Compass", "Jeep", VehicleType.CAR),
        VehicleModel("Meridian", "Jeep", VehicleType.CAR),
        VehicleModel("Wrangler", "Jeep", VehicleType.CAR),
        VehicleModel("Grand Cherokee", "Jeep", VehicleType.CAR),
        VehicleModel(OTHER, "Jeep", VehicleType.CAR),

        // BMW
        VehicleModel("3 Series", "BMW", VehicleType.CAR),
        VehicleModel("5 Series", "BMW", VehicleType.CAR),
        VehicleModel("X1", "BMW", VehicleType.CAR),
        VehicleModel("X3", "BMW", VehicleType.CAR),
        VehicleModel("X5", "BMW", VehicleType.CAR),
        VehicleModel("i4", "BMW", VehicleType.CAR),
        VehicleModel("iX", "BMW", VehicleType.CAR),
        VehicleModel(OTHER, "BMW", VehicleType.CAR),

        // Mercedes-Benz
        VehicleModel("C-Class", "Mercedes-Benz", VehicleType.CAR),
        VehicleModel("E-Class", "Mercedes-Benz", VehicleType.CAR),
        VehicleModel("S-Class", "Mercedes-Benz", VehicleType.CAR),
        VehicleModel("GLA", "Mercedes-Benz", VehicleType.CAR),
        VehicleModel("GLC", "Mercedes-Benz", VehicleType.CAR),
        VehicleModel("GLE", "Mercedes-Benz", VehicleType.CAR),
        VehicleModel("GLS", "Mercedes-Benz", VehicleType.CAR),
        VehicleModel("EQE", "Mercedes-Benz", VehicleType.CAR),
        VehicleModel(OTHER, "Mercedes-Benz", VehicleType.CAR),

        // Audi
        VehicleModel("A4", "Audi", VehicleType.CAR),
        VehicleModel("A6", "Audi", VehicleType.CAR),
        VehicleModel("Q3", "Audi", VehicleType.CAR),
        VehicleModel("Q5", "Audi", VehicleType.CAR),
        VehicleModel("Q7", "Audi", VehicleType.CAR),
        VehicleModel("e-tron", "Audi", VehicleType.CAR),
        VehicleModel(OTHER, "Audi", VehicleType.CAR),

        // Other Make
        VehicleModel(OTHER, OTHER, VehicleType.CAR)
    )

    val allMakes: List<VehicleMake> = bikeMakes + carMakes
    val allModels: List<VehicleModel> = bikeModels + carModels
}
