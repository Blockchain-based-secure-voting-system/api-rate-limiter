package com.cutm.nt14.di

import com.cutm.nt14.data.repository.*
import com.cutm.nt14.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindEndpointRepository(
        impl: EndpointRepositoryImpl
    ): EndpointRepository

    @Binds
    @Singleton
    abstract fun bindIncidentRepository(
        impl: IncidentRepositoryImpl
    ): IncidentRepository
}
