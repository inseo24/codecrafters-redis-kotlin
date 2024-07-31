import config.parseConfig
import kotlinx.coroutines.runBlocking
import server.RedisServer

fun main(args: Array<String>) = runBlocking {
    val config = parseConfig(args)
    val server = RedisServer(config)

    server.start()
}