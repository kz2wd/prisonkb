package org.cludivers.prisonkb

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.cludivers.prisonkb.PlayerCache.loadOrGetPlayer
import org.cludivers.prisonkb.FormatUtils.format
import org.cludivers.prisonkb.MinesListener.getBlockPrice
import java.util.logging.Level

class PrisonCommands: CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) return false
        Prisonkb.plugin.logger.log(Level.INFO, "$args")
        when (args[0].lowercase()) {
            "stats" -> {
                if (sender !is Player) {
                    sender.sendMessage("Only players"); return true
                }
                val p = loadOrGetPlayer(sender.uniqueId)
                sender.sendMessage("§6--- Prison Stats ---")
                sender.sendMessage("Rank: ${PrisonPlayer.rankLetter(p.rank)} (${p.rank})")
                sender.sendMessage("XP: ${format(p.rankXp)} / ${format(PrisonPlayer.xpForRank(p.rank + 1))}")
                sender.sendMessage("Coins: ${p.currency}")
                sender.sendMessage("Prestige points: ${p.prestigePoints}")
                return true
            }

            "prestige" -> {
                if (sender !is Player) {
                    sender.sendMessage("Only players"); return true
                }
                val p = loadOrGetPlayer(sender.uniqueId)
                val maxRank = PrisonPlayer.maxRankIndex()
                if (p.rank < maxRank) {
                    sender.sendMessage("§cYou must reach rank ${PrisonPlayer.rankLetter(maxRank)} to prestige.")
                    return true
                }
                if (args.isNotEmpty() && args[0].lowercase() == "confirm") {
                // calculate prestige reward based on number of times or XP
                    val reward = 1 + p.prestigePoints / 10 // simple: 1 + floor(prev/10)
                    p.prestigePoints += reward
                    p.rank = 0
                    p.rankXp = 0.0
                    p.currency = 0
                    PlayerCache.savePlayerData(p)
                    sender.sendMessage("§bYou prestiged! +$reward prestige points. You now have ${p.prestigePoints} points.")
                } else {
                    sender.sendMessage("§eType /prisonprestige confirm to prestige. This will reset your rank and currency but grant prestige points.")
                }
                return true
            }

            "upgrade" -> {
                if (sender !is Player) {
                    sender.sendMessage("Only players"); return true
                }
                val p = loadOrGetPlayer(sender.uniqueId)
                if (args.isEmpty()) {
                    sender.sendMessage("§6--- Upgrades ---")
                    // show available upgrades
                    PrisonPlayer.knownUpgrades.forEach { (id, up) ->
                        val owned = p.upgrades[id] ?: 0
                        sender.sendMessage("$id) ${up.name} level $owned/${up.maxLevel} - cost: ${up.baseCost * (owned + 1)}")
                    }
                    return true
                }
                if (args[0].lowercase() == "buy" && args.size >= 2) {
                    val id = args[1]
                    val up = PrisonPlayer.knownUpgrades[id]
                    if (up == null) {
                        sender.sendMessage("Unknown upgrade"); return true
                    }
                    val owned = p.upgrades[id] ?: 0
                    if (owned >= up.maxLevel) {
                        sender.sendMessage("Max level reached"); return true
                    }
                    val cost = up.baseCost * (owned + 1)
                    if (p.currency < cost) {
                        sender.sendMessage("Not enough coins: need $cost"); return true
                    }
                    p.currency -= cost
                }
            }

            "sellall" -> {
                if (sender !is Player) {
                    sender.sendMessage("Only players can use this command.")
                    return true
                }

                val player = sender
                var totalEarned = 0

                val inv = player.inventory
                for (slot in 0 until inv.size) {
                    val item = inv.getItem(slot) ?: continue
                    val price = getBlockPrice(item.type)
                    Prisonkb.plugin.logger.log(Level.INFO, "$price for ${item.amount} ${item.type}")
                    if (price > 0.0) {
                        val amount = item.amount
                        totalEarned += (price * amount).toInt()
                        inv.setItem(slot, null) // remove items
                    }
                }

                if (totalEarned > 0) {
                    player.addMoney(totalEarned)
                    player.sendMessage("§aYou sold your items for §6$totalEarned §acurrency!")
                } else {
                    player.sendMessage("§cYou have no sellable items.")
                }
            }
        }
        return true
    }

    override fun onTabComplete(
        p0: CommandSender,
        p1: Command,
        p2: String,
        args: Array<out String>
    ): MutableList<String>? {
        return when (args.size) {
            1 -> mutableListOf("stats", "prestige", "upgrade", "sellall")
            else -> mutableListOf()
        }
    }
}