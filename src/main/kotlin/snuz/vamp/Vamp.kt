package snuz.vamp

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.block.Blocks
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
        PlayerBlockBreakEvents.AFTER.register { world, player, pos, state, entity ->
            // Early return
            state.block.takeIf { it == Blocks.GRASS_BLOCK || it == Blocks.DIRT } ?: return@register
            val server = world.server ?: return@register

            val serverState = StateSaverAndLoader.getServerState(server)!!
            serverState.totalDirtBlocksBroken += 1

            val playerState = StateSaverAndLoader.getPlayerState(player)!!
            playerState.dirtBlocksBroken += 1

            // Send packet to client?
        }
    }
}