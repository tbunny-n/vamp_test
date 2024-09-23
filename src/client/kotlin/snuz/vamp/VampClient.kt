package snuz.vamp

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.entity.Entity
import net.minecraft.entity.mob.PillagerEntity
import net.minecraft.entity.mob.ZombieEntity
import net.minecraft.entity.passive.*
import net.minecraft.util.ActionResult
import org.lwjgl.glfw.GLFW
import snuz.vamp.network.BloodSuckPayload
import snuz.vamp.network.CloakAbilityPayload
import snuz.vamp.network.FlyingRaijinPayload

object VampClient : ClientModInitializer {
    private const val RAIJIN_COOLDOWN: Double = 1.5
    private const val CLOAK_COOLDOWN: Double = 1.5
    private lateinit var RAIJIN_KEYBIND: KeyBinding
    private lateinit var CLOAK_KEYBIND: KeyBinding

    private val FEASTABLE_MOBS: List<Class<out Entity>> = listOf(
        VillagerEntity::class.java,
        PillagerEntity::class.java,
        PigEntity::class.java,
        CowEntity::class.java,
        SheepEntity::class.java,
        GoatEntity::class.java,
        ZombieEntity::class.java,
    )

    override fun onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        RAIJIN_KEYBIND = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.vamp.raijin",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.vamp"
            )
        )
        CLOAK_KEYBIND = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.vamp.cloak",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.vamp"
            )
        )

        // Blood sucking
        UseEntityCallback.EVENT.register { player, world, hand, entity, hitResult ->
            if (!player.isSneaking) return@register ActionResult.PASS
            if (FEASTABLE_MOBS.any { it.isInstance(entity) }) {
                ClientPlayNetworking.send(BloodSuckPayload(entity.id))
            }
            return@register ActionResult.SUCCESS
        }

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (RAIJIN_KEYBIND.wasPressed()) {
                // Tell the server that client wants to use flying raijin
                ClientPlayNetworking.send(FlyingRaijinPayload)
            }
            while (CLOAK_KEYBIND.wasPressed()) {
                ClientPlayNetworking.send(CloakAbilityPayload)
            }
        }
        // Networking
    }
}