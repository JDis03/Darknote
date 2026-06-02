# Keep main class
-keep class com.darknote.desktop.MainKt { *; }

# Keep Koin
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Keep Dropbox SDK
-keep class com.dropbox.** { *; }
-dontwarn com.dropbox.**

# Keep SQLDelight
-keep class com.darknote.persistence.database.** { *; }
-dontwarn app.cash.sqldelight.**

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keep,includedescriptorclasses class com.darknote.**$$serializer { *; }
-keepclassmembers class com.darknote.** {
    *** Companion;
}
-keepclasseswithmembers class com.darknote.** {
    kotlinx.serialization.KSerializer serializer(...);
}
