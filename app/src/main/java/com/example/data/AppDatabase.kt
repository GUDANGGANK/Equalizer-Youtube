package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [PresetEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "horeg_eq_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialPresets(database.presetDao())
                    }
                }
            }

            suspend fun populateInitialPresets(dao: PresetDao) {
                if (dao.getPresetCount() == 0) {
                    val defaultPresets = listOf(
                        PresetEntity(
                            name = "HOREG BASS GLERR (60Hz)",
                            channelABands = "13,9,3,7,11",
                            channelBBands = "13,9,3,7,11",
                            subBoost = 950,
                            masterGain = 750,
                            midHighBoost = 720,
                            isSystemPreset = true
                        ),
                        PresetEntity(
                            name = "HOREG JEDAG-JEDUG",
                            channelABands = "11,8,-1,6,10",
                            channelBBands = "11,8,-1,6,10",
                            subBoost = 900,
                            masterGain = 800,
                            midHighBoost = 850,
                            isSystemPreset = true
                        ),
                        PresetEntity(
                            name = "BASS NENDANG & CRISPY",
                            channelABands = "9,6,1,6,9",
                            channelBBands = "9,6,1,6,9",
                            subBoost = 850,
                            masterGain = 600,
                            midHighBoost = 780,
                            isSystemPreset = true
                        ),
                        PresetEntity(
                            name = "VOCAL & MID-HIGH BERASA",
                            channelABands = "1,4,8,10,12",
                            channelBBands = "1,4,8,10,12",
                            subBoost = 400,
                            masterGain = 500,
                            midHighBoost = 950,
                            isSystemPreset = true
                        ),
                        PresetEntity(
                            name = "EDM SUB DROP",
                            channelABands = "12,7,0,5,10",
                            channelBBands = "12,7,0,5,10",
                            subBoost = 980,
                            masterGain = 700,
                            midHighBoost = 750,
                            isSystemPreset = true
                        ),
                        PresetEntity(
                            name = "ROCK & METAL LIVE",
                            channelABands = "7,4,-1,5,8",
                            channelBBands = "7,4,-1,5,8",
                            subBoost = 700,
                            masterGain = 650,
                            midHighBoost = 650,
                            isSystemPreset = true
                        ),
                        PresetEntity(
                            name = "FLAT MONITOR",
                            channelABands = "0,0,0,0,0",
                            channelBBands = "0,0,0,0,0",
                            subBoost = 0,
                            masterGain = 0,
                            midHighBoost = 0,
                            isSystemPreset = true
                        )
                    )
                    dao.insertAll(defaultPresets)
                }
            }
        }
    }
}
