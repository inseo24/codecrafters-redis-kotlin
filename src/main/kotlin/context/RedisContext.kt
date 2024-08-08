package context

import model.ValueWithExpiry
import java.util.concurrent.ConcurrentHashMap

data class RedisContext(
    val store: ConcurrentHashMap<String, ValueWithExpiry>,
    val isReplica: Boolean,
    var replId: String = "",
    var offset: Long = 0
) {
}