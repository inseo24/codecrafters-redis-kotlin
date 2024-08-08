package server

import config.Config
import slave.RedisSlave
import command.*
import context.RedisContext
import kotlinx.coroutines.*
import model.ValueWithExpiry
import util.CommandReader.readCommand
import util.RedisProtocolUtils.errorReply
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

class RedisServer(private val config: Config) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val store = ConcurrentHashMap<String, ValueWithExpiry>()
    private val context = RedisContext(store, true, false)
    private val commands = mapOf(
        "PING" to PingCommand(),
        "ECHO" to EchoCommand(),
        "GET" to GetCommand(),
        "SET" to SetCommand(),
        "INFO" to InfoCommand(),
        "REPLCONF" to ReplConfCommand(),
        "PSYNC" to PSyncCommand(),
    )

    fun start() = runBlocking {
        val serverJob = scope.launch { runServer() }

        if (config.masterHost != null && config.masterPort != null) {
            val slaveJob = scope.launch {
                RedisSlave(config.masterHost, config.masterPort, config.port, context).run()
            }
        }

        serverJob.join()
    }

    private suspend fun runServer() {
        val serverSocket = ServerSocket(config.port).apply { reuseAddress = true }
        println("Server is running on port ${config.port}")

        while (true) {
            val clientSocket = serverSocket.accept()
            scope.launch { handleClient(clientSocket) }
        }
    }

    private suspend fun handleClient(socket: Socket) = coroutineScope {
        println("New client connected")
        socket.use {
            val reader = socket.getInputStream().bufferedReader()
            val writer = socket.getOutputStream().bufferedWriter(StandardCharsets.ISO_8859_1)

            while (isActive) {
                val command = withContext(Dispatchers.IO) { readCommand(reader) }
                if (command.isEmpty()) break
                val response = processCommand(command)
                writer.write(response)
                writer.flush()

                if (context.isMaster) {
                    propagateCommand(command)
                }
            }
        }
        println("Client disconnected")
    }

    private fun processCommand(command: List<String>): String {
        val cmd = commands[command.firstOrNull()?.uppercase()]
        return cmd?.execute(command, context) ?: errorReply("ERR unknown command '${command.first()}'")
    }

    private fun propagateCommand(command: List<String>) {
        val respCommand = command.joinToString(
            separator = "\r\n",
            prefix = "*${command.size}\r\n",
            postfix = "\r\n"
        ) { "\$${it.length}\r\n$it" }

        context.replicas.forEach { replicaSocket ->
            try {
                val writer = replicaSocket.getOutputStream().bufferedWriter(StandardCharsets.ISO_8859_1)
                writer.write(respCommand)
                writer.flush()
            } catch (e: Exception) {
                println("Failed to propagate command to replica: ${e.message}")
            }
        }
    }

    fun shutdown() {
        scope.cancel()
    }

    companion object {
        const val REPL_OFFSET = 0
        val REPL_ID: String = (1..40).map { ('a'..'z').random() }.joinToString("")
    }
}
