package org.cludivers.prisonkb

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.cludivers.prisonkb.PlayerCache.loadOrGetPlayer


object PlayerListener: Listener {

    init {
        Prisonkb.plugin.server.pluginManager.registerEvents(this, Prisonkb.plugin)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        event.player.showRank()
    }
}