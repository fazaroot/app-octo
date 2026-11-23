# Aturan ProGuard CloneX.
# Pertahankan penerima Device Admin dan entity Room.
-keep public class com.clonex.app.provisioning.CloneXDeviceAdminReceiver
-keep class com.clonex.app.data.db.** { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
