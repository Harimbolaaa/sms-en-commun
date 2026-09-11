package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de données locale Room pour la persistance permanente :
 * 1. Historique des logs et statuts
 * 2. Modèles Regex personnalisables
 * 3. File d'attente (Queue) de secours et mécanisme de Retry pour les requêtes HTTP en échec
 */
@Database(
    entities = [
        AutomationLogEntity::class,
        RegexRuleEntity::class,
        QueueItemEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun logDao(): AutomationLogDao
    abstract fun regexRuleDao(): RegexRuleDao
    abstract fun queueItemDao(): QueueItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autobridge_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
