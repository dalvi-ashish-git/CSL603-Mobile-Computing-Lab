package com.yassineabou.calculator.data.repository

import com.yassineabou.calculator.data.local.PreviousOperationDao
import com.yassineabou.calculator.data.model.PreviousOperation
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/**
 * Repository for managing calculator history data.
 * 
 * This repository acts as a single source of truth for calculator history,
 * abstracting the data source (Room database) from the rest of the application.
 * 
 * Architecture benefits:
 * - Separates data access logic from business logic
 * - Provides a clean API for ViewModels
 * - Enables easy testing with fake implementations
 * - Allows for multiple data sources (local, remote) in the future
 * 
 * Flow optimization:
 * - Uses distinctUntilChanged() to prevent unnecessary UI updates
 * - Only emits when the actual data changes, not on every database query
 * 
 * @property previousOperationDao The DAO for database operations (injected by Hilt)
 */
class PreviousOperationRepository @Inject constructor(
    private val previousOperationDao: PreviousOperationDao,
) {

    /**
     * Flow of all calculator operations from the database.
     * 
     * Features:
     * - Emits ordered list (most recent first)
     * - Only emits when data actually changes (distinctUntilChanged)
     * - Automatically updates UI when history is modified
     * 
     * Usage in ViewModel:
     * ```kotlin
     * repository.listPreviousOperationsFlow.collect { operations ->
     *     // Update UI with operations
     * }
     * ```
     */
    val listPreviousOperationsFlow =
        previousOperationDao.getListPreviousOperations().distinctUntilChanged()

    /**
     * Inserts a new calculator operation into the history.
     * 
     * This is a suspend function that performs the database operation
     * on a background thread. Should be called from a coroutine.
     * 
     * @param previousOperation The calculation to save in history
     */
    suspend fun insert(previousOperation: PreviousOperation) {
        previousOperationDao.insert(previousOperation)
    }

    /**
     * Clears all calculator history from the database.
     * 
     * This operation deletes all records from the history table.
     * Should be called when the user wants to clear their calculation history.
     */
    suspend fun clear() {
        previousOperationDao.clear()
    }
}
