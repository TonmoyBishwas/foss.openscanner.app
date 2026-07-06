package app.openscanner.android.core

import android.content.Context
import app.openscanner.android.core.vision.HybridQuadDetector
import app.openscanner.android.core.vision.MlQuadDetector
import app.openscanner.android.data.DocumentRepository
import app.openscanner.android.data.SettingsRepository
import app.openscanner.android.data.db.OpenScannerDatabase

/** Tiny manual DI: one database, one repository, built once per process. */
class AppContainer(private val context: Context) {
    private val database = OpenScannerDatabase.build(context)
    val documentRepository = DocumentRepository(context, database)
    val settingsRepository = SettingsRepository(context)

    /** Lazy so the 5MB model loads on first scan, not at app start. */
    val quadDetector: HybridQuadDetector by lazy {
        HybridQuadDetector(MlQuadDetector.create(context))
    }
}
