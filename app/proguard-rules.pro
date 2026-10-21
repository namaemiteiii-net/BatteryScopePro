# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /sdk/tools/proguard/proguard-android.txt

# Keep Room entities
-keep class com.batteryscope.pro.data.model.** { *; }
-keep class com.batteryscope.pro.data.db.** { *; }
