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
    private const val DAYLIGHT_TICK_INTERVAL: Long = 5L

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
                    logger.info("Happy halloween fucker")
                    // Apply nighttime status effects
                    server.playerManager.playerList.forEach { player ->
                        val playerState = StateSaverAndLoader.getPlayerState(player)
                        if (playerState != null) {
                            if (playerState.isVampire || playerState.hasSanguinare) {
                                player.removeStatusEffect(
                                    StatusEffects.WEAKNESS
                                )
                                player.addStatusEffect(
                                    StatusEffectInstance(
                                        StatusEffects.NIGHT_VISION,
                                        11000,
                                        0,
                                    )
                                )
                                player.addStatusEffect(
                                    StatusEffectInstance(
                                        StatusEffects.STRENGTH,
                                        11000,
                                        0,
                                    )
                                )
                                player.addStatusEffect(
                                    StatusEffectInstance(
                                        StatusEffects.RESISTANCE,
                                        11000,
                                        0,
                                    )
                                )
                            }
                        }
                    }
                } else if (worldTime in 1000..12000 && lastWorldTime > 12000) {
                    // Apply daytime status effects
                    logger.info("WEEHEEHEEHEE")
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
                                player.addStatusEffect(
                                    StatusEffectInstance(
                                        StatusEffects.WEAKNESS,
                                        11000,
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