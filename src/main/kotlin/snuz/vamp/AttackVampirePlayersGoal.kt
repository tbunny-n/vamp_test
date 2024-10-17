package snuz.vamp

import net.minecraft.entity.ai.goal.ActiveTargetGoal
import net.minecraft.entity.passive.IronGolemEntity
import net.minecraft.entity.player.PlayerEntity

class AttackVampirePlayersGoal(golem: IronGolemEntity) :
    ActiveTargetGoal<PlayerEntity>(golem, PlayerEntity::class.java, true) {
    override fun canStart(): Boolean {
        val targetPlayer = this.targetEntity as PlayerEntity? ?: return false
        return isVampire(targetPlayer)
    }

    private fun isVampire(plr: PlayerEntity): Boolean {
        val playerState = StateSaverAndLoader.getPlayerState(plr) ?: return false
        return playerState.isVampire || playerState.hasSanguinare
    }
}