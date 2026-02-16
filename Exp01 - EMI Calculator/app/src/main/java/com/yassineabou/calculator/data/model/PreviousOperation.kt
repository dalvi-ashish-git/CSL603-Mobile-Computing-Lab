package com.yassineabou.calculator.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity class representing a calculator operation stored in the history database.
 * 
 * This data class is used by Room to create the "previous_operation" table.
 * Each instance represents a completed calculation with its input expression and result.
 * 
 * Room annotations:
 * - @Entity: Marks this class as a database table
 * - @PrimaryKey: Marks the id field as the primary key with auto-generation
 * 
 * Use cases:
 * - Storing calculator history for later reference
 * - Allowing users to reuse previous calculation results
 * - Providing calculation history in the UI
 * 
 * @property input The mathematical expression entered by the user (e.g., "5+3*2")
 * @property result The calculated result of the expression (e.g., "11")
 * @property id Auto-generated unique identifier for the database record
 */
@Entity(tableName = "previous_operation")
data class PreviousOperation(
    val input: String,
    val result: String,
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
)
