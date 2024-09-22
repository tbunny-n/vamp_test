package snuz.vamp

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
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

        // However, some things (like resources) may stil be uninitialized.
        // Proceed with mild caution.

        var lastDaylightEvent = 0L
        ServerTickEvents.END_SERVER_TICK.register { server ->
            val overworld = server.overworld
            val worldTicks = overworld.timeOfDay
            if (worldTicks - lastDaylightEvent >= DAYLIGHT_EVENT_INTERVAL) {
                overworld.players.forEach { player -> player.sendMessage(Text.literal("Hi bitch")) }
                val dayNumber = worldTicks / 24000
                val timeOfDay = worldTicks % 24000

                if (timeOfDay in 13000..24000) {
                    // Nighttime
                    logger.info("Goon night")
                } else {
                    // Daytime
                    logger.info("Goon morning")
                }

                lastDaylightEvent = worldTicks // Reset lastDaylightEvents
            }
        }

        ServerPlayerEvents.AFTER_RESPAWN.register { plr, old_plr, b ->
            plr.sendMessage(Text.literal("eeee"))
            old_plr.sendMessage(Text.literal("oooo"))
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

            val livingVillager = targetEntity as LivingEntity

            livingVillager.addStatusEffect(
                StatusEffectInstance(
                    StatusEffects.SLOWNESS, 40, 2,
                )
            )
            livingVillager.addStatusEffect(
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

            // Killed villager, progress sanguinare
            // TODO: Give this a unique sanguinare increment integer
            if (!targetEntity.isAlive) {
                playerState.progressSanguinare()
                plr.sendMessage(Text.literal("you fucked him..."))
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