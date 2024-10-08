package snuz.vamp

import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos

private const val DEFAULT_EFFECT_DURATION: Int = 300
private const val NIGHT_VISION_DURATION: Int = 800 // Can't be too low due to epilepsy trigger !!
private const val ICKY_BLOOD_NAUSEA_DURATION: Int = 500
private const val SUNLIGHT_FIRE_DURATION: Float = 4f

class PlayerData {
    var hasSanguinare: Boolean = true // Sanguinare is what turns players into vamps
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

    fun ickyBlood(plr: ServerPlayerEntity) {
        plr.addStatusEffect(
            StatusEffectInstance(
                StatusEffects.NAUSEA,
                ICKY_BLOOD_NAUSEA_DURATION,
                0,
                true,
                true,
                true,
            )
        )
        plr.addStatusEffect(
            StatusEffectInstance(
                StatusEffects.HUNGER,
                ICKY_BLOOD_NAUSEA_DURATION,
                2,
                true,
                true,
                true,
            )
        )
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

    fun applyNightTimeEffects(plr: ServerPlayerEntity) {
        if (!plr.isAlive) return
        if (sanguinareProgress > 0.1) {
            plr.addStatusEffect(
                StatusEffectInstance(
                    StatusEffects.SPEED,
                    DEFAULT_EFFECT_DURATION,
                    getSpeed(),
                    true,
                    false,
                    false
                )
            )
            plr.addStatusEffect(
                StatusEffectInstance(
                    StatusEffects.STRENGTH,
                    DEFAULT_EFFECT_DURATION,
                    getStrength(),
                    true,
                    false,
                    false
                )
            )
            plr.addStatusEffect(
                StatusEffectInstance(
                    StatusEffects.RESISTANCE,
                    DEFAULT_EFFECT_DURATION,
                    getResistance(),
                    true,
                    false,
                    false
                )
            )
            plr.addStatusEffect(
                StatusEffectInstance(
                    StatusEffects.NIGHT_VISION,
                    NIGHT_VISION_DURATION,
                    1,
                    true,
                    false,
                    false
                )
            )

            if (vampireLevel > 23) {
                plr.addStatusEffect(
                    StatusEffectInstance(
                        StatusEffects.WATER_BREATHING,
                        DEFAULT_EFFECT_DURATION,
                        1,
                        true,
                        false,
                        false
                    )
                )
            }
        }
    }

    fun applyDayTimeEffects(plr: ServerPlayerEntity) {
        if (!plr.isAlive) return
        if (sanguinareProgress > 0.1) {
            if (isPlayerInSunlight(plr)) {
                plr.setOnFireFor(SUNLIGHT_FIRE_DURATION)
                plr.addStatusEffect(
                    StatusEffectInstance(
                        StatusEffects.WEAKNESS,
                        DEFAULT_EFFECT_DURATION,
                        0,
                        true,
                        false,
                        false
                    )
                )
            }
        }

    }
}