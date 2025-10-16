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
 * 
 * Features:
 * - Type converters for custom types (SyncStatus enum)
 * - Migration support for future schema changes
 * - Singleton pattern via Hilt dependency injection
 */
@Database(
    entities = [UserEntity::class, LocalDataEntity::class],
    version = 1,
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
         * Example migration from version 1 to 2 (for future use).
         * When you need to modify the schema, add migrations here.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example: Add a new column to users table
                // database.execSQL("ALTER TABLE users ADD COLUMN phoneNumber TEXT")
            }
        }
    }
}
