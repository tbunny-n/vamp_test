package snuz.vamp

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.math.BlockPos

data class BlockHighlightPayload(val blockPos: BlockPos) : CustomPayload {
    companion object {
        val ID = CustomPayload.Id<BlockHighlightPayload>(VampNetworkingConstants.HIGHLIGHT_PACKET_ID)
        val CODEC: PacketCodec<RegistryByteBuf, BlockHighlightPayload> =
            PacketCodec.tuple(BlockPos.PACKET_CODEC, BlockHighlightPayload::blockPos, ::BlockHighlightPayload)
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> {
        return ID
    }
}