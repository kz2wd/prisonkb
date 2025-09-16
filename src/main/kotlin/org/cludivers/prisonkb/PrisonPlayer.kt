package org.cludivers.prisonkb

import net.kyori.adventure.text.Component
import java.lang.Math.clamp
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

data class PrisonPlayer(
    val uuid: UUID = UUID.randomUUID(),
    var rank: Int = 0, // 0==A, 25==Z
    var totalXp: Double = 0.0,
    var currency: Int = 0,
    var prestigePoints: Int = 0,
    var upgrades: MutableMap<String, Int> = HashMap()
) {
    companion object {
        // XP curve example: next rank requires previous * factor + base
        fun moneyForRank(rank: Int): Int {
// rank 0 -> next rank requires 50, rank 1->100... geometric
            if (rank <= 0) return 50
            return (50.0 * 1.6.pow((rank - 1).toDouble())).toInt()
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

    fun getMiningLevel(): Int {
        val rank = 1 + ln(totalXp / 50.0) / ln(1.6)
        return rank.roundToInt().coerceAtLeast(1)
    }

    fun getXpBonusMultiplier(): Double {
        val lvl = upgrades["xp_boost"] ?: 0
        return 1.0 + (getMiningLevel() + lvl ) * 0.15 // 15% per level
    }


    fun getCurrencyBonusMultiplier(): Double {
        val lvl = upgrades["coin_boost"] ?: 0
        return 1.0 + (getMiningLevel() + lvl) * 0.15
    }


    fun getRegenReductionFactor(): Double {
        val lvl = upgrades["regen_reduce"] ?: 0
        return 1.0 - min(0.5, lvl * 0.12) // up to 50% reduction
    }

    fun miningLevelProgress(): Float {
        return clamp((this.totalXp / moneyForRank(this.rank + 1)).toFloat(), 0.0f, 1.0f)
    }

    fun getPrestigeMultiplier(): Double {
        return 1.0 + this.prestigePoints * 0.02 // each prestige point +2% gain
    }
}