package com.vismo.nextgenmeter.service

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SimCardStateReceiverEntryPoint {
    fun moduleRestartManager(): ModuleRestartManager
} 