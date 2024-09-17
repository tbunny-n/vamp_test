package snuz.vamp

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d
import org.slf4j.LoggerFactory

object Vamp : ModInitializer {
    private val logger = LoggerFactory.getLogger("vamp")

    const val MOD_ID = "vamp"

    private val RaijinPositions: HashMap<ServerPlayerEntity, Vec3d> = HashMap()

    private const val SANGUINARE_INCREMENT_AMOUNT: Float = 0.2f

    override fun onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Server events
        var lastNightTime = 0L
        ServerTickEvents.END_SERVER_TICK.register { server ->
            server.worlds.forEach { world ->
                val currentTime = world.timeOfDay % 24000 // Get time of day (0 - 24000)
                val currentNight = world.timeOfDay / 24000

                // Increment sanguinare specimen once per night
                if (currentTime in 18000..20000 && currentNight != lastNightTime) {
                    lastNightTime = currentNight

                    server.playerManager.playerList.forEach { player ->
                        val playerState = StateSaverAndLoader.getPlayerState(player)
                        if (playerState != null) {
                            if (playerState.hasSanguinare) {
                                playerState.sanguinareProgress += SANGUINARE_INCREMENT_AMOUNT
                                player.sendMessage(Text.literal("GRAHHHH!!!!"))
                            }
                        }
                    }
                }
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