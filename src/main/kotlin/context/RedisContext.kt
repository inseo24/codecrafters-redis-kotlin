package context

import model.ValueWithExpiry
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

data class RedisContext(
    val store: ConcurrentHashMap<String, ValueWithExpiry>,
    val isMaster: Boolean,
    val isReplica: Boolean,
    val replicas: MutableList<Socket> = mutableListOf(),
    var replId: String = "",
    var offset: Long = 0
) {
}