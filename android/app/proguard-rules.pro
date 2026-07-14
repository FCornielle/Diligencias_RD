# Reglas adicionales de ProGuard/R8 para DiligenciaRD.
# Retrofit + kotlinx.serialization
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keep,includedescriptorclasses class com.diligenciard.app.**$$serializer { *; }
-keepclassmembers class com.diligenciard.app.** { *** Companion; }
-keepclasseswithmembers class com.diligenciard.app.** { kotlinx.serialization.KSerializer serializer(...); }
