package command

import context.RedisContext
import server.RedisServer.Companion.REPL_ID
import server.RedisServer.Companion.REPL_OFFSET
import util.RedisProtocolUtils.bulkStringReply

class InfoCommand : Command {
    override fun execute(args: List<String>, context: RedisContext): String {
        val info = buildString {
            appendLine("master_repl_offset:$REPL_OFFSET")
            appendLine("master_replid:$REPL_ID")
            appendLine("role:${if (context.isReplica) "slave" else "master"}")
        }
        return bulkStringReply(info)
    }
}