package command

import context.RedisContext
import util.RedisProtocolUtils.errorReply
import java.nio.charset.StandardCharsets

class PSyncCommand : Command {
    override fun execute(args: List<String>, context: RedisContext): String {
        if (args.size < 3) {
            return errorReply("ERR wrong number of arguments for 'psync' command")
        }
        var replId = args[1]
        var offset = args[2].toLongOrNull()
            ?: return errorReply("ERR value is not an integer or out of range")

        if (replId == "?") {
            replId = generateReplId()
        }
        if (offset == -1L) {
            offset = 0L
        }

        context.replId = replId
        context.offset = offset

        val fullResyncResponse = "+FULLRESYNC $replId $offset\r\n"
        val emptyRDBFileResponse = generateEmptyRDBFileResponse()

        return fullResyncResponse + emptyRDBFileResponse
    }

    private fun generateReplId(): String {
        return (1..40).map { ('a'..'z').random() }.joinToString("")
    }

    private fun generateEmptyRDBFileResponse(): String {
        val emptyRDBFileHex = "524544495330303131fa0972656469732d76657205372e322e30fa0a72656469732d62697473c040fa056374696d65c26d08bc65fa08757365642d6d656dc2b0c41000fa08616f662d62617365c000fff06e3bfec0ff5aa2"
        val emptyRDBFileBytes = emptyRDBFileHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val length = emptyRDBFileBytes.size
        return "\$$length\r\n${String(emptyRDBFileBytes, StandardCharsets.ISO_8859_1)}"
    }
}
