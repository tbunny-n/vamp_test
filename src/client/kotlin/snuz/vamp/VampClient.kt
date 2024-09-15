package snuz.vamp

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW

object VampClient : ClientModInitializer {
    private lateinit var RAIJIN_KEYBIND: KeyBinding
    override fun onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        RAIJIN_KEYBIND = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.vamp.raijin",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.vamp.raijin"
            )
        )

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (RAIJIN_KEYBIND.wasPressed()) {
                val plr = client.player ?: return@register
                plr.sendMessage(Text.literal("Key R was pressed!"), false)
                // TODO: Tell the server that client wants to use flying raijin
            }
        }
        // Networking
    }
}