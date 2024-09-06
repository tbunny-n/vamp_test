package snuz.vamp.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import snuz.vamp.BatAttackGoal;


@Mixin(BatEntity.class)
public abstract class BatMixin extends MobEntity {
    @Unique
    private final static Boolean spawnBatsInLowLight = true; // TODO: Make this a toggleable via game rule.

    @Unique
    private static final float EYE_HEIGHT_COMPENSATION = 0.5f;

    protected BatMixin(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "isTodayAroundHalloween", at = @At("HEAD"), cancellable = true)
    private static void setSpawnBatsInLowLight(CallbackInfoReturnable<Boolean> cir) {
        if (spawnBatsInLowLight) {
            cir.setReturnValue(true);
        }
    }

    @Shadow
    public abstract boolean damage(DamageSource source, float amount);

    @Shadow
    public abstract void setRoosting(boolean roosting);

    // Custom movement
    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci) {
        if (this.getTarget() != null) {
            this.setRoosting(false);
            Entity target = this.getTarget();
            double adjustedEyeHeight = target.getEyeHeight(target.getPose()) - EYE_HEIGHT_COMPENSATION;
            Vec3d targetEyePos = target.getPos().add(0, adjustedEyeHeight, 0);
            Vec3d direction = targetEyePos.subtract(this.getPos()).normalize();

            this.setVelocity(direction.multiply(BatAttackGoal.getChaseSpeed()));

            double dx = targetEyePos.x - this.getX();
            double dy = targetEyePos.y - (this.getY() + this.getEyeHeight(this.getPose()));
            double dz = targetEyePos.z - this.getZ();
            double distanceXZ = Math.sqrt(dx * dx + dz * dz);

            this.setYaw((float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F);
            this.setPitch((float) (-(MathHelper.atan2(dy, distanceXZ) * (100.0 / Math.PI))));
            this.bodyYaw = this.getYaw();
        }
    }

    @Override
    public void initGoals() {
        super.initGoals();
        this.goalSelector.add(1, new BatAttackGoal((BatEntity) (Object) this));
    }

    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        if (!this.getWorld().isClient && source.getAttacker() != null) {
            LivingEntity attacker = (LivingEntity) source.getAttacker();
            this.setTarget(attacker);
        }
    }
}

