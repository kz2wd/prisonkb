package org.cludivers.prisonkb

import net.kyori.adventure.text.Component
import java.lang.Math.clamp
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.min

data class PrisonPlayer(
    val uuid: UUID = UUID.randomUUID(),
    var rank: Int = 0, // 0==A, 25==Z
    var rankXp: Double = 0.0,
    var currency: Int = 0,
    var prestigePoints: Int = 0,
    var upgrades: MutableMap<String, Int> = HashMap()
) {
    companion object {
        // XP curve example: next rank requires previous * factor + base
        fun xpForRank(rank: Int): Double {
// rank 0 -> next rank requires 50, rank 1->100... geometric
            if (rank <= 0) return 50.0
            return 50.0 * Math.pow(1.6, (rank - 1).toDouble())
        }


        fun rankLetter(rank: Int): Component {
            return Ranks.getRank(rank).getComponent()
        }

        fun maxRankIndex() = 25

        val knownUpgrades = mapOf(
            "xp_boost" to Upgrade("XP Boost", baseCost = 100, maxLevel = 5),
            "coin_boost" to Upgrade("Coin Boost", baseCost = 120, maxLevel = 5),
            "regen_reduce" to Upgrade("Mine Regen Reduce", baseCost = 200, maxLevel = 3)
        )
    }


    fun getXpBonusMultiplier(): Double {
        val lvl = upgrades["xp_boost"] ?: 0
        return 1.0 + lvl * 0.15 // 15% per level
    }


    fun getCurrencyBonusMultiplier(): Double {
        val lvl = upgrades["coin_boost"] ?: 0
        return 1.0 + lvl * 0.15
    }


    fun getRegenReductionFactor(): Double {
        val lvl = upgrades["regen_reduce"] ?: 0
        return 1.0 - min(0.5, lvl * 0.12) // up to 50% reduction
    }

    fun rankProgress(): Float {
        return clamp((this.rankXp / PrisonPlayer.xpForRank(this.rank + 1)).toFloat(), 0.0f, 1.0f)
    }
}