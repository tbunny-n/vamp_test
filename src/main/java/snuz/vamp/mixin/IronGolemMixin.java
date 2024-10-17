package snuz.vamp.mixin;

import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import snuz.vamp.GolemVampGoal;

@Mixin(IronGolemEntity.class)
public abstract class IronGolemMixin {

    @Inject(method = "initGoals", at = @At("HEAD"))
    public void initVampireTarget(CallbackInfo info) {
        IronGolemEntity golem = (IronGolemEntity) (Object) this;

        // Target vampires
        GoalSelector targetSelector = ((MobEntityAccessor) golem).getTargetSelector();
        targetSelector.add(3, new GolemVampGoal(golem, PlayerEntity.class, true));
    }
}
