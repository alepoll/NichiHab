package com.nichihab.app.util

/**
 * In-memory cache of recent photo perceptual hashes (dHash) for duplicate detection.
 *
 * Uses an ArrayDeque with automatic expiry of entries older than
 * [Constants.PHOTO_HASH_CACHE_EXPIRY_HOURS]. Follows the same pattern as
 * the AI result cache in [com.nichihab.app.data.repository.AiRepositoryImpl].
 */
object PhotoHashCache {

    private data class Entry(val hash: Long, val timestamp: Long)

    private val entries = ArrayDeque<Entry>()

    /**
     * Returns true if a photo with the given [hash] is similar to a recently
     * submitted photo, within [threshold] bits of Hamming distance.
     */
    fun isDuplicate(hash: Long, threshold: Int = Constants.PHOTO_DUPLICATE_HAMMING_THRESHOLD): Boolean {
        evictExpired()
        return entries.any { (hash xor it.hash).countOneBits() <= threshold }
    }

    /**
     * Records the hash of a photo that was submitted for verification.
     */
    fun addHash(hash: Long) {
        evictExpired()
        entries.addLast(Entry(hash, System.currentTimeMillis()))
        while (entries.size > Constants.PHOTO_HASH_CACHE_MAX_SIZE) {
            entries.removeFirst()
        }
    }

    /**
     * Clears all cached hashes (useful when starting a new session).
     */
    fun clear() {
        entries.clear()
    }

    private fun evictExpired() {
        val cutoff = System.currentTimeMillis() - Constants.PHOTO_HASH_CACHE_EXPIRY_HOURS * 60 * 60 * 1000L
        while (entries.isNotEmpty() && entries.first().timestamp < cutoff) {
            entries.removeFirst()
        }
    }
}
