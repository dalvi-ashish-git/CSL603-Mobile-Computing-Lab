package com.yassineabou.calculator.di

import android.content.Context
import androidx.room.Room
import com.yassineabou.calculator.data.local.PreviousOperationDao
import com.yassineabou.calculator.data.local.PreviousOperationDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt dependency injection module for providing database-related dependencies.
 * 
 * This module is installed in the SingletonComponent, ensuring that database instances
 * are created once and shared across the entire application lifecycle.
 * 
 * Provides:
 * - Room database instance for calculator history
 * - DAO (Data Access Object) for database operations
 * 
 * Best practices implemented:
 * - Singleton scope for database instance (prevents multiple instances)
 * - Pre-populated database from asset file
 * - Dependency injection for testability and loose coupling
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the Room database instance for the calculator application.
     * 
     * The database is:
     * - Created as a singleton (one instance per app lifecycle)
     * - Pre-populated with data from assets/database/previous_operation.db
     * - Named "calculator_history_database"
     * 
     * Room features enabled:
     * - Coroutines support via KTX extensions
     * - Flow-based reactive queries
     * - Type converters for complex data types
     * 
     * Migration strategy:
     * For production apps, implement proper migration strategies when schema changes.
     * Example: .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
     * 
     * @param app Application context (injected by Hilt)
     * @return The singleton Room database instance
     */
    @Singleton
    @Provides
    fun providePreviousOperationDatabase(
        @ApplicationContext app: Context,
    ) = Room.databaseBuilder(
        app,
        PreviousOperationDatabase::class.java,
        "calculator_history_database",
    )
        .createFromAsset("database/previous_operation.db") // Pre-populate from assets
        .fallbackToDestructiveMigration() // For development; use proper migrations in production
        .build()

    /**
     * Provides the DAO (Data Access Object) for calculator history operations.
     * 
     * The DAO provides:
     * - Suspend functions for database operations (coroutine-friendly)
     * - Flow-based queries for reactive data observation
     * - Type-safe database access
     * 
     * Operations available:
     * - Insert new calculation results
     * - Query all previous operations
     * - Clear history
     * 
     * @param previousOperationDatabase The Room database instance
     * @return The DAO instance for database operations
     */
    @Singleton
    @Provides
    fun providePreviousOperationDao(previousOperationDatabase: PreviousOperationDatabase):
        PreviousOperationDao {
        return previousOperationDatabase.getPreviousOperationDao()
    }
}
