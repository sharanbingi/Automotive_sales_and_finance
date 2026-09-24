package com.automotive.salesfinance.data

import com.automotive.salesfinance.model.AuditLog
import com.automotive.salesfinance.model.Bike
import com.automotive.salesfinance.model.BikeStatus
import com.automotive.salesfinance.model.BillingCycle
import com.automotive.salesfinance.model.Customer
import com.automotive.salesfinance.model.Dealership
import com.automotive.salesfinance.model.FuelType
import com.automotive.salesfinance.model.Loan
import com.automotive.salesfinance.model.LoanStatus
import com.automotive.salesfinance.model.Store
import com.automotive.salesfinance.model.Subscription
import com.automotive.salesfinance.model.SubscriptionPlan
import com.automotive.salesfinance.model.SubscriptionStatus
import com.automotive.salesfinance.model.SupportTicket
import com.automotive.salesfinance.model.TicketPriority
import com.automotive.salesfinance.model.TicketStatus
import com.automotive.salesfinance.model.Transaction
import com.automotive.salesfinance.model.TransactionStatus
import com.automotive.salesfinance.model.TransmissionType
import com.automotive.salesfinance.model.User
import com.automotive.salesfinance.model.UserRole
import com.automotive.salesfinance.model.Vehicle
import com.automotive.salesfinance.model.VehicleType
import com.automotive.salesfinance.model.toBike
import com.automotive.salesfinance.model.toVehicle

object DemoData {

    private val now = System.currentTimeMillis()
    private const val DAY_IN_MS = 24L * 60 * 60 * 1000

    const val DEMO_DEALERSHIP_ID = "dealership_demo_001"
    const val DEMO_DEALERSHIP_BIKES_ONLY = "dealership_bikes_only_001"
    const val DEMO_DEALERSHIP_CARS_ONLY = "dealership_cars_only_001"

    val dealerships = mutableListOf(
        Dealership(
            dealershipId = DEMO_DEALERSHIP_ID,
            name = "DEMO AUTOMOTIVE",
            legalName = "Automotive Sales & Finance Pvt Ltd",
            email = "contact@demoautomotive.com",
            phone = "+91 98765 43210",
            address = "Plot 12, Road No. 36, Jubilee Hills",
            city = "Hyderabad",
            state = "Telangana",
            country = "India",
            active = true,
            subscriptionPlan = SubscriptionPlan.BUSINESS.planId,
            subscriptionStatus = SubscriptionStatus.ACTIVE,
            trialStartDate = now - (18L * DAY_IN_MS),
            trialEndDate = now + (12L * DAY_IN_MS),
            subscriptionStartDate = now - (18L * DAY_IN_MS),
            subscriptionEndDate = now + (347L * DAY_IN_MS),
            supportedVehicleTypes = listOf(VehicleType.BIKE, VehicleType.CAR),
            createdAt = now - (180L * DAY_IN_MS),
            updatedAt = now - (1L * DAY_IN_MS)
        ),
        Dealership(
            dealershipId = DEMO_DEALERSHIP_BIKES_ONLY,
            name = "APEX TWO WHEELERS",
            legalName = "Apex Bikes Pvt Ltd",
            email = "bikes@apex.com",
            phone = "+91 98765 11111",
            address = "MG Road",
            city = "Bangalore",
            state = "Karnataka",
            country = "India",
            active = true,
            subscriptionPlan = SubscriptionPlan.PROFESSIONAL.planId,
            subscriptionStatus = SubscriptionStatus.ACTIVE,
            trialStartDate = now - (10L * DAY_IN_MS),
            trialEndDate = now + (20L * DAY_IN_MS),
            subscriptionStartDate = now - (10L * DAY_IN_MS),
            subscriptionEndDate = now + (355L * DAY_IN_MS),
            supportedVehicleTypes = listOf(VehicleType.BIKE),
            createdAt = now - (90L * DAY_IN_MS),
            updatedAt = now - (1L * DAY_IN_MS)
        ),
        Dealership(
            dealershipId = DEMO_DEALERSHIP_CARS_ONLY,
            name = "ROYAL AUTOMOBILES (CARS)",
            legalName = "Royal Cars Pvt Ltd",
            email = "cars@royal.com",
            phone = "+91 98765 22222",
            address = "Banjara Hills",
            city = "Hyderabad",
            state = "Telangana",
            country = "India",
            active = true,
            subscriptionPlan = SubscriptionPlan.ENTERPRISE.planId,
            subscriptionStatus = SubscriptionStatus.ACTIVE,
            trialStartDate = now - (5L * DAY_IN_MS),
            trialEndDate = now + (25L * DAY_IN_MS),
            subscriptionStartDate = now - (5L * DAY_IN_MS),
            subscriptionEndDate = now + (360L * DAY_IN_MS),
            supportedVehicleTypes = listOf(VehicleType.CAR),
            createdAt = now - (60L * DAY_IN_MS),
            updatedAt = now - (1L * DAY_IN_MS)
        )
    )

    val stores = mutableListOf(
        Store(
            storeId = "TG_Madhapur",
            dealershipId = DEMO_DEALERSHIP_ID,
            storeName = "Madhapur Flagship Store",
            stateCode = "TG",
            city = "Hyderabad",
            address = "Plot 12, Road No. 36, Jubilee Hills/Madhapur, Hyderabad",
            active = true
        ),
        Store(
            storeId = "TG_Hyderabad",
            dealershipId = DEMO_DEALERSHIP_ID,
            storeName = "Kukatpally Branch",
            stateCode = "TG",
            city = "Hyderabad",
            address = "KPHB Main Road, Kukatpally, Hyderabad",
            active = true
        ),
        Store(
            storeId = "KA_Bangalore",
            dealershipId = DEMO_DEALERSHIP_ID,
            storeName = "Indiranagar Experience Hub",
            stateCode = "KA",
            city = "Bangalore",
            address = "100 Feet Road, Indiranagar, Bengaluru",
            active = true
        ),
        Store(
            storeId = "KA_Mysore",
            dealershipId = DEMO_DEALERSHIP_ID,
            storeName = "Mysore Central Dealership",
            stateCode = "KA",
            city = "Mysore",
            address = "JLB Road, Mysuru",
            active = true
        )
    )

    val bikes = mutableListOf(
        Bike(
            bikeId = "BIKE_001",
            dealershipId = DEMO_DEALERSHIP_ID,
            chassisNumber = "TEST001",
            engineNumber = "ENG001",
            make = "Honda",
            model = "Activa 6G",
            year = 2023,
            color = "Pearl White",
            registrationNumber = "TS09AB1234",
            costPrice = 85000.0,
            listedPrice = 95000.0,
            stateCode = "TG",
            storeLocation = "TG_Madhapur",
            status = BikeStatus.AVAILABLE,
            inwardTimestamp = now - (67L * DAY_IN_MS)
        ),
        Bike(
            bikeId = "BIKE_002",
            dealershipId = DEMO_DEALERSHIP_ID,
            chassisNumber = "CHASSIS002",
            engineNumber = "ENG002",
            make = "TVS",
            model = "Jupiter 125",
            year = 2024,
            color = "Matte Black",
            registrationNumber = "TS09CD5678",
            costPrice = 78000.0,
            listedPrice = 88000.0,
            stateCode = "TG",
            storeLocation = "TG_Madhapur",
            status = BikeStatus.AVAILABLE,
            inwardTimestamp = now - (15L * DAY_IN_MS)
        ),
        Bike(
            bikeId = "BIKE_003",
            dealershipId = DEMO_DEALERSHIP_ID,
            chassisNumber = "CHASSIS003",
            engineNumber = "ENG003",
            make = "Royal Enfield",
            model = "Classic 350",
            year = 2024,
            color = "Halcyon Green",
            registrationNumber = "TS07EF9012",
            costPrice = 190000.0,
            listedPrice = 220000.0,
            stateCode = "TG",
            storeLocation = "TG_Hyderabad",
            status = BikeStatus.RESERVED,
            inwardTimestamp = now - (30L * DAY_IN_MS)
        ),
        Bike(
            bikeId = "BIKE_004",
            dealershipId = DEMO_DEALERSHIP_ID,
            chassisNumber = "CHASSIS004",
            engineNumber = "ENG004",
            make = "Bajaj",
            model = "Pulsar 220F",
            year = 2023,
            color = "Volcano Red",
            registrationNumber = "KA01GH3456",
            costPrice = 120000.0,
            listedPrice = 140000.0,
            stateCode = "KA",
            storeLocation = "KA_Bangalore",
            status = BikeStatus.FINANCED,
            inwardTimestamp = now - (45L * DAY_IN_MS)
        ),
        Bike(
            bikeId = "BIKE_005",
            dealershipId = DEMO_DEALERSHIP_ID,
            chassisNumber = "CHASSIS005",
            engineNumber = "ENG005",
            make = "Hero",
            model = "Splendor Plus",
            year = 2023,
            color = "Black with Silver",
            registrationNumber = "KA05IJ7890",
            costPrice = 65000.0,
            listedPrice = 75000.0,
            stateCode = "KA",
            storeLocation = "KA_Mysore",
            status = BikeStatus.AVAILABLE,
            inwardTimestamp = now - (72L * DAY_IN_MS)
        ),
        Bike(
            bikeId = "BIKE_006",
            dealershipId = DEMO_DEALERSHIP_ID,
            chassisNumber = "CHASSIS006",
            engineNumber = "ENG006",
            make = "Yamaha",
            model = "FZ-S V4",
            year = 2024,
            color = "Metallic Grey",
            registrationNumber = "TS08KL2345",
            costPrice = 105000.0,
            listedPrice = 122000.0,
            stateCode = "TG",
            storeLocation = "TG_Hyderabad",
            status = BikeStatus.AVAILABLE,
            inwardTimestamp = now - (10L * DAY_IN_MS)
        ),
        Bike(
            bikeId = "BIKE_BO_001",
            dealershipId = DEMO_DEALERSHIP_BIKES_ONLY,
            chassisNumber = "BIKE_VIN_BO01",
            engineNumber = "ENG_BO01",
            make = "Royal Enfield",
            model = "Hunter 350",
            year = 2024,
            color = "Dapper Grey",
            registrationNumber = "TS09MN6789",
            costPrice = 140000.0,
            listedPrice = 175000.0,
            stateCode = "KA",
            storeLocation = "KA_Bangalore",
            status = BikeStatus.AVAILABLE,
            inwardTimestamp = now - (5L * DAY_IN_MS)
        )
    )

    val sampleCars = mutableListOf(
        Vehicle(
            vehicleId = "CAR_001",
            dealershipId = DEMO_DEALERSHIP_ID,
            vehicleType = VehicleType.CAR,
            chassisNumber = "CARVIN001",
            engineNumber = "ENGCAR001",
            make = "Hyundai",
            model = "Creta",
            variant = "1.5 Petrol SX",
            fuelType = FuelType.PETROL,
            transmission = TransmissionType.MANUAL,
            registrationNumber = "TS09FA1234",
            color = "Polar White",
            odometerKm = 12000,
            year = 2023,
            costPrice = 1100000.0,
            listedPrice = 1350000.0,
            stateCode = "TG",
            storeId = "TG_Madhapur",
            storeLocation = "TG_Madhapur",
            status = BikeStatus.AVAILABLE,
            inwardTimestamp = now - (20L * DAY_IN_MS)
        ),
        Vehicle(
            vehicleId = "CAR_002",
            dealershipId = DEMO_DEALERSHIP_ID,
            vehicleType = VehicleType.CAR,
            chassisNumber = "CARVIN002",
            engineNumber = "ENGCAR002",
            make = "Tata",
            model = "Nexon EV",
            variant = "Empowered Plus",
            fuelType = FuelType.ELECTRIC,
            transmission = TransmissionType.AUTOMATIC,
            registrationNumber = "KA01EV5678",
            color = "Teal Blue",
            odometerKm = 5000,
            year = 2024,
            costPrice = 1450000.0,
            listedPrice = 1680000.0,
            stateCode = "KA",
            storeId = "KA_Bangalore",
            storeLocation = "KA_Bangalore",
            status = BikeStatus.AVAILABLE,
            inwardTimestamp = now - (15L * DAY_IN_MS)
        ),
        Vehicle(
            vehicleId = "CAR_003",
            dealershipId = DEMO_DEALERSHIP_ID,
            vehicleType = VehicleType.CAR,
            chassisNumber = "CARVIN003",
            engineNumber = "ENGCAR003",
            make = "Mahindra",
            model = "Thar",
            variant = "LX 4x4 Hard Top",
            fuelType = FuelType.DIESEL,
            transmission = TransmissionType.MANUAL,
            registrationNumber = "TG07TH7777",
            color = "Napoli Black",
            odometerKm = 18000,
            year = 2023,
            costPrice = 1300000.0,
            listedPrice = 1520000.0,
            stateCode = "TG",
            storeId = "TG_Hyderabad",
            storeLocation = "TG_Hyderabad",
            status = BikeStatus.RESERVED,
            inwardTimestamp = now - (40L * DAY_IN_MS)
        ),
        Vehicle(
            vehicleId = "CAR_004",
            dealershipId = DEMO_DEALERSHIP_ID,
            vehicleType = VehicleType.CAR,
            chassisNumber = "CARVIN004",
            engineNumber = "ENGCAR004",
            make = "Maruti Suzuki",
            model = "Swift",
            variant = "ZXi Plus AMT",
            fuelType = FuelType.PETROL,
            transmission = TransmissionType.AMT,
            registrationNumber = "KA05SW9999",
            color = "Sizzling Red",
            odometerKm = 8000,
            year = 2024,
            costPrice = 680000.0,
            listedPrice = 790000.0,
            stateCode = "KA",
            storeId = "KA_Mysore",
            storeLocation = "KA_Mysore",
            status = BikeStatus.AVAILABLE,
            inwardTimestamp = now - (5L * DAY_IN_MS)
        ),
        Vehicle(
            vehicleId = "CAR_CO_001",
            dealershipId = DEMO_DEALERSHIP_CARS_ONLY,
            vehicleType = VehicleType.CAR,
            chassisNumber = "CARVINCO01",
            engineNumber = "ENGCO01",
            make = "Honda",
            model = "City",
            variant = "ZX CVT",
            fuelType = FuelType.PETROL,
            transmission = TransmissionType.CVT,
            registrationNumber = "TS08HC1111",
            color = "Radiant Red",
            odometerKm = 10000,
            year = 2023,
            costPrice = 1200000.0,
            listedPrice = 1450000.0,
            stateCode = "TG",
            storeId = "TG_Madhapur",
            storeLocation = "TG_Madhapur",
            status = BikeStatus.AVAILABLE,
            inwardTimestamp = now - (8L * DAY_IN_MS)
        )
    )

    val vehicles = mutableListOf<Vehicle>().apply {
        addAll(bikes.map { it.toVehicle() })
        addAll(sampleCars)
    }

    val demoUserProfiles: Map<UserRole, User> = mapOf(
        UserRole.SUPER_ADMIN to User(
            uid = "demo_super_admin",
            dealershipId = DEMO_DEALERSHIP_ID,
            name = "Demo Super Admin",
            email = "admin@automotive.com",
            role = UserRole.SUPER_ADMIN,
            stateCode = "ALL",
            storeId = "ALL",
            active = true
        ),
        UserRole.DEALERSHIP_ADMIN to User(
            uid = "demo_dealership_admin",
            dealershipId = DEMO_DEALERSHIP_ID,
            name = "Demo Dealership Admin",
            email = "dealer.admin@automotive.com",
            role = UserRole.DEALERSHIP_ADMIN,
            stateCode = "ALL",
            storeId = "ALL",
            active = true
        ),
        UserRole.ADMIN to User(
            uid = "demo_dealership_admin",
            dealershipId = DEMO_DEALERSHIP_ID,
            name = "Demo Dealership Admin",
            email = "dealer.admin@automotive.com",
            role = UserRole.ADMIN,
            stateCode = "ALL",
            storeId = "ALL",
            active = true
        ),
        UserRole.STATE_MANAGER to User(
            uid = "demo_state_manager",
            dealershipId = DEMO_DEALERSHIP_ID,
            name = "Demo State Manager",
            email = "state.manager@automotive.com",
            role = UserRole.STATE_MANAGER,
            stateCode = "TG",
            storeId = "ALL",
            active = true
        ),
        UserRole.STORE_MANAGER to User(
            uid = "demo_store_manager",
            dealershipId = DEMO_DEALERSHIP_ID,
            name = "Demo Store Manager",
            email = "store.manager@automotive.com",
            role = UserRole.STORE_MANAGER,
            stateCode = "TG",
            storeId = "TG_Madhapur",
            active = true
        ),
        UserRole.SALES_USER to User(
            uid = "demo_sales_user",
            dealershipId = DEMO_DEALERSHIP_ID,
            name = "Demo Sales User",
            email = "sales@automotive.com",
            role = UserRole.SALES_USER,
            stateCode = "TG",
            storeId = "TG_Madhapur",
            active = true
        ),
        UserRole.FINANCE_USER to User(
            uid = "demo_finance_user",
            dealershipId = DEMO_DEALERSHIP_ID,
            name = "Demo Finance User",
            email = "finance@automotive.com",
            role = UserRole.FINANCE_USER,
            stateCode = "TG",
            storeId = "TG_Madhapur",
            active = true
        ),
        UserRole.CUSTOMER to User(
            uid = "demo_customer_user",
            dealershipId = DEMO_DEALERSHIP_ID,
            name = "Demo Customer User",
            email = "customer@automotive.com",
            role = UserRole.CUSTOMER,
            stateCode = "TG",
            storeId = "TG_Madhapur",
            active = true
        )
    )

    fun getDemoProfileForRole(role: UserRole): User {
        return demoUserProfiles[role]
            ?: users.find { it.role == role }
            ?: users.first()
    }

    val users = mutableListOf(
        demoUserProfiles[UserRole.SUPER_ADMIN]!!,
        demoUserProfiles[UserRole.DEALERSHIP_ADMIN]!!,
        demoUserProfiles[UserRole.STATE_MANAGER]!!,
        demoUserProfiles[UserRole.STORE_MANAGER]!!,
        demoUserProfiles[UserRole.SALES_USER]!!,
        demoUserProfiles[UserRole.FINANCE_USER]!!,
        demoUserProfiles[UserRole.CUSTOMER]!!,
        User(
            uid = "USER_SUPER_ADMIN_001",
            dealershipId = DEMO_DEALERSHIP_ID,
            name = "System Super Admin",
            email = "superadmin@automotive.com",
            role = UserRole.SUPER_ADMIN,
            stateCode = "ALL",
            storeId = "ALL",
            active = true
        ),
        User(
            uid = "USER_DEALERSHIP_ADMIN_001",
            dealershipId = DEMO_DEALERSHIP_ID,
            name = "Rajesh Sharma (Dealership Admin)",
            email = "admin@automotive.com",
            role = UserRole.DEALERSHIP_ADMIN,
            stateCode = "ALL",
            storeId = "ALL",
            active = true
        ),
        User(
            uid = "USER_ADMIN_001",
            dealershipId = DEMO_DEALERSHIP_ID,
            name = "Rajesh Sharma (Admin)",
            email = "admin.legacy@automotive.com",
            role = UserRole.ADMIN,
            stateCode = "ALL",
            storeId = "ALL",
            active = true
        ),
        User(
            uid = "USER_SM_TG_001",
            dealershipId = DEMO_DEALERSHIP_ID,
            name = "Venkatesh Rao (TG State Mgr)",
            email = "statemanager.tg@automotive.com",
            role = UserRole.STATE_MANAGER,
            stateCode = "TG",
            storeId = "ALL",
            active = true
        ),
        User(
            uid = "USER_SM_KA_001",
            dealershipId = DEMO_DEALERSHIP_ID,
            name = "Praveen Kumar (KA State Mgr)",
            email = "statemanager.ka@automotive.com",
            role = UserRole.STATE_MANAGER,
            stateCode = "KA",
            storeId = "ALL",
            active = true
        ),
        User(
            uid = "USER_STM_MAD_001",
            dealershipId = DEMO_DEALERSHIP_ID,
            name = "Srinivas Reddy (Store Mgr)",
            email = "storemanager.madhapur@automotive.com",
            role = UserRole.STORE_MANAGER,
            stateCode = "TG",
            storeId = "TG_Madhapur",
            active = true
        ),
        User(
            uid = "USER_SALES_MAD_001",
            dealershipId = DEMO_DEALERSHIP_ID,
            name = "Anil Verma (Sales Exec)",
            email = "sales.madhapur@automotive.com",
            role = UserRole.SALES_USER,
            stateCode = "TG",
            storeId = "TG_Madhapur",
            active = true
        ),
        User(
            uid = "USER_FIN_MAD_001",
            dealershipId = DEMO_DEALERSHIP_ID,
            name = "Pooja Mehta (Finance Exec)",
            email = "finance.madhapur@automotive.com",
            role = UserRole.FINANCE_USER,
            stateCode = "TG",
            storeId = "TG_Madhapur",
            active = true
        ),
        User(
            uid = "USER_CUST_001",
            dealershipId = DEMO_DEALERSHIP_ID,
            name = "Rajesh Kumar (Customer)",
            email = "customer.rajesh@gmail.com",
            role = UserRole.CUSTOMER,
            stateCode = "TG",
            storeId = "TG_Madhapur",
            active = true
        )
    )

    val customers = mutableListOf(
        Customer(
            customerId = "CUST_001",
            dealershipId = DEMO_DEALERSHIP_ID,
            fullName = "Rajesh Kumar",
            phone = "9876543210",
            email = "customer.rajesh@gmail.com",
            address = "Flat 402, Sai Residency, Madhapur",
            city = "Hyderabad",
            state = "Telangana",
            createdAt = now - (60L * DAY_IN_MS)
        ),
        Customer(
            customerId = "CUST_002",
            dealershipId = DEMO_DEALERSHIP_ID,
            fullName = "Suresh Reddy",
            phone = "9876543211",
            email = "suresh@gmail.com",
            address = "12th Main, 4th Cross, Indiranagar",
            city = "Bangalore",
            state = "Karnataka",
            createdAt = now - (45L * DAY_IN_MS)
        ),
        Customer(
            customerId = "CUST_003",
            dealershipId = DEMO_DEALERSHIP_ID,
            fullName = "Anita Sharma",
            phone = "9876543212",
            email = "anita@gmail.com",
            address = "Road No. 10, Jubilee Hills",
            city = "Hyderabad",
            state = "Telangana",
            createdAt = now - (20L * DAY_IN_MS)
        )
    )

    val loans = mutableListOf(
        Loan(
            loanId = "LOAN_001",
            dealershipId = DEMO_DEALERSHIP_ID,
            customerId = "CUST_001",
            bikeId = "BIKE_004",
            totalAmount = 140000.0,
            remainingBalance = 110000.0,
            emiAmount = 5500.0,
            nextEmiDate = now + (15L * DAY_IN_MS),
            upiAutoPayMandateHash = "MANDATE_HASH_UPI_99201",
            loanStatus = LoanStatus.ACTIVE,
            tenureMonths = 24,
            interestRate = 10.5,
            downPayment = 30000.0,
            createdAt = now - (45L * DAY_IN_MS)
        ),
        Loan(
            loanId = "LOAN_002",
            dealershipId = DEMO_DEALERSHIP_ID,
            customerId = "CUST_002",
            bikeId = "BIKE_003",
            totalAmount = 220000.0,
            remainingBalance = 200000.0,
            emiAmount = 8500.0,
            nextEmiDate = now - (5L * DAY_IN_MS),
            upiAutoPayMandateHash = "MANDATE_HASH_UPI_88102",
            loanStatus = LoanStatus.OVERDUE,
            tenureMonths = 24,
            interestRate = 11.0,
            downPayment = 40000.0,
            createdAt = now - (35L * DAY_IN_MS)
        )
    )

    val transactions = mutableListOf(
        Transaction(
            transactionId = "TXN_001",
            dealershipId = DEMO_DEALERSHIP_ID,
            customerId = "CUST_001",
            loanId = "LOAN_001",
            bikeId = "BIKE_004",
            amount = 5500.0,
            paymentMethod = "RAZORPAY",
            razorpayPaymentId = "pay_K123456789",
            status = TransactionStatus.SUCCESS,
            createdAt = now - (30L * DAY_IN_MS)
        ),
        Transaction(
            transactionId = "TXN_002",
            dealershipId = DEMO_DEALERSHIP_ID,
            customerId = "CUST_001",
            loanId = "LOAN_001",
            bikeId = "BIKE_004",
            amount = 5500.0,
            paymentMethod = "UPI",
            razorpayPaymentId = "pay_K987654321",
            status = TransactionStatus.SUCCESS,
            createdAt = now - (1L * DAY_IN_MS)
        )
    )

    val subscriptions = mutableListOf(
        Subscription(
            subscriptionId = "SUB_001",
            dealershipId = DEMO_DEALERSHIP_ID,
            planId = "BUSINESS",
            planName = "Business Plan",
            status = SubscriptionStatus.ACTIVE,
            billingCycle = BillingCycle.YEARLY,
            price = 69990.0,
            currency = "INR",
            startDate = now - (18L * DAY_IN_MS),
            endDate = now + (347L * DAY_IN_MS),
            trialStartDate = now - (18L * DAY_IN_MS),
            trialEndDate = now + (12L * DAY_IN_MS),
            nextBillingDate = now + (347L * DAY_IN_MS),
            provider = "RAZORPAY",
            providerCustomerId = "cust_demo_razor_001",
            providerSubscriptionId = "sub_demo_razor_001",
            createdAt = now - (18L * DAY_IN_MS),
            updatedAt = now - (18L * DAY_IN_MS)
        )
    )

    val auditLogs = mutableListOf(
        AuditLog(
            auditLogId = "LOG_001",
            dealershipId = DEMO_DEALERSHIP_ID,
            userId = "USER_ADMIN_001",
            action = "CREATE_BIKE",
            entityType = "Bike",
            entityId = "BIKE_001",
            timestamp = now - (67L * DAY_IN_MS),
            metadata = mapOf("model" to "Honda Activa 6G", "chassis" to "TEST001")
        ),
        AuditLog(
            auditLogId = "LOG_002",
            dealershipId = DEMO_DEALERSHIP_ID,
            userId = "USER_SALES_MAD_001",
            action = "CREATE_LOAN",
            entityType = "Loan",
            entityId = "LOAN_001",
            timestamp = now - (45L * DAY_IN_MS),
            metadata = mapOf("customerId" to "CUST_001", "amount" to "140000.0")
        ),
        AuditLog(
            auditLogId = "LOG_003",
            dealershipId = DEMO_DEALERSHIP_ID,
            userId = "USER_FIN_MAD_001",
            action = "EMI_PAYMENT",
            entityType = "Transaction",
            entityId = "TXN_001",
            timestamp = now - (30L * DAY_IN_MS),
            metadata = mapOf("amount" to "5500.0", "paymentMethod" to "RAZORPAY")
        )
    )

    val supportTickets = mutableListOf(
        SupportTicket(
            ticketId = "TICKET_001",
            dealershipId = DEMO_DEALERSHIP_ID,
            userId = "USER_STM_MAD_001",
            category = "Billing",
            subject = "Invoice Request for Annual Subscription",
            description = "Need GST tax invoice copy for the annual Professional plan renewal.",
            priority = TicketPriority.MEDIUM,
            createdAt = now - (5L * DAY_IN_MS),
            status = TicketStatus.IN_PROGRESS
        ),
        SupportTicket(
            ticketId = "TICKET_002",
            dealershipId = DEMO_DEALERSHIP_ID,
            userId = "USER_FIN_MAD_001",
            category = "Finance Integration",
            subject = "Razorpay Webhook Latency",
            description = "Payment status took 2 minutes to reflect in transactions screen.",
            priority = TicketPriority.LOW,
            createdAt = now - (2L * DAY_IN_MS),
            status = TicketStatus.OPEN
        )
    )
}
