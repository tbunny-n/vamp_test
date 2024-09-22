package snuz.vamp

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos

class PlayerData {
    var hasSanguinare: Boolean = true // Sanguine is what turns players into vamps
    var sanguinareProgress: Float = 5.0f
    var isVampire: Boolean = true
    var vampireLevel: Int = 50
    var lastFeed: Long = 0L

    // ** Calculate Stats ** ---------

    fun getStrength(): Int {
        return when {
            this.vampireLevel > 40 -> 3
            this.sanguinareProgress > 3.0 -> 1
            else -> 0
        }
    }

    fun getSpeed(): Int {
        return when {
            this.vampireLevel > 40 -> 2
            this.sanguinareProgress > 2.0 -> 1
            else -> 0
        }
    }

    fun getResistance(): Int {
        return when {
            this.vampireLevel > 40 -> 2
            this.vampireLevel > 20 -> 1
            else -> 0
        }
    }

    fun getBloodSuckDamage(): Float {
        return when {
            this.vampireLevel > 40 -> 6.0f
            else -> 1.0f
        }
    }

    /**
     * How much life to give to the player per drain
     */
    fun getBloodSuckSteal(): Float {
        return when {
            this.sanguinareProgress > 10.0 -> 0.7f
            this.vampireLevel > 40 -> 0.4f
            else -> 0.1f
        }
    }

    /**
     * How much saturation to give to the vampire
     */
    fun getBloodSuckSaturation(): Float {
        return when {
            this.sanguinareProgress > 10.0 -> 0.4f
            this.vampireLevel > 40 -> 0.3f
            else -> 0.2f
        }
    }

    /**
     * How much food value to give to the vampire
     */
    fun getBloodSuckFood(): Int {
        return when {
            this.sanguinareProgress > 10.0 -> 2
            else -> 1
        }
    }

    private fun getSanguinareIncrease(): Float {
        return when {
            this.sanguinareProgress > 15.0 -> 0.1f
            this.sanguinareProgress > 5.0 -> 0.5f
            else -> 10.0f
        }
    }

    /**
     * Applied a scaled sanguinare increase value!
     */
    fun progressSanguinare() {
        val progressInc = getSanguinareIncrease()
        this.sanguinareProgress += progressInc
    }

    fun isPlayerInSunlight(plr: ServerPlayerEntity): Boolean {
        val world = plr.world
        val pos = BlockPos(plr.x.toInt(), plr.eyeY.toInt(), plr.z.toInt())

        val timeOfDay = world.timeOfDay % 24000
        if (timeOfDay !in 0..12000) {
            return false // Not daytime
        }

        val isExposedToSky = world.isSkyVisible(pos)
        val skylightLevel = world.getLightLevel(pos, 0)

        return isExposedToSky && skylightLevel > 0
    }
}