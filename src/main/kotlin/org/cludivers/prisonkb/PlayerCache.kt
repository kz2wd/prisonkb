package org.cludivers.prisonkb

import com.google.gson.GsonBuilder
import java.io.File
import java.util.*

object PlayerCache {

    private val playerDataDir by lazy { File(Prisonkb.plugin.dataFolder, "playerdata") }
    private val playerCache = HashMap<UUID, PrisonPlayer>()
    private val gson = GsonBuilder().setPrettyPrinting().create()

    init {
        if (!playerDataDir.exists()) playerDataDir.mkdirs()
    }

    private fun playerFile(uuid: UUID) = File(playerDataDir, "$uuid.json")

    fun saveAllPlayer() {
        playerCache.values.forEach { savePlayerData(it) }
    }

    fun loadOrGetPlayer(uuid: UUID): PrisonPlayer {
        val cached = playerCache[uuid]
        if (cached != null) return cached
        val f = playerFile(uuid)
        if (f.exists()) {
            try {
                val txt = f.readText()
                val p = gson.fromJson(txt, PrisonPlayer::class.java)
                playerCache[uuid] = p
                return p
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        val p = PrisonPlayer(uuid)
        playerCache[uuid] = p
        savePlayerData(p)
        return p
    }


    fun savePlayerData(p: PrisonPlayer) {
        try {
            val f = playerFile(p.uuid)
            f.parentFile?.mkdirs()
            f.writeText(gson.toJson(p))
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}