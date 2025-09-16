package org.cludivers.prisonkb

import com.sk89q.worldedit.function.pattern.Pattern
import com.sk89q.worldedit.function.pattern.RandomPattern
import com.sk89q.worldedit.world.block.BlockTypes
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.cludivers.prisonkb.Prisonkb.Companion.plugin
import java.util.HashMap

object MinesListener: Listener {

    val mines = HashMap<String, Mine>()

    private val blockPrices = HashMap<Material, Int>()

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)

        loadMinesFromConfig()

        loadBlockPrices()
        generateMines()
        startMineRegeneratorTask()

        plugin.logger.info("Mines: ${mines}")
    }

    private fun getMineLocations(i: Int): Pair<Block, Block> {
        val spacing = 300
        val world = Bukkit.getWorld("world")
        return Pair(
            Location(world, 50.0 + spacing * i, 50.0, 50.0).block,
            Location(world, 150.0 + spacing * i, 150.0, 150.0).block,)
    }

    private fun getMineLetter(i: Int): String {
        return Ranks.getRank(i).name
    }

    private fun generateMines() {

        val patterns: List<Pattern> = listOf(
            {
                val pattern = RandomPattern()
                pattern.add(BlockTypes.STONE!!.defaultState, 1.0)
                pattern
            }(),
            {
                val pattern = RandomPattern()
                pattern.add(BlockTypes.STONE!!.defaultState, 3.0)
                pattern.add(BlockTypes.COAL_ORE!!.defaultState, 1.0)
                pattern
            }(),
            {
                val pattern = RandomPattern()
                pattern.add(BlockTypes.COAL_ORE!!.defaultState, 1.0)
                pattern
            }(),
            {
                val pattern = RandomPattern()
                pattern.add(BlockTypes.COAL_ORE!!.defaultState, 3.0)
                pattern.add(BlockTypes.COPPER_ORE!!.defaultState, 1.0)
                pattern
            }(),
            BlockTypes.COPPER_ORE!!.defaultState,
            {
                val pattern = RandomPattern()
                pattern.add(BlockTypes.COPPER_ORE!!.defaultState, 3.0)
                pattern.add(BlockTypes.IRON_ORE!!.defaultState, 1.0)
                pattern
            }(),
            BlockTypes.IRON_ORE!!.defaultState,
            {
                val pattern = RandomPattern()
                pattern.add(BlockTypes.IRON_ORE!!.defaultState, 3.0)
                pattern.add(BlockTypes.GOLD_ORE!!.defaultState, 1.0)
                pattern
            }(),
            {
                val pattern = RandomPattern()
                pattern.add(BlockTypes.GOLD_ORE!!.defaultState, 1.0)
                pattern
            }(),
            {
                val pattern = RandomPattern()
                pattern.add(BlockTypes.GOLD_ORE!!.defaultState, 3.0)
                pattern.add(BlockTypes.LAPIS_ORE!!.defaultState, 1.0)
                pattern
            }(),
            {
                val pattern = RandomPattern()
                pattern.add(BlockTypes.LAPIS_ORE!!.defaultState, 1.0)
                pattern
            }(),
            {
                val pattern = RandomPattern()
                pattern.add(BlockTypes.LAPIS_ORE!!.defaultState, 3.0)
                pattern.add(BlockTypes.REDSTONE_ORE!!.defaultState, 1.0)
                pattern
            }(),
            {
                val pattern = RandomPattern()
                pattern.add(BlockTypes.REDSTONE_ORE!!.defaultState, 1.0)
                pattern
            }(),
            {
                val pattern = RandomPattern()
                pattern.add(BlockTypes.REDSTONE_ORE!!.defaultState, 3.0)
                pattern.add(BlockTypes.DIAMOND_ORE!!.defaultState, 1.0)
                pattern
            }(),
            {
                val pattern = RandomPattern()
                pattern.add(BlockTypes.DIAMOND_ORE!!.defaultState, 1.0)
                pattern
            }(),
            {
                val pattern = RandomPattern()
                pattern.add(BlockTypes.DIAMOND_ORE!!.defaultState, 3.0)
                pattern.add(BlockTypes.EMERALD_ORE!!.defaultState, 1.0)
                pattern
            }(),
            {
                val pattern = RandomPattern()
                pattern.add(BlockTypes.EMERALD_ORE!!.defaultState, 1.0)
                pattern
            }(),
            BlockTypes.IRON_BLOCK!!.defaultState as Pattern,
            BlockTypes.GOLD_BLOCK!!.defaultState,
            BlockTypes.REDSTONE_BLOCK!!.defaultState,
            BlockTypes.DIAMOND_BLOCK!!.defaultState,
            BlockTypes.EMERALD_BLOCK!!.defaultState,
            {
                val pattern = RandomPattern()
                pattern.add(BlockTypes.EMERALD_BLOCK!!.defaultState, 4.0)
                pattern.add(BlockTypes.ANCIENT_DEBRIS!!.defaultState, 1.0)
                pattern
            }(),
            {
                val pattern = RandomPattern()
                pattern.add(BlockTypes.EMERALD_BLOCK!!.defaultState, 55.0)
                pattern.add(BlockTypes.ANCIENT_DEBRIS!!.defaultState, 45.0)
                pattern
            }(),
            BlockTypes.ANCIENT_DEBRIS!!.defaultState,
            BlockTypes.NETHERITE_BLOCK!!.defaultState,
        )

        (0..25).forEach {
            val loc = getMineLocations(it)
            val mineName = getMineLetter(it)
            mines[mineName] = Mine(mineName, loc.first, loc.second, 30 + it * 2,
                patterns[it])
        }
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

        mines.forEach {
            object : BukkitRunnable() {
                override fun run() {
                    Bukkit.broadcast(Component.text("Regenerating Mine ${it.value.name}"))
                    it.value.refill()
                }
            }.runTaskTimer(plugin,  it.value.regenMinutes * 60 * 20L, it.value.regenMinutes * 60 * 20L)
        }
    }

    private fun getMineAt(block: Block): Mine? {
        for (mine in mines.values) if (mine.contains(block)) return mine
        return null
    }

    fun getBlockPrice(material: Material): Int {
        return blockPrices[material] ?: 0
    }

    @EventHandler
    fun onBlockPlace(e: BlockPlaceEvent) {
        val block = e.block
        if (e.player.gameMode != GameMode.CREATIVE) {
            e.isCancelled = true
        }
    }


    @EventHandler
    fun onBlockBreak(e: BlockBreakEvent) {
        val player = e.player
        val block = e.block
        // Only process if inside a configured mine
        val mine = getMineAt(block)
        if (mine == null) {
            if (e.player.gameMode != GameMode.CREATIVE) {
                e.isCancelled = true
            }
        }
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
        val xpGain = (totalXp * pData.getPrestigeMultiplier() * pData.getXpBonusMultiplier()).coerceAtLeast(0.0)
        val currencyGain = (totalCurrency * pData.getPrestigeMultiplier() * pData.getCurrencyBonusMultiplier()).coerceAtLeast(0.0).toInt()

        // give items
        val inv = player.inventory
        droppedItems.forEach { inv.addItem(it) }
        player.giveExp(0) // avoid in-game XP; rank XP tracked separately

        // update player data
        pData.totalXp += xpGain
        pData.currency += currencyGain
        // handle rank ups

        // Save
        PlayerCache.savePlayerData(pData)

        // Feedback
        val message = Component.text("+ ${xpGain}XP" +
           if (droppedItems.isNotEmpty()) " ${droppedItems.joinToString { it.type.name + "x" + it.amount }}" else " Dropped nothing"
        ).color(NamedTextColor.GOLD)
        player.notifyBrokenBlock(message, bestEntrySound)
    }

    fun selectAdjacentMineBlocks(start: Block, count: Int): List<Block> {
        val mine = getMineAt(start) ?: return emptyList()
        if (count <= 0) return emptyList()

        val result = mutableListOf<Block>()
        val visited = mutableSetOf<Block>()
        val queue = ArrayDeque<Block>()

        queue.add(start)
        visited.add(start)

        while (queue.isNotEmpty() && result.size < count) {
            val current = queue.removeFirst()
            result.add(current)

            // Add neighbors (6 directions in 3D grid)
            val neighbors = listOf(
                current.getRelative(1, 0, 0),
                current.getRelative(-1, 0, 0),
                current.getRelative(0, 1, 0),
                current.getRelative(0, -1, 0),
                current.getRelative(0, 0, 1),
                current.getRelative(0, 0, -1),
            )

            for (neighbor in neighbors) {
                if (neighbor !in visited && getMineAt(neighbor) == mine) {
                    visited.add(neighbor)
                    queue.add(neighbor)
                }
            }
        }

        return result
    }

    @EventHandler
    fun onPickaxePowerUp(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        if (event.item == null || event.item!!.type != Material.WOODEN_PICKAXE) {
            return
        }

        val blocks = selectAdjacentMineBlocks(block, 10)
        for (block in blocks) {
            // Fire a BlockBreakEvent for each block
            val event = BlockBreakEvent(block, event.player)
            Bukkit.getPluginManager().callEvent(event)

            if (!event.isCancelled) {
                // If not cancelled, break the block naturally
                block.breakNaturally(event.player.inventory.itemInMainHand)
            }
        }

    }
}