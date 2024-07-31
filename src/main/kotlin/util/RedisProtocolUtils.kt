package util

object RedisProtocolUtils {
    fun simpleStringReply(str: String) = "+$str\r\n"
    fun bulkStringReply(str: String) = "$${str.length}\r\n$str\r\n"
    fun nullBulkStringReply() = "$-1\r\n"
    fun errorReply(str: String) = "-$str\r\n"
    fun replyPing() = "*1\r\n$4\r\nPING\r\n"
    fun replyReplConfListeningPort(port: Int) = "*3\r\n$8\r\nREPLCONF\r\n$14\r\nlistening-port\r\n$${port.toString().length}\r\n$port\r\n"
    fun replyReplConfCapaPsync2() = "*3\r\n$8\r\nREPLCONF\r\n$4\r\ncapa\r\n$6\r\npsync2\r\n"
    fun okReply() = "+OK\r\n"
    fun replyPsync() = "*3\r\n$5\r\nPSYNC\r\n$1\r\n?\r\n$2\r\n-1\r\n"
    fun replyFullResync(replId: String) = "+FULLRESYNC $replId 0\r\n"
}