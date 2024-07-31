package util

import java.io.BufferedReader

object CommandReader {
    fun readCommand(reader: BufferedReader): List<String> {
        val line = reader.readLine() ?: return emptyList()
        return if (line.startsWith("*")) {
            val count = line.substring(1).toInt()
            (1..count).mapNotNull {
                reader.readLine()  // $ line
                reader.readLine()  // actual argument
            }
        } else {
            listOf(line)
        }
    }
}