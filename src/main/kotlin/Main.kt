import kotlinx.coroutines.*
import java.io.*
import java.net.ServerSocket
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

data class ValueWithExpiry(val value: String, val expiryTime: Instant?)
val store = ConcurrentHashMap<String, ValueWithExpiry>()

fun main(args: Array<String>) = runBlocking {
    val portIndex = args.indices.firstOrNull { args[it] == "--port" || args[it] == "-p" }
    val port = if (portIndex != null && portIndex + 1 < args.size) args[portIndex + 1].toInt() else 6379

    val replicaIndex = args.indexOf("--replicaof")
    val masterHost = if (replicaIndex != -1 && replicaIndex + 1 < args.size) args[replicaIndex + 1] else null
    val masterPort = if (replicaIndex != -1 && replicaIndex + 2 < args.size) args[replicaIndex + 2].toIntOrNull() else null
    val isReplica = (replicaIndex != -1)

    val serverSocket = ServerSocket(port).apply { reuseAddress = true }
    println("Server is running on port $port")

    while (true) {
        val clientSocket = serverSocket.accept()
        launch(Dispatchers.IO) { handleClient(clientSocket, isReplica) }
    }
}

suspend fun handleClient(socket: java.net.Socket, isReplica: Boolean) = coroutineScope {
    println("New client connected")
    socket.use {
        val reader = socket.getInputStream().bufferedReader()
        val writer = socket.getOutputStream().bufferedWriter()

        while (isActive) {
            val command = withContext(Dispatchers.IO) { readCommand(reader) }
            if (command.isEmpty()) break
            val response = processCommand(command, isReplica)
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

fun processCommand(command: List<String>, isReplica: Boolean): String {
    return when (command.firstOrNull()?.uppercase()) {
        "PING" -> simpleStringReply("PONG")
        "ECHO" -> bulkStringReply(command.getOrElse(1) { "" })
        "SET" -> handleSet(command)
        "GET" -> handleGet(command)
        "INFO" -> bulkStringReply("role:${if (isReplica) "slave" else "master"}")
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
    var expiryTime: Instant? = null

    if (command.size > 3 && command[3].uppercase() == "PX") {
        if (command.size < 5) {
            return errorReply("ERR syntax error")
        }
        val milliseconds = command[4].toLongOrNull()
            ?: return errorReply("ERR value is not an integer or out of range")
        expiryTime = Instant.now().plusMillis(milliseconds)
    }

    store[key] = ValueWithExpiry(value, expiryTime)

    return simpleStringReply("OK")
}

fun handleGet(command: List<String>): String {
    if (command.size != 2) {
        return errorReply("ERR wrong number of arguments for 'get' command")
    }
    val key = command[1]
    val valueWithExpiry = store[key]

    return if (valueWithExpiry != null) {
        if (valueWithExpiry.expiryTime == null || Instant.now().isBefore(valueWithExpiry.expiryTime)) {
            bulkStringReply(valueWithExpiry.value)
        } else {
            store.remove(key)  // Remove expired key
            nullBulkStringReply()
        }
    } else {
        nullBulkStringReply()
    }
}

fun simpleStringReply(str: String) = "+$str\r\n"
fun bulkStringReply(str: String) = "$${str.length}\r\n$str\r\n"
fun nullBulkStringReply() = "$-1\r\n"
fun errorReply(str: String) = "-$str\r\n"