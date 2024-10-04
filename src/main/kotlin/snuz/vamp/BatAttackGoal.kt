package snuz.vamp

import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.passive.BatEntity
import net.minecraft.text.Text
import java.util.*
import kotlin.random.Random

class BatAttackGoal(private val bat: BatEntity) : Goal() {
    private val logger = Vamp.LOGGER

    // Bat Attack & Movement Settings
    private val attackIntervalInTicks = 20 // Time between attacks
    private val attackRange: Double = 2.0
    private val chaseRange: Double = 64.0
    private val damageAmount: Float = 1.0f

    companion object {
        private const val BAT_CHASE_SPEED: Double = 0.4
        private const val SANGUINE_CHANCE_ON_HIT: Int = 3 // Percent chance to inflict sanguinare

        @JvmStatic
        fun getChaseSpeed(): Double {
            return BAT_CHASE_SPEED
        }
    }

    // Local state
    private var ticksSinceLastAttack = 0

    init {
        controls = EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP)
    }

    override fun canStart(): Boolean {
        return bat.target != null
    }

    override fun start() {
        logger.info("Starting! I'm going to chase you!")
        ticksSinceLastAttack = 0
    }

    override fun shouldContinue(): Boolean {
        return bat.target?.isAlive ?: false
    }

    override fun tick() {
        // If targetting, chase and attack. Else return
        val targetEntity = bat.target ?: return
        if (!targetEntity.isAlive || bat.squaredDistanceTo(targetEntity) > chaseRange) {
            bat.target = null // Setting the target to null makes the bat stop chasing
            logger.info("Bat out of range or won!")
            return
        }

        ticksSinceLastAttack++

        // Attack if within range and within interval
        if (bat.squaredDistanceTo(targetEntity) <= attackRange && ticksSinceLastAttack >= attackIntervalInTicks) {
            targetEntity.damage(VampDamageTypes.of(bat.world, VampDamageTypes.BAT_DAMAGE_TYPE), damageAmount)
            ticksSinceLastAttack = 0

            // * Chance to give target sanguinare vampirism
            // Check if entity is player
            if (!targetEntity.isPlayer) {
                return
            }
            val randomNumber = Random.nextInt(100)
            if (randomNumber <= SANGUINE_CHANCE_ON_HIT) {
                // Inflict sanguinare
                val playerState = StateSaverAndLoader.getPlayerState(targetEntity) ?: return
                if (playerState.isVampire || playerState.hasSanguinare) {
                    return
                }

                playerState.hasSanguinare = true
                playerState.sanguinareProgress = 0.1f
                targetEntity.sendMessage(Text.literal("you're... infected..."))
            }
        }
    }
}