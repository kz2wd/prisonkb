package org.cludivers.prisonkb

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import org.cludivers.prisonkb.Prisonkb.Companion.plugin


object PlayerListener: Listener {

    init {
        Prisonkb.plugin.server.pluginManager.registerEvents(this, Prisonkb.plugin)
    }

    private fun getBeginPickaxe(): ItemStack {
        val item = ItemStack(Material.WOODEN_PICKAXE)
        val meta = item.itemMeta
        meta.isUnbreakable = true
        meta.addEnchant(Enchantment.EFFICIENCY, 15, true)
        item.setItemMeta(meta)
        return item
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        event.player.showRank()
        if (!event.player.inventory.contains(Material.WOODEN_PICKAXE)) {
            event.player.give(getBeginPickaxe())
        }
    }

    @EventHandler
    fun onPlayerHungry(event: FoodLevelChangeEvent) {
        event.entity.saturation = 20f
        event.entity.foodLevel = 20
    }

    private val JMP_PAD_MATERIAL = Material.SEA_LANTERN

    private fun dashPlayer(player: Player) {
        player.velocity = player.velocity.add(player.location.direction.multiply(0.1)).add(Vector(0, 10, 0))
        player.playSound(player, Sound.ENTITY_BREEZE_CHARGE, 1.0f, 1.0f)
        player.spawnParticle(Particle.POOF, player.location.add(-1.0, 0.0, -1.0), 20, 1.0, 1.0, 1.0)
    }

    @EventHandler
    fun onPlayerWalkOnJumpPad(event: PlayerMoveEvent) {
        val blockUnder =  event.player.location.add(0.0, -1.0, 0.0).block
        if (blockUnder.type == JMP_PAD_MATERIAL) {
            dashPlayer(event.player)
            object: BukkitRunnable() {
                override fun run() {
                    dashPlayer(event.player)
                }
            }.runTaskLater(plugin, 15L)
        }
    }

    @EventHandler
    fun onPlayerBreakJumpPad(event: BlockBreakEvent) {
        if (event.player.gameMode == GameMode.CREATIVE) {
            return
        }
        if (event.block.type == JMP_PAD_MATERIAL) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onFallDamage(event: EntityDamageEvent) {
        if (event.cause == EntityDamageEvent.DamageCause.FALL) {
            event.isCancelled = true
        }
    }
}