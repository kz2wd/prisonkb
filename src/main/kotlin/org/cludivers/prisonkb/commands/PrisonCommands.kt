package org.cludivers.prisonkb.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.cludivers.prisonkb.*
import org.cludivers.prisonkb.PlayerCache.loadOrGetPlayer
import org.cludivers.prisonkb.FormatUtils.format
import org.cludivers.prisonkb.MinesListener.getBlockPrice
import org.cludivers.prisonkb.PlayerCache.savePlayerData

class PrisonCommands: CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) return false
        when (args[0].lowercase()) {
            "stats" -> {
                if (sender !is Player) {
                    sender.sendMessage("Only players"); return true
                }
                val p = loadOrGetPlayer(sender.uniqueId)
                sender.sendMessage("§6--- Prison Stats ---")
                sender.sendMessage(Component.text("Rank: ").append(PrisonPlayer.rankLetter(p.rank)))
                sender.sendMessage(Component.text("Mining level: ${p.getMiningLevel()} (Total xp: ${format(p.totalXp)}/${
                    PrisonPlayer.moneyForRank(
                        p.getMiningLevel() + 1
                    )
                })"))
                sender.sendMessage("Coins: ${p.currency}, next rank: ${PrisonPlayer.moneyForRank(p.rank + 1)}")
                sender.sendMessage("Prestige points: ${p.prestigePoints}")
                sender.sendMessage("Prestige bonus: ${p.getPrestigeMultiplier()}, XP bonus ${p.getXpBonusMultiplier()}, Coin bonus ${
                    p.getCurrencyBonusMultiplier()
                }")
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
                    p.totalXp = 0.0
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
                    if (price > 0.0) {
                        val amount = item.amount
                        totalEarned += (price * amount).toInt()
                        inv.setItem(slot, null) // remove items
                    }
                }
                val currencyMultiplier = loadOrGetPlayer(sender.uniqueId).getCurrencyBonusMultiplier()
                totalEarned = (totalEarned * currencyMultiplier).toInt()
                if (totalEarned > 0) {
                    player.addMoney(totalEarned)
                    player.sendMessage("§aYou sold your items for §6$totalEarned §acurrency! (Multiplier: ${currencyMultiplier})")
                } else {
                    player.sendMessage("§cYou have no sellable items.")
                }
            }

            "rankup" -> {
                if (sender !is Player) {
                    sender.sendMessage("Only players can use this command.")
                    return true
                }
                val pData = loadOrGetPlayer(sender.uniqueId)
                while (pData.rank < PrisonPlayer.maxRankIndex() && pData.currency >= PrisonPlayer.moneyForRank(pData.rank + 1)) {
                    pData.rank += 1
                    pData.currency -= PrisonPlayer.moneyForRank(pData.rank + 1)
                    sender.sendMessage(Component.text("§aRank up! You are now rank ").append(
                        PrisonPlayer.rankLetter(
                            pData.rank
                        )
                    ))
                }
                savePlayerData(pData)
                sender.showRank()

            }
            "mine" -> {
                if (sender !is Player) {
                    sender.sendMessage("Only players can use this command.")
                    return true
                }

                if (args.size < 2) {
                    sender.sendMessage("Specify a mine")
                    return true
                }
                val targetMine = args[1]

                val mine = MinesListener.mines[targetMine]
                if (mine == null) {
                    sender.sendMessage(Component.text("Mine \"$targetMine\" does not exist.").color(NamedTextColor.RED))
                    return true
                }
                mine.teleportToEntrance(sender)
                sender.sendMessage(Component.text("Welcome to mine $targetMine.").color(NamedTextColor.GREEN))
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
            1 -> mutableListOf("stats", "prestige", "upgrade", "sellall", "mine", "rankup")
            2 -> {
                if (args[0] == "mine") {
                    MinesListener.mines.map { it.key }.toMutableList()
                }
                else
                    mutableListOf()
            }
            else -> mutableListOf()
        }
    }
}