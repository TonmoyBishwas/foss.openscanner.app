package app.openscanner.android

import android.app.Application
import android.util.Log
import org.opencv.android.OpenCVLoader

class OpenScannerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!OpenCVLoader.initLocal()) {
            Log.e("OpenScanner", "OpenCV failed to load")
        }
    }
}
