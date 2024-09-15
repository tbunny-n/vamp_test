package snuz.vamp

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import org.slf4j.LoggerFactory

object Vamp : ModInitializer {
    private val logger = LoggerFactory.getLogger("vamp")

    const val MOD_ID = "vamp"
    val DIRT_BROKEN: Identifier = Identifier.of(MOD_ID, "dirt_broken")

    val RaijinPositions: HashMap<ServerPlayerEntity, Vec3d> = HashMap()

    override fun onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        logger.info("Hello Fabric world!")

        // TODO: Register networking events

        PayloadTypeRegistry.playC2S().register(FlyingRaijinPayload.ID, FlyingRaijinPayload.CODEC)
        ServerPlayNetworking.registerGlobalReceiver(FlyingRaijinPayload.ID) { payload, context ->
            // TODO: Check if flying raijin is valid
            // then send packet to update client
            val plr = context.player()
            plr.sendMessage(Text.literal("I see you bitch"), false)
            val blockPos = payload.blockPos // TODO: Get rid of this, I'm just using the player's pos
            val plrPos = plr.pos

            val raijinPos = RaijinPositions[plr]
            // Put down sign
            if (raijinPos == null || raijinPos == Vec3d.ZERO) {
                RaijinPositions[plr] = plrPos
                plr.sendMessage(Text.literal("Flying..."), false)
                return@registerGlobalReceiver
            }

            // Teleport to sign and reset
            plr.teleport(raijinPos.x, raijinPos.y, raijinPos.z, true)
            RaijinPositions[plr] = Vec3d.ZERO
            plr.sendMessage(Text.literal("Raijin!!"), false)
        }

//        PlayerBlockBreakEvents.AFTER.register { world, player, pos, state, entity ->
//            // Early return
//            state.block.takeIf { it == Blocks.GRASS_BLOCK || it == Blocks.DIRT } ?: return@register
//            val server = world.server ?: return@register
//
//            val serverState = StateSaverAndLoader.getServerState(server)!!
//            serverState.totalDirtBlocksBroken += 1
//
//            val playerState = StateSaverAndLoader.getPlayerState(player)!!
//            playerState.dirtBlocksBroken += 1
//
//            // Send packet to client?
//        }
    }
}