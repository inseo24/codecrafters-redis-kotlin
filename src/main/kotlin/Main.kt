import kotlinx.coroutines.*
import java.io.*
import java.net.ServerSocket

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
            val response = processCommand(command)
            writer.write(response)
            writer.flush()
        }
    }
}

fun readCommand(reader: BufferedReader): List<String> {
    val line = reader.readLine() ?: return emptyList()
    return if (line.startsWith("*")) {
        val count = line.substring(1).toInt()
        (1..count).map { reader.readLine(); reader.readLine() ?: "" }
    } else {
        listOf(line)
    }
}

fun processCommand(command: List<String>): String {
    return when (command.firstOrNull()?.uppercase()) {
        "PING" -> simpleStringReply("PONG")
        "ECHO" -> bulkStringReply(command.getOrElse(1) { "" })
        null -> errorReply("ERR no command")
        else -> errorReply("ERR unknown command '${command.first()}'")
    }
}

fun simpleStringReply(str: String) = "+$str\r\n"
fun bulkStringReply(str: String) = "$${str.length}\r\n$str\r\n"
fun errorReply(str: String) = "-$str\r\n"