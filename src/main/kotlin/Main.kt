import config.parseConfig
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import server.RedisServer
import slave.RedisSlave

fun main(args: Array<String>) = runBlocking {
    val config = parseConfig(args)
    val server = RedisServer(config)

    if (config.masterHost != null && config.masterPort != null) {
        val slave = RedisSlave(config.masterHost, config.masterPort, config.port)
        launch { slave.run() }
    }

    server.start()
}