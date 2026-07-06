package app.openscanner.android.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DocumentEntity::class, PageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class OpenScannerDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun pageDao(): PageDao

    companion object {
        fun build(context: Context): OpenScannerDatabase =
            Room.databaseBuilder(context, OpenScannerDatabase::class.java, "openscanner.db")
                .build()
    }
}
