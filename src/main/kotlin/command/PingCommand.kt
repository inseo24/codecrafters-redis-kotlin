package command

import context.RedisContext
import util.RedisProtocolUtils.simpleStringReply

class PingCommand : Command {
    override fun execute(args: List<String>, context: RedisContext): String {
        return simpleStringReply("PONG")
    }
}