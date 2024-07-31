package command

import context.RedisContext
import util.RedisProtocolUtils.bulkStringReply
import util.RedisProtocolUtils.errorReply
import util.RedisProtocolUtils.nullBulkStringReply
import java.time.Instant

class GetCommand : Command {
    override fun execute(args: List<String>, context: RedisContext): String {
        if (args.size != 2) {
            return errorReply("ERR wrong number of arguments for 'get' command")
        }
        val key = args[1]
        val valueWithExpiry = context.store[key]

        return if (valueWithExpiry != null) {
            if (valueWithExpiry.expiryTime == null || Instant.now().isBefore(valueWithExpiry.expiryTime)) {
                bulkStringReply(valueWithExpiry.value)
            } else {
                context.store.remove(key)  // Remove expired key
                nullBulkStringReply()
            }
        } else {
            nullBulkStringReply()
        }
    }
}