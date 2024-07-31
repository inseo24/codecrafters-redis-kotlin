package command

import context.RedisContext
import model.ValueWithExpiry
import util.RedisProtocolUtils.errorReply
import util.RedisProtocolUtils.simpleStringReply
import java.time.Instant

class SetCommand : Command {
    override fun execute(args: List<String>, context: RedisContext): String {
        if (args.size < 3) {
            return errorReply("ERR wrong number of arguments for 'set' command")
        }
        val key = args[1]
        val value = args[2]
        var expiryTime: Instant? = null

        if (args.size > 3 && args[3].uppercase() == "PX") {
            if (args.size < 5) {
                return errorReply("ERR syntax error")
            }
            val milliseconds = args[4].toLongOrNull()
                ?: return errorReply("ERR value is not an integer or out of range")
            expiryTime = Instant.now().plusMillis(milliseconds)
        }

        context.store[key] = ValueWithExpiry(value, expiryTime)

        return simpleStringReply("OK")
    }
}