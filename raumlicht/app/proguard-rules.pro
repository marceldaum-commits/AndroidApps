-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Sprach-Bibliothek sherpa-onnx: Ihr Maschinencode greift per Namen auf diese Klassen und Felder zu – nichts umbenennen oder entfernen
-keep class com.k2fsa.sherpa.onnx.** { *; }
