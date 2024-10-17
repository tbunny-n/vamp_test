package snuz.vamp

import net.minecraft.entity.ai.goal.ActiveTargetGoal
import net.minecraft.entity.passive.IronGolemEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity

class GolemVampGoal(
    golem: IronGolemEntity,
    targetClass: Class<PlayerEntity>,
    checkVisibility: Boolean,
) : ActiveTargetGoal<PlayerEntity>(golem, targetClass, checkVisibility) {

    override fun findClosestTarget() {
        // Handle vampire targetting
        if (this.targetClass == PlayerEntity::class.java || this.targetClass == ServerPlayerEntity::class.java) {
            this.targetEntity =
                this.mob.world.getClosestPlayer(this.targetPredicate, this.mob, this.mob.x, this.mob.eyeY, this.mob.z)
            // If they're not a vampire, clear 'em
            if (this.targetEntity != null && !isVamp(this.targetEntity as PlayerEntity))
                this.targetEntity = null
            // If it's day time, don't suspect the vampires
            // TODO: Test if this messes with the forgiveness system
            if (this.targetEntity != null && this.mob.world.isDay)
                this.targetEntity = null
        } else {
            super.findClosestTarget()
        }
    }

    private fun isVamp(plr: PlayerEntity): Boolean {
        val state = StateSaverAndLoader.getPlayerState(plr) ?: return false
        return (state.hasSanguinare || state.isVampire)
    }
}