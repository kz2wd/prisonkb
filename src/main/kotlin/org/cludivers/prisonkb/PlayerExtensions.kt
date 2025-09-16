package org.cludivers.prisonkb

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.bossbar.BossBar.Overlay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.cludivers.prisonkb.PlayerCache.loadOrGetPlayer
import org.cludivers.prisonkb.PrisonPlayer.Companion.rankLetter


fun Player.notifyBrokenBlock(message: Component, sound: Sound?) {
    this.sendActionBar(message)
    if (sound != null) this.playSound(this, sound, 0.4f, 1.0f)
    this.showRank()
}

fun Player.showRank() {
    val playerInfo = loadOrGetPlayer(this.uniqueId)
    val bars = this.activeBossBars()
    val name = Component.text("Mining Level ").color(NamedTextColor.DARK_GREEN)
        .append(Component.text("${playerInfo.getMiningLevel()}").decorate(TextDecoration.BOLD).color(NamedTextColor.GOLD))
        Component.text("Rank ").append(rankLetter(playerInfo.rank))
    bars.forEach {
        it.name(name)
        it.progress(playerInfo.miningLevelProgress())
        return
    }
    // If no bars, create one!
    this.showBossBar(BossBar.bossBar(name,
        playerInfo.miningLevelProgress(),
        BossBar.Color.PURPLE,
        Overlay.PROGRESS
    ))
}

fun Player.addMoney(amount: Int) {
    val playerInfo = loadOrGetPlayer(this.uniqueId)
    playerInfo.currency += amount
}
