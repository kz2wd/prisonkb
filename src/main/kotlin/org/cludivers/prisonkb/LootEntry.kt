package org.cludivers.prisonkb

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.Sound
import org.cludivers.prisonkb.Prisonkb.Companion.plugin
import java.util.HashMap

data class LootEntry(
    val item: String? = null,
    val material: Material? = null,
    val fallbackMaterial: Material? = null,
    val chance: Double = 1.0,
    val amount: Any? = 1,
    val xp: Double? = 0.0,
    val currency: Double? = 0.0,
    val sound: Sound? = Sound.BLOCK_AMETHYST_CLUSTER_PLACE
) {

    companion object {

        val lootTables = HashMap<Material, List<LootEntry>>()

         init {
             loadLootTables()
         }

        private fun getSound(name: String): Sound? {
            return Registry.SOUNDS.get(NamespacedKey.minecraft(name.lowercase()))
        }

        private fun loadLootTables() {
            lootTables.clear()
            val section = plugin.config.getConfigurationSection("loot-tables") ?: return

            for (key in section.getKeys(false)) {
                val mat = Material.matchMaterial(key) ?: continue
                val entries = mutableListOf<LootEntry>()
                val list = section.getMapList(key)

                for (map in list) {
                    val materialName = map["material"] as? String ?: continue
                    val dropMat = Material.matchMaterial(materialName) ?: continue

                    val chance = (map["chance"] as? Number)?.toDouble() ?: 1.0
                    val amount = (map["amount"] as? Number)?.toInt() ?: 1
                    val xp = (map["xp"] as? Number)?.toDouble() ?: 0.0
                    val currency = (map["currency"] as? Number)?.toDouble() ?: 0.0
                    val soundName = map["sound"] as? String
                    val sound = if (soundName != null) getSound(soundName) else null

                    entries.add(
                        LootEntry(
                            material = dropMat,
                            chance = chance,
                            amount = amount,
                            xp = xp,
                            currency = currency,
                            sound = sound
                        )
                    )
                }

                lootTables[mat] = entries
            }

            plugin.logger.info("Loaded ${lootTables.size} loot tables from config")
        }

    }



}

