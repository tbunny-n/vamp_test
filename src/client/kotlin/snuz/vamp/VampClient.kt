package snuz.vamp

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.entity.Entity
import net.minecraft.entity.mob.PillagerEntity
import net.minecraft.entity.mob.ZombieEntity
import net.minecraft.entity.passive.*
import net.minecraft.util.ActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import org.lwjgl.glfw.GLFW
import snuz.vamp.Vamp.AMATERASU_RANGE
import snuz.vamp.network.AmaterasuPayload
import snuz.vamp.network.BloodSuckPayload
import snuz.vamp.network.CloakAbilityPayload
import snuz.vamp.network.FlyingRaijinPayload

object VampClient : ClientModInitializer {
    private const val RAIJIN_COOLDOWN: Int = 30
    private const val CLOAK_COOLDOWN: Int = 50
    private const val SPEED_COOLDOWN: Int = 50
    private const val AMATERASU_COOLDOWN: Int = 500
    private var lastRaijin = RAIJIN_COOLDOWN
    private var lastCloak = CLOAK_COOLDOWN
    private var lastSpeed = SPEED_COOLDOWN
    private var lastAmaterasu = AMATERASU_COOLDOWN

    private lateinit var RAIJIN_KEYBIND: KeyBinding
    private lateinit var CLOAK_KEYBIND: KeyBinding
    private lateinit var SPEED_KEYBIND: KeyBinding
    private lateinit var AMATERASU_KEYBIND: KeyBinding

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
                "key.vamp.raijin", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.vamp"
            )
        )
        CLOAK_KEYBIND = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.vamp.cloak", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.vamp"
            )
        )
        SPEED_KEYBIND = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.vamp.speed", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.vamp"
            )
        )
        AMATERASU_KEYBIND = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.vamp.amaterasu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, "category.vamp"
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
                if (lastRaijin < RAIJIN_COOLDOWN) break
                ClientPlayNetworking.send(FlyingRaijinPayload)
                lastRaijin = 0
            }
            while (CLOAK_KEYBIND.wasPressed()) {
                if (lastCloak < CLOAK_COOLDOWN) break
                ClientPlayNetworking.send(CloakAbilityPayload)
                lastCloak = 0
            }
            while (AMATERASU_KEYBIND.wasPressed()) {
                if (lastAmaterasu < AMATERASU_COOLDOWN) break
                // Get blocks player is looking at
                val amaterasuPayload = amaterasuClient(client) ?: break // am i stupid
                ClientPlayNetworking.send(amaterasuPayload)
                lastAmaterasu = 0
            }
            while (SPEED_KEYBIND.wasPressed()) {
                if (lastSpeed < SPEED_COOLDOWN) break
                // TODO:
                lastSpeed = 0
            }

            // Increment tick counters for cooldowns
            lastRaijin = updateCounter(lastRaijin, RAIJIN_COOLDOWN)
            lastCloak = updateCounter(lastCloak, CLOAK_COOLDOWN)
            lastAmaterasu = updateCounter(lastAmaterasu, AMATERASU_COOLDOWN)
            lastSpeed = updateCounter(lastSpeed, SPEED_COOLDOWN)
        }
    }

    private fun updateCounter(counter: Int, cooldown: Int): Int {
        return (counter + 1).coerceAtMost(cooldown)
    }

    private fun amaterasuClient(client: MinecraftClient): AmaterasuPayload? {
        val hit = client.cameraEntity?.raycast(AMATERASU_RANGE, 1.0F, true) ?: return null
        if (hit is BlockHitResult) {
            return AmaterasuPayload(hit.blockPos)
        } else if (hit is EntityHitResult) {
            return AmaterasuPayload(hit.entity.blockPos)
        }
        return null
    }
}