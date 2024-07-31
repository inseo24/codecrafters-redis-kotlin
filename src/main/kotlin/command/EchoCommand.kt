package command

import context.RedisContext
import util.RedisProtocolUtils.bulkStringReply
import util.RedisProtocolUtils.errorReply

class EchoCommand: Command {
    override fun execute(args: List<String>, context: RedisContext): String {
        if (args.size != 2) {
            return errorReply("ERR wrong number of arguments for 'echo' command")
        }
        return bulkStringReply(args[1])
    }
}