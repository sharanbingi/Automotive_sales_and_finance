# Jetpack Compose rules
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Firebase Auth, Firestore, Storage, Messaging, Functions
-keep class com.google.firebase.** { *; }
-keepclassmembers class com.google.firebase.** { *; }

# Preserve App Data Models
-keep class com.automotive.salesfinance.model.** { *; }
-keepclassmembers class com.automotive.salesfinance.model.** { *; }

# Kotlin Coroutines & StateFlow / Flow
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }
-keep class kotlin.coroutines.** { *; }
-keepclassmembers class kotlin.coroutines.** { *; }

# Room
-keep class androidx.room.** { *; }
-keepclassmembers class androidx.room.** { *; }

# Moshi / Serialization
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class com.squareup.moshi.** { *; }
-keep class kotlinx.serialization.** { *; }
