package snuz.vamp

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.block.Blocks
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.mob.ZombieEntity
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.GameRules
import org.slf4j.LoggerFactory
import snuz.vamp.block.ModBlocks
import snuz.vamp.mixin.ServerWorldAccessors
import snuz.vamp.network.AmaterasuPayload
import snuz.vamp.network.BloodSuckPayload
import snuz.vamp.network.CloakAbilityPayload
import snuz.vamp.network.FlyingRaijinPayload

object Vamp : ModInitializer {
    val LOGGER = LoggerFactory.getLogger("vamp")

    const val MOD_ID = "vamp"

    private val RaijinPositions: HashMap<ServerPlayerEntity, Vec3d> = HashMap()

    private const val BLOOD_SUCK_RANGE: Double = 9.0
    private const val DAYLIGHT_EVENT_INTERVAL: Long = 200L
    const val AMATERASU_RANGE = 500.0 // TODO: Make this configurable

    override fun onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        ModBlocks.registerModBlocks()

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

        EntitySleepEvents.ALLOW_SLEEP_TIME.register(EntitySleepEvents.AllowSleepTime { plr, _, vanillaResult ->
            val playerState = StateSaverAndLoader.getPlayerState(plr) ?: return@AllowSleepTime ActionResult.PASS
            if (playerState.sanguinareProgress < 0.1) return@AllowSleepTime ActionResult.PASS
            val serverWorld = plr.world as ServerWorld
            if (!serverWorld.isDay) return@AllowSleepTime ActionResult.FAIL
            serverWorld.updateSleepingPlayers()
            val sleepPercentage = serverWorld.gameRules.getInt(GameRules.PLAYERS_SLEEPING_PERCENTAGE)
            val sleepManager = (serverWorld as ServerWorldAccessors).sleepManager
            if (sleepManager.canSkipNight(sleepPercentage) && sleepManager.canResetTime(
                    sleepPercentage,
                    (serverWorld as ServerWorldAccessors).players
                )
            ) {
                if (serverWorld.gameRules.getBoolean(GameRules.DO_DAYLIGHT_CYCLE)) {
                    val t = serverWorld.timeOfDay + 12000L
                    serverWorld.timeOfDay = t - t % 12000L
                }
                (serverWorld as ServerWorldAccessors).invokeWakeSleepingPlayers()
                if (serverWorld.gameRules.getBoolean(GameRules.DO_WEATHER_CYCLE)) (serverWorld as ServerWorldAccessors).invokeResetWeather()
            }
            return@AllowSleepTime if (vanillaResult == serverWorld.isNight) ActionResult.SUCCESS else ActionResult.PASS
        })

        // Run upon player join
        ServerPlayConnectionEvents.JOIN.register { networkHandler, packetSender, server ->
            val plr = networkHandler.player
            val playerState = StateSaverAndLoader.getPlayerState(plr)
            if (playerState == null) {
                LOGGER.info("Player state could not be loaded for player: [${plr.name} | ${plr.id}]")
                return@register
            }

            LOGGER.info("Player state loaded for player: [${plr.name} | ${plr.id}]")
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

        // * Cloak
        PayloadTypeRegistry.playC2S().register(CloakAbilityPayload.ID, CloakAbilityPayload.CODEC)
        ServerPlayNetworking.registerGlobalReceiver(CloakAbilityPayload.ID) { _, context ->
            val plr = context.player()
            val playerState = StateSaverAndLoader.getPlayerState(plr) ?: return@registerGlobalReceiver
            if (playerState.vampireLevel < 26) return@registerGlobalReceiver

            val isInvisible = plr.hasStatusEffect(StatusEffects.INVISIBILITY)
            if (!isInvisible) {
                plr.addStatusEffect(
                    StatusEffectInstance(
                        StatusEffects.INVISIBILITY,
                        20000,
                        5,
                        false,
                        false,
                        true,
                    )
                )
            } else {
                plr.removeStatusEffect(StatusEffects.INVISIBILITY)
            }
        }

        // * Amaterasu
        PayloadTypeRegistry.playC2S().register(AmaterasuPayload.ID, AmaterasuPayload.CODEC)
        ServerPlayNetworking.registerGlobalReceiver(AmaterasuPayload.ID) { payload, context ->
            val plr = context.player()
            val plrPos = plr.pos
            val targetPos = payload.targetPos
            val world = plr.serverWorld

            // Player checks
            val playerState = StateSaverAndLoader.getPlayerState(plr) ?: return@registerGlobalReceiver
            if (playerState.vampireLevel < 43) return@registerGlobalReceiver
            if (!targetPos.isWithinDistance(plrPos, AMATERASU_RANGE)) return@registerGlobalReceiver

            /* NOTES:
              The amaterasu flame should "drip" off of things initially.
              Then it is an inextinguishable black flame.
              It only burns through blocks initially which is
              done by the server here, so the flame does not have
              a block destruction property at all.
              Entities do not stop burning once lit.
              The flame should easily exist when submerge or on top of water.
            */

            BlockPos.iterate(targetPos.add(-2, -2, -2), targetPos.add(2, 2, 2)).forEach { pos ->
                // Light surrounding blocks
                if (world.isAir(pos.up())) {
                    world.setBlockState(pos.up(), Blocks.FIRE.defaultState)
                }

                // Drip down
                var lastAirBlock = pos.down()
                while (world.isAir(lastAirBlock)) {
                    lastAirBlock = lastAirBlock.down()
                }

                if (world.isAir(lastAirBlock.up())) {
                    world.setBlockState(lastAirBlock.up(), Blocks.FIRE.defaultState)
                }
            }

            plr.sendMessage(Text.literal("Amaterasu!!"))
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
