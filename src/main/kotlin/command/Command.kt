package command

import context.RedisContext

interface Command {
    fun execute(args: List<String>, context: RedisContext): String
}