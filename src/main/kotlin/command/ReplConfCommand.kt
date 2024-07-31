package command

import context.RedisContext
import util.RedisProtocolUtils.errorReply
import util.RedisProtocolUtils.okReply

class ReplConfCommand : Command {
    override fun execute(args: List<String>, context: RedisContext): String {
        return when (args.getOrNull(1)?.lowercase()) {
            "listening-port" -> {
                okReply()
            }
            "capa" -> {
                okReply()
            }
            else -> errorReply("ERR Unknown REPLCONF subcommand or wrong number of arguments")
        }
    }
}