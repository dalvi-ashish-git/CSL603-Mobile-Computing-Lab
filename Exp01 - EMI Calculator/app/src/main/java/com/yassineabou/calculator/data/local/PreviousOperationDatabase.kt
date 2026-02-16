package com.yassineabou.calculator.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.yassineabou.calculator.data.model.PreviousOperation

/**
 * Room database for storing calculator history.
 * 
 * Database configuration:
 * - Version: 1 (increment when schema changes)
 * - Entities: PreviousOperation (calculator history records)
 * - exportSchema: false (set to true for production to track schema versions)
 * 
 * Room best practices:
 * - Abstract class extending RoomDatabase
 * - Abstract functions for DAOs
 * - Singleton instance provided by Hilt (see DatabaseModule)
 * 
 * Migration strategy:
 * For production apps, implement proper migrations when schema changes:
 * ```kotlin
 * @Database(
 *     entities = [PreviousOperation::class],
 *     version = 2,
 *     exportSchema = true
 * )
 * // Then add migrations in DatabaseModule
 * ```
 * 
 * Database location:
 * - Internal storage: /data/data/com.yassineabou.calculator/databases/
 * - Pre-populated from: assets/database/previous_operation.db
 */
@Database(entities = [PreviousOperation::class], version = 1, exportSchema = false)
abstract class PreviousOperationDatabase : RoomDatabase() {

    /**
     * Provides access to the DAO for calculator history operations.
     * 
     * Room generates the implementation at compile time.
     * 
     * @return The DAO instance for database operations
     */
    abstract fun getPreviousOperationDao(): PreviousOperationDao
}
