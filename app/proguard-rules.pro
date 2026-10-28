# ProGuard / R8 configuration for Memo Bills (Play Store Release Readiness)

# Keep Room database, DAOs, entities, and type converters
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.example.core.database.** { *; }

# Keep enum models serialized by Room TypeConverters
-keepclassmembers enum com.example.core.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep BroadcastReceiver for local bill reminders & reboot rescheduling
-keep class com.example.core.notifications.MemoReminderReceiver { *; }

# Keep Kotlin metadata and coroutines for Room + Compose
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod
-dontwarn kotlinx.coroutines.**
