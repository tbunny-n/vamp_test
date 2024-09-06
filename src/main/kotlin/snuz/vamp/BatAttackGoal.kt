package snuz.vamp

import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.passive.BatEntity
import org.slf4j.LoggerFactory
import java.util.*

class BatAttackGoal(private val bat: BatEntity) : Goal() {
    private val logger = LoggerFactory.getLogger("vamp")

    // Bat Attack & Movement Settings
    private val attackIntervalInTicks = 20 // Time between attacks
    private val attackRange: Double = 2.0
    private val chaseRange: Double = 64.0
    private val damageAmount: Float = 1.0f

    companion object {
        private const val BAT_CHASE_SPEED: Double = 0.4

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
        }
    }
}