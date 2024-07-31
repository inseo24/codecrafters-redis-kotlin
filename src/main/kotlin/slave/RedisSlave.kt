package slave

import kotlinx.coroutines.*
import util.RedisProtocolUtils
import java.net.Socket

class RedisSlave(private val masterHost: String, private val masterPort: Int, private val slavePort: Int) {
    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun run() {
        while (true) {
            try {
                connectToMaster()
            } catch (e: Exception) {
                println("Failed to connect to master: ${e.message}")
                delay(1000) // 재연결 전 대기
            }
        }
    }

    private suspend fun connectToMaster() {
        withContext(Dispatchers.IO) {
            Socket(masterHost, masterPort).use { socket ->
                val writer = socket.getOutputStream().bufferedWriter()
                val reader = socket.getInputStream().bufferedReader()

                // PING 전송
                writer.write(RedisProtocolUtils.replyPing())
                writer.flush()

                // PING 응답 확인
                val pingResponse = reader.readLine()
                if (pingResponse == "+PONG") {
                    // REPLCONF listening-port 전송
                    writer.write(RedisProtocolUtils.replyReplConfListeningPort(slavePort))
                    writer.flush()
                    val portResponse = reader.readLine()
                    println("REPLCONF listening-port response: $portResponse")

                    // REPLCONF capa psync2 전송
                    writer.write(RedisProtocolUtils.replyReplConfCapaPsync2())
                    writer.flush()
                    val capaResponse = reader.readLine()
                    println("REPLCONF capa psync2 response: $capaResponse")

                    // PSYNC 전송
                    writer.write(RedisProtocolUtils.replyPsync())
                    writer.flush()
                } else {
                    println("Unexpected response to PING: $pingResponse")
                }
            }
        }
    }
}