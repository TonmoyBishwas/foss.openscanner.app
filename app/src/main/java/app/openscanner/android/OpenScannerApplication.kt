package app.openscanner.android

import android.app.Application
import android.util.Log
import app.openscanner.android.core.AppContainer
import org.opencv.android.OpenCVLoader

class OpenScannerApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        if (!OpenCVLoader.initLocal()) {
            Log.e("OpenScanner", "OpenCV failed to load")
        }
    }
}
