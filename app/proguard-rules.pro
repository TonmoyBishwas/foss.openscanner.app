# OpenCV loads parts of itself via JNI; keep its Java surface intact.
-keep class org.opencv.** { *; }
