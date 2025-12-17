package gaming.xplay.data.network

import kotlinx.coroutines.flow.Flow

interface ConnectivityRepository {
    fun hasConnection(): Flow<Boolean>
}
