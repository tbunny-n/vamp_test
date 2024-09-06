package snuz.vamp

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

object VampClient : ClientModInitializer {
    override fun onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.

        ClientBlockHighlighting.registerRenderCallback()
        // Networking
        ClientPlayNetworking.registerGlobalReceiver(BlockHighlightPayload.ID) { payload, context ->
            context.client().execute {
                ClientBlockHighlighting.highlightBlock(context.client(), payload.blockPos)
            }
        }
    }
}