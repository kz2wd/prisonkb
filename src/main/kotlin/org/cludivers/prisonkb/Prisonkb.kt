package org.cludivers.prisonkb

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.cludivers.prisonkb.MinesListener.getBlockPrice
import org.cludivers.prisonkb.PlayerCache.loadOrGetPlayer
import org.cludivers.prisonkb.PlayerCache.savePlayerData


class Prisonkb : JavaPlugin() {

    companion object {
        lateinit var plugin: JavaPlugin
    }

    override fun onEnable() {
        plugin = this
        saveDefaultConfig()
        PlayerListener  // declare before the mine listener
        MinesListener
        PlayerCache
        PrisonCommands

        logger.info("PrisonKB enabled")
    }

    override fun onDisable() {
        // flush cache
        PlayerCache.saveAllPlayer()
        logger.info("PrisonRanks disabled")
    }



}







