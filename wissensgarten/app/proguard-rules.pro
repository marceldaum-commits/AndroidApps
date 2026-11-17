-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Natürliche Stimme „Thorsten“: sherpa-onnx liest Felder per JNI – nichts umbenennen oder entfernen
-keep class com.k2fsa.sherpa.onnx.** { *; }
