package org.mobilenativefoundation.storex.paging.testUtils

import org.mobilenativefoundation.storex.paging.runtime.Identifier

data class CursorIdentifier(val cursor: String) : Identifier<CursorIdentifier> {
    // Splitting the cursor into two components for efficient comparison
    private val timestamp: Long
    private val uniqueId: String

    init {
        // Parsing the cursor string into its components
        val parts = cursor.split('-')
        require(parts.size == 2) { "Invalid cursor format. Expected 'timestamp-uniqueId'" }

        // Converting the timestamp string to a Long for numerical comparison
        timestamp = parts[0].toLongOrNull() ?: throw IllegalArgumentException("Invalid timestamp in cursor")
        uniqueId = parts[1]
    }

    override fun minus(other: CursorIdentifier): Int {
        // Compare timestamps
        val timeDiff = this.timestamp - other.timestamp

        return when {
            // If timestamps are different, use their difference
            // Coercing to Int range to avoid overflow issues
            timeDiff != 0L -> timeDiff.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()

            // If timestamps are the same, compare the unique parts lexicographically
            // This ensures a consistent, deterministic ordering
            else -> this.uniqueId.compareTo(other.uniqueId)
        }
    }
}
