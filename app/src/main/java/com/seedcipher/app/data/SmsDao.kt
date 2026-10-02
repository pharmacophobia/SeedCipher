package com.seedcipher.app.data

import androidx.room.*

@Dao
interface SmsDao {
    @Query("SELECT * FROM cached_sms WHERE address = :address ORDER BY timestamp ASC")
    suspend fun getMessagesForAddress(address: String): List<CachedSms>

    @Query("SELECT * FROM cached_sms WHERE messageId = :messageId")
    suspend fun getMessageById(messageId: Long): CachedSms?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<CachedSms>)

    @Query("DELETE FROM cached_sms WHERE seedUsed != :currentSeed")
    suspend fun clearCacheForNewSeed(currentSeed: String)

    @Query("DELETE FROM cached_sms WHERE timestamp < :threshold")
    suspend fun pruneOldMessages(threshold: Long)

    @Query("DELETE FROM cached_sms")
    suspend fun clearAll()
}
