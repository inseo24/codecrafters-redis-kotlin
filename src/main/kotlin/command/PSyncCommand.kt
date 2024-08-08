package command

import context.RedisContext
import util.RedisProtocolUtils.bulkStringReply
import util.RedisProtocolUtils.errorReply

class PSyncCommand: Command {
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

        return bulkStringReply("FULLRESYNC $replId $offset")
    }

    private fun generateReplId(): String {
        return (1..40).map { ('a'..'z').random() }.joinToString("")
    }
}