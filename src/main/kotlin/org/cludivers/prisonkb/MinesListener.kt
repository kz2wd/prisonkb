package org.cludivers.prisonkb

import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import org.cludivers.prisonkb.FormatUtils.format
import org.cludivers.prisonkb.Prisonkb.Companion.plugin
import java.util.HashMap

object MinesListener: Listener {

    private val mines = HashMap<String, Mine>()

    private val blockPrices = HashMap<Material, Int>()

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)

        loadMinesFromConfig()
        startMineRegeneratorTask()
        loadBlockPrices()
    }

    private fun loadBlockPrices() {
        blockPrices.clear()
        val section = plugin.config.getConfigurationSection("block-prices") ?: return
        for (key in section.getKeys(false)) {
            val material = Material.matchMaterial(key)
            if (material != null) {
                blockPrices[material] = section.getInt(key, 0)
            } else {
                plugin.logger.warning("Unknown material in block-prices: $key")
            }
        }
        plugin.logger.info("Loaded ${blockPrices.size} block prices from config.")
    }

    private fun loadMinesFromConfig() {
        val cfg = plugin.config
        if (!cfg.isSet("mines")) {
        // create example mine
            cfg.set("mines.example.world", "world")
            cfg.set("mines.example.min", listOf(0, 0, 0))
            cfg.set("mines.example.max", listOf(10, 128, 10))
            cfg.set("mines.example.regen-seconds", 30)
            plugin.saveConfig()
        }
        val minesSection = cfg.getConfigurationSection("mines") ?: return
        for (key in minesSection.getKeys(false)) {
            val s = minesSection.getConfigurationSection(key) ?: continue
            val worldName = s.getString("world") ?: continue
            val world = plugin.server.getWorld(worldName) ?: continue
            val minList = s.getIntegerList("min")
            val maxList = s.getIntegerList("max")
            val regenSeconds = s.getInt("regen-seconds", 30)
            if (minList.size < 3 || maxList.size < 3) continue
            val min = Location(world, minList[0].toDouble(), minList[1].toDouble(), minList[2].toDouble())
            val max = Location(world, maxList[0].toDouble(), maxList[1].toDouble(), maxList[2].toDouble())
            val mine = Mine(key, min.block, max.block, regenSeconds)
            mines[key] = mine
            plugin.logger.info("Loaded mine $key at ${world.name}")
        }
    }

    private fun startMineRegeneratorTask() {
        // We rely on each mine scheduling its own regeneration on break; we just keep the plugin alive.
    }

    private fun getMineAt(block: Block): Mine? {
        for (mine in mines.values) if (mine.contains(block)) return mine
        return null
    }

    fun getBlockPrice(material: Material): Int {
        return blockPrices[material] ?: 0
    }


    @EventHandler
    fun onBlockBreak(e: BlockBreakEvent) {
        val player = e.player
        val block = e.block
        // Only process if inside a configured mine
        val mine = getMineAt(block) ?: return
        // Cancel default drops to control custom loots
        e.isDropItems = false

        val entries = LootEntry.lootTables[block.type] ?: LootEntry.lootTables[Material.STONE]
        var totalXp = 0.0
        var totalCurrency = 0.0
        val droppedItems = mutableListOf<ItemStack>()
        // apply loot table
        var bestEntrySound: Sound? = null
        if (entries != null) {
            for (entry in entries) {
                if (Math.random() <= entry.chance) {
                    val amt = when (entry.amount) {
                        is Int -> entry.amount
//                        is kotlin.IntRange -> (entry.amountSpec as IntRange).random()
                        else -> 1
                    }
                    val mat = entry.material ?: entry.fallbackMaterial ?: Material.COBBLESTONE
                    droppedItems.add(ItemStack(mat, amt))
                    totalXp += entry.xp ?: 0.0
                    totalCurrency += entry.currency ?: 0.0
                    bestEntrySound = entry.sound
                }
            }
        }

        // apply prestige multiplier
        val pData = PlayerCache.loadOrGetPlayer(player.uniqueId)
        val prestigeMultiplier = 1.0 + pData.prestigePoints * 0.02 // each prestige point +2% gain
        val xpGain = (totalXp * prestigeMultiplier * pData.getXpBonusMultiplier()).coerceAtLeast(0.0)
        val currencyGain = (totalCurrency * prestigeMultiplier * pData.getCurrencyBonusMultiplier()).coerceAtLeast(0.0).toInt()

        // give items
        val inv = player.inventory
        droppedItems.forEach { inv.addItem(it) }
        player.giveExp(0) // avoid in-game XP; rank XP tracked separately

        // update player data
        pData.rankXp += xpGain
        pData.currency += currencyGain
        // handle rank ups
        while (pData.rank < PrisonPlayer.maxRankIndex() && pData.rankXp >= PrisonPlayer.xpForRank(pData.rank + 1)) {
            pData.rank += 1
            player.sendMessage(Component.text("§aRank up! You are now rank ").append(PrisonPlayer.rankLetter(pData.rank)))
        }

        // schedule block regeneration
        mine.scheduleRegen(block)

        // Save
        PlayerCache.savePlayerData(pData)

        // Feedback
        val message = Component.text("§6+${format(xpGain)} Rank XP +${currencyGain} coins. " +
           if (droppedItems.isNotEmpty()) "\"§ ${droppedItems.joinToString { it.type.name + "x" + it.amount }}\"" else "\"§eDropped nothing")
        player.notifyBrokenBlock(message, bestEntrySound)
    }
}