package config

data class Config(
    val port: Int,
    val isReplica: Boolean,
    val masterHost: String?,
    val masterPort: Int?
)

fun parseConfig(args: Array<String>): Config {
    val portIndex = args.indexOf("--port").takeIf { it != -1 } ?: args.indexOf("-p")
    val port = if (portIndex != -1 && portIndex + 1 < args.size) args[portIndex + 1].toInt() else 6379

    val replicaIndex = args.indexOf("--replicaof")
    val isReplica = replicaIndex != -1
    var masterHost: String? = null
    var masterPort: Int? = null

    if (isReplica && replicaIndex + 1 < args.size) {
        val replicaOfArgs = args[replicaIndex + 1].split(" ")
        if (replicaOfArgs.size == 2) {
            masterHost = replicaOfArgs[0]
            masterPort = replicaOfArgs[1].toIntOrNull()
        }
    }

    if (isReplica && (masterHost == null || masterPort == null)) {
        throw IllegalArgumentException("Invalid --replicaof arguments. Usage: --replicaof <master-host> <master-port>")
    }

    return Config(port, isReplica, masterHost, masterPort)
}