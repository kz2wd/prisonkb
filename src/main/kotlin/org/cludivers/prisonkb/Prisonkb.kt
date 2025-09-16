package org.cludivers.prisonkb

import com.google.gson.GsonBuilder
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.*

class Prisonkb : JavaPlugin() {

    companion object {
        lateinit var plugin: JavaPlugin
    }

    override fun onEnable() {
        plugin = this
        saveDefaultConfig()
        MinesListener
        PlayerCache
        PlayerListener
        val prisonCommands = PrisonCommands()
        getCommand("prison")?.setExecutor(prisonCommands)
        getCommand("prison")?.tabCompleter = prisonCommands
        logger.info("PrisonKB enabled")


    }

    override fun onDisable() {
        // flush cache
        PlayerCache.saveAllPlayer()
        logger.info("PrisonRanks disabled")
    }



}







