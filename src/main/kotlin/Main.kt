import kotlinx.coroutines.*
import java.io.*
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap

val store = ConcurrentHashMap<String, String>()

fun main() = runBlocking {
    val serverSocket = ServerSocket(6379).apply { reuseAddress = true }
    println("Server is running on port 6379")

    while (true) {
        val clientSocket = serverSocket.accept()
        launch(Dispatchers.IO) { handleClient(clientSocket) }
    }
}

suspend fun handleClient(socket: java.net.Socket) = coroutineScope {
    println("New client connected")
    socket.use {
        val reader = socket.getInputStream().bufferedReader()
        val writer = socket.getOutputStream().bufferedWriter()

        while (isActive) {
            val command = withContext(Dispatchers.IO) { readCommand(reader) }
            if (command.isEmpty()) break
            val response = processCommand(command)
            writer.write(response)
            writer.flush()
        }
    }
    println("Client disconnected")
}

fun readCommand(reader: BufferedReader): List<String> {
    val line = reader.readLine() ?: return emptyList()
    return if (line.startsWith("*")) {
        val count = line.substring(1).toInt()
        (1..count).mapNotNull {
            reader.readLine()  // $ line
            reader.readLine()  // actual argument
        }
    } else {
        listOf(line)
    }
}

fun processCommand(command: List<String>): String {
    return when (command.firstOrNull()?.uppercase()) {
        "PING" -> simpleStringReply("PONG")
        "ECHO" -> bulkStringReply(command.getOrElse(1) { "" })
        "SET" -> handleSet(command)
        "GET" -> handleGet(command)
        null -> errorReply("ERR no command")
        else -> errorReply("ERR unknown command '${command.first()}'")
    }
}

fun handleSet(command: List<String>): String {
    if (command.size < 3) {
        return errorReply("ERR wrong number of arguments for 'set' command")
    }
    val key = command[1]
    val value = command[2]
    store[key] = value
    return simpleStringReply("OK")
}

fun handleGet(command: List<String>): String {
    if (command.size != 2) {
        return errorReply("ERR wrong number of arguments for 'get' command")
    }
    val key = command[1]
    return bulkStringReply(store[key] ?: "")
}

fun simpleStringReply(str: String) = "+$str\r\n"
fun bulkStringReply(str: String) = "$${str.length}\r\n$str\r\n"
fun errorReply(str: String) = "-$str\r\n"