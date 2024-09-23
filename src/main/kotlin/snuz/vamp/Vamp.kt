package snuz.vamp

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.mob.ZombieEntity
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d
import org.slf4j.LoggerFactory
import snuz.vamp.network.BloodSuckPayload
import snuz.vamp.network.FlyingRaijinPayload

object Vamp : ModInitializer {
    private val logger = LoggerFactory.getLogger("vamp")

    const val MOD_ID = "vamp"

    private val RaijinPositions: HashMap<ServerPlayerEntity, Vec3d> = HashMap()

    private const val BLOOD_SUCK_RANGE: Double = 9.0
    private const val DAYLIGHT_EVENT_INTERVAL: Long = 200L

    override fun onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // ? Not sure that I actually want to run this every tick
        ServerTickEvents.END_SERVER_TICK.register { server ->
            val overworld = server.overworld
            if (!overworld.isDay) {
                // Nighttime
                overworld.players.forEach { plr ->
                    val playerState = StateSaverAndLoader.getPlayerState(plr) ?: return@forEach
                    playerState.applyNightTimeEffects(plr)
                }
            } else {
                // Daytime
                overworld.players.forEach { plr ->
                    val playerState = StateSaverAndLoader.getPlayerState(plr) ?: return@forEach
                    playerState.applyDayTimeEffects(plr)
                }
            }
        }

        ServerPlayerEvents.AFTER_RESPAWN.register { plr, old_plr, b ->
            plr.sendMessage(Text.literal("eeee"))
            old_plr.sendMessage(Text.literal("oooo"))
        }

        // Run upon player join
        ServerPlayConnectionEvents.JOIN.register { networkHandler, packetSender, server ->
            val plr = networkHandler.player
            val playerState = StateSaverAndLoader.getPlayerState(plr)
            if (playerState == null) {
                logger.info("Player state could not be loaded for player: [${plr.name} | ${plr.id}]")
                return@register
            }

            logger.info("Player state loaded for player: [${plr.name} | ${plr.id}]")
        }
        // Networking events --

        // * Blood sucking
        PayloadTypeRegistry.playC2S().register(BloodSuckPayload.ID, BloodSuckPayload.CODEC)
        ServerPlayNetworking.registerGlobalReceiver(BloodSuckPayload.ID) { payload, context ->
            val plr = context.player()
            val playerState = StateSaverAndLoader.getPlayerState(plr) ?: return@registerGlobalReceiver
            if (!playerState.hasSanguinare || !playerState.isVampire) return@registerGlobalReceiver
            val entityId = payload.entityId
            val targetEntity = plr.serverWorld.getEntityById(entityId) ?: return@registerGlobalReceiver
            if (!targetEntity.isAlive) return@registerGlobalReceiver
            // Distance check
            if (!plr.pos.isWithinRangeOf(targetEntity.pos, BLOOD_SUCK_RANGE, 2.0)) return@registerGlobalReceiver

            val livingTarget = targetEntity as LivingEntity

            livingTarget.addStatusEffect(
                StatusEffectInstance(
                    StatusEffects.SLOWNESS, 40, 2,
                )
            )
            livingTarget.addStatusEffect(
                StatusEffectInstance(
                    StatusEffects.NAUSEA, 200, 2,
                )
            )

            plr.heal(playerState.getBloodSuckSteal())
            plr.hungerManager.saturationLevel += playerState.getBloodSuckSaturation()
            plr.hungerManager.foodLevel += playerState.getBloodSuckFood()


            targetEntity.damage(
                VampDamageTypes.of(
                    plr.world,
                    VampDamageTypes.BLOOD_SUCK_DAMAGE_TYPE,
                ), playerState.getBloodSuckDamage()
            )
            if (playerState.vampireLevel > 8) {
                targetEntity.velocity = Vec3d.ZERO // Prevent knockback
            }

            // Killed entity via blood sucking
            if (!targetEntity.isAlive) {
                if (targetEntity is VillagerEntity) {
                    // TODO: Give this a unique sanguinare increment integer
                    playerState.progressSanguinare()
                    plr.sendMessage(Text.literal("you fucked him..."))
                } else if (targetEntity is ZombieEntity) {
                    playerState.ickyBlood(plr)
                } else {
                    plr.sendMessage(Text.literal("rip vro"))
                }
            }

            playerState.lastFeed = plr.serverWorld.timeOfDay
        }

        // * Flying Raijin
        PayloadTypeRegistry.playC2S().register(FlyingRaijinPayload.ID, FlyingRaijinPayload.CODEC)
        ServerPlayNetworking.registerGlobalReceiver(FlyingRaijinPayload.ID) { _, context ->
            val plr = context.player()
            val plrPos = plr.pos

            val playerState = StateSaverAndLoader.getPlayerState(plr) ?: return@registerGlobalReceiver
            plr.sendMessage(Text.literal("Sanguine progress: " + playerState.sanguinareProgress.toString()))

            if (playerState.sanguinareProgress < 0.1) {
                return@registerGlobalReceiver
            }

            val savedRaijinPos = RaijinPositions[plr]
            // Put down sign
            if (savedRaijinPos == null || savedRaijinPos == Vec3d.ZERO) {
                RaijinPositions[plr] = plrPos
                return@registerGlobalReceiver
            }

            // Teleport to sign and reset
            plr.teleport(savedRaijinPos.x, savedRaijinPos.y, savedRaijinPos.z, true)
            RaijinPositions[plr] = Vec3d.ZERO
        }

    }
}