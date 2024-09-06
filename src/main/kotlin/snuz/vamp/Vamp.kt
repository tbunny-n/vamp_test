package snuz.vamp

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.block.Blocks
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

object Vamp : ModInitializer {
    private val logger = LoggerFactory.getLogger("vamp")

    const val MOD_ID = "vamp"
    val DIRT_BROKEN: Identifier = Identifier.of(MOD_ID, "dirt_broken")

    override fun onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        logger.info("Hello Fabric world!")

        // Register networking events
        PayloadTypeRegistry.playS2C().register(BlockHighlightPayload.ID, BlockHighlightPayload.CODEC)

        // ! TEMP:
        // This is to increment player block count as a test for attaching
        // NBT data onto players (persistent)
        PlayerBlockBreakEvents.AFTER.register { world, player, pos, state, entity ->
            if (state.block == Blocks.GRASS_BLOCK || state.block == Blocks.DIRT) {
                val worldServer = world.server ?: return@register
                val serverState: StateSaverAndLoader =
                    StateSaverAndLoader.getServerState(worldServer) ?: return@register
                serverState.totalDirtBlocksBroken += 1

                // Send a packet to the client
                val data: PacketByteBuf = PacketByteBufs.create()
                data.writeInt(serverState.totalDirtBlocksBroken)

                val playerEntity: ServerPlayerEntity =
                    worldServer.playerManager.getPlayer(player.uuid) ?: return@register
                worldServer.execute {
                    ServerPlayNetworking.send(playerEntity, BlockHighlightPayload(playerEntity.blockPos))
                }
            }
        }
    }
}