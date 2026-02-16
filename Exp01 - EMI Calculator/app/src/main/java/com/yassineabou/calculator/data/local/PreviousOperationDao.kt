package com.yassineabou.calculator.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.yassineabou.calculator.data.model.PreviousOperation
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for calculator history operations.
 * 
 * This interface defines database operations for the PreviousOperation entity.
 * Room generates the implementation automatically at compile time.
 * 
 * Features:
 * - Suspend functions for asynchronous database operations
 * - Flow-based queries for reactive data observation
 * - Type-safe SQL queries
 * 
 * Best practices:
 * - All database operations are suspend functions (coroutine-friendly)
 * - Read operations return Flow for automatic UI updates
 * - Write operations use suspend for background execution
 */
@Dao
interface PreviousOperationDao {

    /**
     * Inserts a new calculator operation into the history database.
     * 
     * This is a suspend function that should be called from a coroutine.
     * The operation is performed on a background thread by Room.
     * 
     * Conflict strategy:
     * - Default is OnConflictStrategy.ABORT
     * - For duplicate IDs, consider adding @Insert(onConflict = OnConflictStrategy.REPLACE)
     * 
     * @param previousOperation The calculation to save in history
     */
    @Insert
    suspend fun insert(previousOperation: PreviousOperation)

    /**
     * Deletes all calculator history from the database.
     * 
     * This operation clears the entire history table.
     * Should be called when the user wants to clear their calculation history.
     * 
     * Implementation note:
     * - This is a suspend function for background execution
     * - UI will automatically update via Flow observation
     */
    @Query("DELETE FROM previous_operation")
    suspend fun clear()

    /**
     * Retrieves all calculator operations from the database, ordered by most recent first.
     * 
     * Returns:
     * - Flow<List<PreviousOperation>>: Reactive stream of calculator history
     * - Emits new values whenever the database changes
     * - Automatically updates UI when history is added/removed
     * 
     * Query details:
     * - Orders by ID in descending order (most recent first)
     * - Returns all columns from the previous_operation table
     * 
     * Usage in ViewModel:
     * ```kotlin
     * val historyFlow = dao.getListPreviousOperations()
     * // Collect in a coroutine:
     * historyFlow.collect { operations -> updateUI(operations) }
     * ```
     * 
     * @return Flow emitting the list of all previous calculator operations
     */
    @Query("SELECT * FROM previous_operation ORDER BY id DESC")
    fun getListPreviousOperations(): Flow<List<PreviousOperation>>
}
