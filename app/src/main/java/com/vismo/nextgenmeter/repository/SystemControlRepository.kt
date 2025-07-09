package com.vismo.nextgenmeter.repository

interface SystemControlRepository {
    
    /**
     * Puts the device to sleep using PowerManager
     */
    suspend fun sleepDevice()
    
    /**
     * Shuts down the device safely
     */
    suspend fun shutdownDevice()
} 