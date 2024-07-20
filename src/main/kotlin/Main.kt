import java.io.DataOutputStream
import java.net.ServerSocket

fun main(args: Array<String>) {
    val serverSocket = ServerSocket(6379)

    // Since the tester restarts your program quite often, setting SO_REUSEADDR
    // ensures that we don't run into 'Address already in use' errors
    serverSocket.reuseAddress = true

    // syntax: PING [message]
    serverSocket.accept().use { socket ->
            DataOutputStream(socket.getOutputStream()).use { out ->
                out.write("+PONG\r\n".toByteArray())
        }
    }
    println("accepted new connection")
}
