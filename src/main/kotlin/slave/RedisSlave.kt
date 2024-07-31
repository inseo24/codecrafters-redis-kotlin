package slave

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.Socket

class RedisSlave(private val masterHost: String, private val masterPort: Int) {
    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun run() {
        while (true) {
            sendPingToMaster(masterHost, masterPort)
            delay(1000)
        }
    }

    private suspend fun sendPingToMaster(masterHost: String, masterPort: Int) {
        try {
            withContext(Dispatchers.IO) {
                val socket = Socket(masterHost, masterPort)
                socket.use {
                    val writer = socket.getOutputStream().bufferedWriter()
                    val reader = socket.getInputStream().bufferedReader()

                    writer.write("*1\r\n$4\r\nPING\r\n")
                    writer.flush()

                    val response = reader.readLine()
                    println("Received from master: $response")
                }
            }
        } catch (e: Exception) {
            println("Failed to connect to master: ${e.message}")
        }
    }
}
