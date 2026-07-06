package app.openscanner.android.core

import android.content.Context
import app.openscanner.android.data.DocumentRepository
import app.openscanner.android.data.SettingsRepository
import app.openscanner.android.data.db.OpenScannerDatabase

/** Tiny manual DI: one database, one repository, built once per process. */
class AppContainer(context: Context) {
    private val database = OpenScannerDatabase.build(context)
    val documentRepository = DocumentRepository(context, database)
    val settingsRepository = SettingsRepository(context)
}
