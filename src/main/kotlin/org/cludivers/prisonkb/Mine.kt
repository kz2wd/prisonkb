package org.cludivers.prisonkb

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

class Mine(val name: String, val min: Block, val max: Block, val regenSeconds: Int) {
    private val regenerating = HashSet<Location>()


    fun contains(b: Block): Boolean {
        if (b.world != min.world) return false
        val x = b.x
        val y = b.y
        val z = b.z
        val minx = min.x.coerceAtMost(max.x)
        val miny = min.y.coerceAtMost(max.y)
        val minz = min.z.coerceAtMost(max.z)
        val maxx = min.x.coerceAtLeast(max.x)
        val maxy = min.y.coerceAtLeast(max.y)
        val maxz = min.z.coerceAtLeast(max.z)
        return x in minx..maxx && y in miny..maxy && z in minz..maxz
    }


    fun size(): Int {
        val dx = (max.x - min.x).absoluteValue() + 1
        val dy = (max.y - min.y).absoluteValue() + 1
        val dz = (max.z - min.z).absoluteValue() + 1
        return dx * dy * dz
    }


    fun scheduleRegen(block: Block) {
        val loc = block.location
        if (regenerating.contains(loc)) return
        val originalType = block.type
        val originalData = null // placeholder for block data
        // remove block
        block.type = Material.AIR
        regenerating.add(loc)
        object : BukkitRunnable() {
            override fun run() {
                try {
                    if (loc.chunk.isLoaded.not()) loc.chunk.load(true)
                    val b = loc.block
                    b.type = originalType
                } finally {
                    regenerating.remove(loc)
                    cancel()
                }
            }
        }.runTaskLater(Prisonkb.plugin, regenSeconds.toLong() * 20L)
    }

    private fun Int.absoluteValue(): Int = if (this < 0) -this else this
}