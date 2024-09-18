package snuz.vamp

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d
import org.slf4j.LoggerFactory

object Vamp : ModInitializer {
    private val logger = LoggerFactory.getLogger("vamp")

    const val MOD_ID = "vamp"

    private val RaijinPositions: HashMap<ServerPlayerEntity, Vec3d> = HashMap()

    private const val SANGUINARE_INCREMENT_AMOUNT: Float = 0.2f
    private const val TEN_MIN_DURATION: Int = 11000

    override fun onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Server events
        var lastWorldTime: Long = -1
        ServerTickEvents.END_SERVER_TICK.register { server ->
            server.worlds.forEach { world ->
                val worldTime = world.timeOfDay % 24000
                if (worldTime in 14000..23000 && lastWorldTime < 14000) {
                    // Apply nighttime status effects
                    server.playerManager.playerList.forEach { player ->
                        val playerState = StateSaverAndLoader.getPlayerState(player)
                        if (playerState != null) {
                            if (playerState.isVampire || playerState.hasSanguinare) {
                                playerState.sanguinareProgress += SANGUINARE_INCREMENT_AMOUNT // Progress sanguinare

                                player.removeStatusEffect(
                                    StatusEffects.WEAKNESS
                                )
                                player.addStatusEffect(
                                    StatusEffectInstance(
                                        StatusEffects.NIGHT_VISION,
                                        TEN_MIN_DURATION,
                                        0,
                                    )
                                )

                                val strengthAmp = when {
                                    playerState.vampireLevel > 33 -> 2
                                    playerState.sanguinareProgress > 1.4 -> 1
                                    else -> 0
                                }
                                player.addStatusEffect(
                                    StatusEffectInstance(
                                        StatusEffects.STRENGTH,
                                        TEN_MIN_DURATION,
                                        strengthAmp,
                                    )
                                )

                                val speedAmp = when {
                                    playerState.vampireLevel > 38 -> 2
                                    playerState.sanguinareProgress > 1.2 -> 1
                                    else -> 0
                                }
                                player.addStatusEffect(
                                    StatusEffectInstance(
                                        StatusEffects.SPEED,
                                        TEN_MIN_DURATION,
                                        speedAmp,
                                    )
                                )

                                val resistanceAmp = when {
                                    playerState.vampireLevel > 41 -> 2
                                    playerState.vampireLevel > 18 -> 1
                                    else -> 0
                                }
                                player.addStatusEffect(
                                    StatusEffectInstance(
                                        StatusEffects.RESISTANCE,
                                        TEN_MIN_DURATION,
                                        resistanceAmp,
                                    )
                                )
                            }
                        }
                    }
                } else if (worldTime in 200..12000 && lastWorldTime > 12000) {
                    // Apply daytime status effects
                    server.playerManager.playerList.forEach { player ->
                        val playerState = StateSaverAndLoader.getPlayerState(player)
                        if (playerState != null) {
                            if (playerState.isVampire || playerState.hasSanguinare) {
                                player.removeStatusEffect(
                                    StatusEffects.NIGHT_VISION
                                )
                                player.removeStatusEffect(
                                    StatusEffects.STRENGTH
                                )
                                player.removeStatusEffect(
                                    StatusEffects.RESISTANCE
                                )
                                player.removeStatusEffect(
                                    StatusEffects.SPEED
                                )
                                player.addStatusEffect(
                                    StatusEffectInstance(
                                        StatusEffects.WEAKNESS,
                                        TEN_MIN_DURATION,
                                        0,
                                    )
                                )
                            }
                        }
                    }
                }
                lastWorldTime = worldTime
            }
        }


        // Register networking events
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