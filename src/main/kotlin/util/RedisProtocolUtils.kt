package util

object RedisProtocolUtils {
    fun simpleStringReply(str: String) = "+$str\r\n"
    fun bulkStringReply(str: String) = "$${str.length}\r\n$str\r\n"
    fun nullBulkStringReply() = "$-1\r\n"
    fun errorReply(str: String) = "-$str\r\n"
}