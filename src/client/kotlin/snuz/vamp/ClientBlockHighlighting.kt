package snuz.vamp

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

class ClientBlockHighlighting {
    companion object {
        private var blockPos: BlockPos? = null

        fun highlightBlock(client: MinecraftClient, pos: BlockPos) {
            blockPos = pos
        }

        fun renderHighlight(client: MinecraftClient, matrices: MatrixStack, tickDelta: Float) {
            val pos = blockPos ?: return
            val cameraPos: Vec3d = client.gameRenderer.camera.pos
            matrices.push()

            matrices.translate(
                pos.x - cameraPos.x,
                pos.y - cameraPos.y,
                pos.z - cameraPos.z
            )

            val box = Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0) // The block bounds
            val vertexConsumers = client.bufferBuilders.entityVertexConsumers
            val outlineColor = 0xFFFFFF // White outline color

            // Draw the block outline
            WorldRenderer.drawBox(
                matrices,
                vertexConsumers.getBuffer(RenderLayer.getLines()),
                box,
                (outlineColor shr 16 and 255) / 255.0f,
                (outlineColor shr 8 and 255) / 255.0f,
                (outlineColor and 255) / 255.0f,
                1.0f // Line width
            )

            matrices.pop()
        }

        fun registerRenderCallback() {
            WorldRenderEvents.AFTER_ENTITIES.register(WorldRenderEvents.AfterEntities { context ->
                val matrixStack = context.matrixStack() ?: return@AfterEntities
                val client = MinecraftClient.getInstance()
                val tickDelta = 1.0F // ? Not sure about this one
                renderHighlight(client, matrixStack, tickDelta)
            })
        }
    }
}