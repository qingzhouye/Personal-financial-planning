# ProGuard rules
# Keep Room entities
-keep class com.finance.loanmanager.data.entity.** { *; }

# Keep Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
