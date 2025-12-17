package gaming.xplay.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import gaming.xplay.data.network.ConnectivityRepository
import gaming.xplay.data.network.ConnectivityRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectivityModule {

    @Binds
    abstract fun bindConnectivityRepository(
        impl: ConnectivityRepositoryImpl
    ): ConnectivityRepository
}
