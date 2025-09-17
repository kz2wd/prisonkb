package org.cludivers.prisonkb

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.cludivers.prisonkb.FormatUtils.format
import org.cludivers.prisonkb.MinesListener.getBlockPrice
import org.cludivers.prisonkb.PlayerCache.loadOrGetPlayer
import org.cludivers.prisonkb.PlayerCache.savePlayerData
import org.cludivers.prisonkb.Prisonkb.Companion.plugin


object PrisonCommands {

    init {
        rankupCommand()
        sellallCommand()
        statsCommand()
        spawnCommand()

        val mineCmd = mineCommand()
        plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { commands ->
            commands.registrar().register(mineCmd)
        }
    }

    private fun rankupCommand() {
        plugin.registerCommand("rankup") {
                source, args ->
            val sender: CommandSender = source.sender
            val executor: Entity? = source.executor
            if (executor !is Player) {
                sender.sendMessage("Only players can rank up.")
                return@registerCommand
            }
            val pData = loadOrGetPlayer(executor.uniqueId)
            if (pData.currency >= PrisonPlayer.moneyForRank(pData.rank + 1)) {
                pData.rank += 1
                executor.sendMessage(Component.text("§aRanked up to ").append(PrisonPlayer.rankLetter(pData.rank)))
                if (sender != executor) {
                    sender.sendMessage(Component.text("§aRanked up [${executor.name}] to ").append(PrisonPlayer.rankLetter(pData.rank)))
                }
            } else {
                sender.sendMessage("§cNot enough currency to rank up.")
                return@registerCommand
            }
            savePlayerData(pData)
            executor.showRank()

        }
    }

    private fun sellallCommand() {
        plugin.registerCommand(
            "sellall"
        ) { source, args ->
            val sender = source.sender
            val executor = source.executor
            if (executor !is Player) {
                sender.sendMessage("Only players can use this command.")
                return@registerCommand
            }
            val player = executor
            var totalEarned = 0

            val inv = player.inventory
            for (slot in 0 until inv.size) {
                val item = inv.getItem(slot) ?: continue
                val price = getBlockPrice(item.type)
                if (price > 0.0) {
                    val amount = item.amount
                    totalEarned += (price * amount)
                    inv.setItem(slot, null) // remove items
                }
            }
            val currencyMultiplier = loadOrGetPlayer(executor.uniqueId).getCurrencyBonusMultiplier()
            totalEarned = (totalEarned * currencyMultiplier).toInt()
            if (totalEarned > 0) {
                player.addMoney(totalEarned)
                player.sendMessage("§aYou sold your items for §6$totalEarned §acurrency! (Multiplier: ${currencyMultiplier})")
                if (sender != executor) {
                    sender.sendMessage("[${executor.name}] gained $totalEarned coins.")
                }
            } else {
                sender.sendMessage("§cYou have no sellable items.")
            }
        }
    }

    private fun statsCommand() {
        plugin.registerCommand("stats") {
                source, args ->
            val sender = source.sender
            val executor = source.executor
            if (executor !is Player) {
                sender.sendMessage("Only players")
                return@registerCommand
            }
            val p = loadOrGetPlayer(executor.uniqueId)
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
        }
    }

    private fun mineCommand(): LiteralCommandNode<CommandSourceStack> {

        val cmd = Commands.literal("mine")
            .requires { ctx -> ctx.executor is Player}
            .then(
                Commands.argument("mine_name", StringArgumentType.word())
                .suggests { ctx, builder ->
                    MinesListener.mines
                        .filter { it.key.startsWith(builder.remaining, true) }
                        .forEach { builder.suggest(it.key) }
                    return@suggests builder.buildFuture()
                })
            .executes { ctx ->
                val sender = ctx.source.sender
                val executor = ctx.source.executor as Player

                val targetMine = StringArgumentType.getString(ctx, "mine_name")

                val mine = MinesListener.mines[targetMine]
                if (mine == null) {
                    sender.sendMessage(Component.text("Mine \"$targetMine\" does not exist.").color(NamedTextColor.RED))
                    return@executes com.mojang.brigadier.Command.SINGLE_SUCCESS
                }
                mine.teleportToEntrance(executor)
                executor.sendMessage(Component.text("Welcome to mine $targetMine.").color(NamedTextColor.GREEN))
                if (executor != sender) {
                    sender.sendMessage("Sent [${executor.name}] to mine ${targetMine}")
                }

                return@executes com.mojang.brigadier.Command.SINGLE_SUCCESS
            }.build()

        return cmd
    }




    private fun spawnCommand() {
        plugin.registerCommand("spawn") {
                source, args ->
            val sender = source.sender
            val executor = source.executor
            if (executor !is Player) {
                sender.sendMessage("Only players can use this command.")
                return@registerCommand
            }
            val spawn = Location(Bukkit.getWorld("world"), 36.0, 152.01, 101.0)
            spawn.direction = Vector(90.0, 0.0, 0.0)
            executor.teleport(spawn)
            executor.sendMessage("Welcome to spawn")
            if (executor != sender) {
                sender.sendMessage("Teleported [${executor}] to spawn")
            }
        }
    }

}