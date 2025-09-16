package org.cludivers.prisonkb

import org.bukkit.Material
import org.bukkit.Sound
import java.util.HashMap

data class LootEntry(
    val item: String? = null,
    val material: Material? = null,
    val fallbackMaterial: Material? = null,
    val chance: Double = 1.0,
    val amount: Any? = 1,
    val xp: Double? = 0.0,
    val currency: Double? = 0.0,
    val sound: Sound = Sound.BLOCK_AMETHYST_CLUSTER_PLACE
) {

    companion object {

        val lootTables = HashMap<Material, List<LootEntry>>()

         init {
             loadLootTables()
         }


        private fun loadLootTables() {
            // Minimal example: configure some loot entries for stone, coal_ore, iron_ore, diamond_ore
            // In production, move this into config.yml for easy editing.
            lootTables[Material.STONE] = listOf(
                LootEntry(item = null, material = Material.COBBLESTONE, chance = 1.0, amount = 1, xp = 1.0, currency = 0.1),
                LootEntry(item = null, material = Material.EMERALD, chance = 0.01, amount = 1, xp = 10.0, currency = 5.0)
            )
            lootTables[Material.COAL_ORE] = listOf(
                LootEntry(material = Material.COAL, chance = 1.0, amount = 2, xp = 2.0, currency = 0.2),
                LootEntry(material = Material.EMERALD, chance = 0.005, amount = 1, xp = 20.0, currency = 10.0)
            )
            lootTables[Material.IRON_ORE] = listOf(
                LootEntry(material = Material.IRON_ORE, chance = 1.0, amount = 1, xp = 4.0, currency = 0.5)
            )
            lootTables[Material.DIAMOND_ORE] = listOf(
                LootEntry(material = Material.DIAMOND, chance = 1.0, amount = 1, xp = 25.0, currency = 20.0)
            )
        }

    }



}

