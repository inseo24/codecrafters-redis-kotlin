import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket

fun main() = runBlocking {
    val serverSocket = ServerSocket(6379)
    serverSocket.reuseAddress = true

    println("Server is running on port 6379")

    while (true) {
        val clientSocket = serverSocket.accept()
        launch(Dispatchers.IO) {
            handleClient(clientSocket)
        }
    }
}

suspend fun handleClient(socket: java.net.Socket) = coroutineScope {
    println("New client connected")
    socket.use {
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

        while (isActive) {
            val command = withContext(Dispatchers.IO) { readCommand(reader) }
            when {
                command == "PING" -> {
                    writer.write("+PONG\r\n")
                }
                command.startsWith("PING ") -> {
                    val message = command.substring(5)
                    writer.write("$${message.length}\r\n$message\r\n")
                }
                command == "QUIT" -> {
                    println("Client disconnected")
                    break
                }
                else -> {
                    writer.write("-ERR unknown command\r\n")
                }
            }
            writer.flush()
        }
    }
}

fun readCommand(reader: BufferedReader): String {
    val line = reader.readLine() ?: return ""
    if (line.startsWith("*")) {
        val count = line.substring(1).toInt()
        repeat(count * 2 - 1) { reader.readLine() }
        return reader.readLine() ?: ""
    }
    return line
}