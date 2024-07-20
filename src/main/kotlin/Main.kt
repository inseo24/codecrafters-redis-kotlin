import java.io.DataOutputStream
import java.net.ServerSocket

fun main(args: Array<String>) {
    val serverSocket = ServerSocket(6379)

    // Since the tester restarts your program quite often, setting SO_REUSEADDR
    // ensures that we don't run into 'Address already in use' errors
    serverSocket.reuseAddress = true

    // syntax: PING [message]
    serverSocket.accept().use { socket ->
        val input = socket.getInputStream().bufferedReader()
        val output = DataOutputStream(socket.getOutputStream())

        while (true) {
            val request = input.readLine() ?: break

            val response = when {
                request.startsWith("PING") -> {
                    val message = request.substringAfter("PING").trim()
                    "PONG $message"
                }
                else -> "ERROR unknown command"
            }

            output.writeBytes("$response\n")
            output.flush()
        }
    }
    println("accepted new connection")
}
