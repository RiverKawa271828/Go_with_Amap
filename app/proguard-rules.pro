# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ============================================
# 高德地图 SDK - Amap
# ============================================
-keep class com.amap.api.** {*;}
-keep class com.autonavi.** {*;}
-keep class com.amap.api.** {*;}
-dontwarn com.amap.api.**
-dontwarn com.autonavi.**

# ============================================
# OkHttp
# ============================================
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ============================================
# Material Design / AndroidX
# ============================================
-keep class com.google.android.material.** { *; }
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# ============================================
# PreferenceFragment
# ============================================
-keep public class * extends android.preference.PreferenceFragment
-keep public class * extends androidx.preference.PreferenceFragmentCompat

# ============================================
# Application Classes
# ============================================
-keep class com.river.gowithamap.** { *; }

# ============================================
# Database Classes
# ============================================
-keep class * extends android.database.sqlite.SQLiteOpenHelper

# ============================================
# Keep Parcelable classes
# ============================================
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ============================================
# Keep Serializable classes
# ============================================
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================
# Keep native methods
# ============================================
-keepclasseswithmembernames class * {
    native <methods>;
}

# ============================================
# Keep view getters/setters
# ============================================
-keepclassmembers public class * extends android.view.View {
    void set*(***);
    *** get*();
}

# ============================================
# Keep Fragment constructors
# ============================================
-keepclassmembers class * extends android.app.Fragment {
    public <init>(...);
}
-keepclassmembers class * extends androidx.fragment.app.Fragment {
    public <init>(...);
}

# ============================================
# Keep enums
# ============================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
