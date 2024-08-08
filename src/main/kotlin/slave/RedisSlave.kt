package slave

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import util.RedisProtocolUtils.replyPing
import util.RedisProtocolUtils.replyPsync
import util.RedisProtocolUtils.replyReplConfCapaPsync2
import util.RedisProtocolUtils.replyReplConfListeningPort
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
                writer.write(replyPing())
                writer.flush()

                // PING 응답 확인
                val pingResponse = reader.readLine()
                if (pingResponse == "+PONG") {
                    // REPLCONF listening-port 전송
                    writer.write(replyReplConfListeningPort(slavePort))
                    writer.flush()
                    val portResponse = reader.readLine()
                    println("REPLCONF listening-port response: $portResponse")

                    // REPLCONF capa psync2 전송
                    writer.write(replyReplConfCapaPsync2())
                    writer.flush()
                    val capaResponse = reader.readLine()
                    println("REPLCONF capa psync2 response: $capaResponse")

                    // PSYNC 전송
                    writer.write(replyPsync())
                    writer.flush()
                    val psyncResponse = reader.readLine()
                    println("PSYNC response: $psyncResponse")

                    // PSYNC 응답 처리
                    if (psyncResponse.startsWith("+FULLRESYNC")) {
                        val parts = psyncResponse.split(" ")
                        if (parts.size != 3) {
                            println("Unexpected FULLRESYNC response format")
                        }
                        val replId = parts[1]
                        val offset = parts[2]
                        println("Received FULLRESYNC with REPL_ID: $replId and offset: $offset")
                    } else {
                        println("Unexpected response to PSYNC: $psyncResponse")
                    }
                } else {
                    println("Unexpected response to PING: $pingResponse")
                }
            }
        }
    }
}