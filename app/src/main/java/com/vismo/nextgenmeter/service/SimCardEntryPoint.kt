package com.vismo.nextgenmeter.service

import com.vismo.nextgenmeter.repository.RemoteMeterControlRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SimCardStateReceiverEntryPoint {
    fun moduleRestartManager(): ModuleRestartManager
    fun remoteMeterControlRepository(): RemoteMeterControlRepository
} 