package com.example.modicanalyzer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.modicanalyzer.data.local.dao.LocalDataDao
import com.example.modicanalyzer.data.local.dao.UserDao
import com.example.modicanalyzer.data.local.entity.LocalDataEntity
import com.example.modicanalyzer.data.local.entity.UserEntity

/**
 * Main Room Database for the application.
 * 
 * Manages two tables:
 * 1. users - Stores user authentication data with offline-first support
 * 2. local_data - Stores user-generated data that syncs to Firestore
 * 
 * Version 1: Initial database schema
 * Version 2: Added encryptedPassword field to UserEntity for offline user sync
 * 
 * Features:
 * - Type converters for custom types (SyncStatus enum)
 * - Migration support for future schema changes
 * - Singleton pattern via Hilt dependency injection
 */
@Database(
    entities = [UserEntity::class, LocalDataEntity::class],
    version = 2,
    exportSchema = false  // Set to true and provide schema location in production
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * Provides access to User table operations.
     */
    abstract fun userDao(): UserDao
    
    /**
     * Provides access to LocalData table operations.
     */
    abstract fun localDataDao(): LocalDataDao
    
    companion object {
        const val DATABASE_NAME = "modicare_offline_db"
        
        /**
         * Migration from version 1 to 2: Add encryptedPassword column.
         * This field stores encrypted password for offline users until they sync to Firebase.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add encryptedPassword column to users table
                database.execSQL("ALTER TABLE users ADD COLUMN encryptedPassword TEXT DEFAULT NULL")
            }
        }
    }
}
