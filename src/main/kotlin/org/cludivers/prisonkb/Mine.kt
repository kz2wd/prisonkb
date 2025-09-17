package org.cludivers.prisonkb

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.function.pattern.Pattern
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.world.block.BlockTypes
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.cludivers.prisonkb.Prisonkb.Companion.plugin
import java.util.*
import java.util.logging.Level

class Mine(val name: String, val min: Block, val max: Block, val regenMinutes: Int, val pattern: Pattern = BlockTypes.STONE!!.defaultState) {
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

    private var region: CuboidRegion? = null

    fun region(): CuboidRegion {
        if (region == null) {
            region = CuboidRegion(BlockVector3.at(min.x, min.y, min.z), BlockVector3.at(max.x, max.y, max.z))
        }
        return region!!
    }


    fun size(): Int {
        val dx = (max.x - min.x).absoluteValue() + 1
        val dy = (max.y - min.y).absoluteValue() + 1
        val dz = (max.z - min.z).absoluteValue() + 1
        return dx * dy * dz
    }

    fun teleportToEntrance(player: Player) {
        val region = region()
        val center = region.center.add(0.0, region.height / 2.0 + 1.1, 0.0)
        player.teleport(Location(player.world, center.x(), center.y(), center.z()))
    }

    private fun emptyPlayers() {
        Bukkit.getOnlinePlayers().filter { region().contains(BlockVector3.at(it.x.toInt(), it.y.toInt(), it.z.toInt())) }
            .forEach { teleportToEntrance(it) }
    }

    fun refill(): Unit {
        emptyPlayers()
        val bukkitWorld = Bukkit.getWorld("world")
        if (bukkitWorld == null) {
            plugin.logger.log(Level.INFO, "World is null :/")
        }

        val world = BukkitAdapter.adapt(bukkitWorld)
        val session = WorldEdit.getInstance().newEditSession(world)
        session.setBlocks(region() as Region, pattern)
        session.close()
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
        }.runTaskLater(Prisonkb.plugin, regenMinutes.toLong() * 20L)
    }

    private fun Int.absoluteValue(): Int = if (this < 0) -this else this
}