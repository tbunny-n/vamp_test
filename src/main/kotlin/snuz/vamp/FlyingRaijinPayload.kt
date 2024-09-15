package snuz.vamp

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.math.BlockPos

data class FlyingRaijinPayload(val blockPos: BlockPos) : CustomPayload {
    companion object {
        val ID = CustomPayload.Id<FlyingRaijinPayload>(VampNetworkingConstants.RAIJIN_PACKET_ID)
        val CODEC: PacketCodec<RegistryByteBuf, FlyingRaijinPayload> = PacketCodec.tuple(
            BlockPos.PACKET_CODEC,
            FlyingRaijinPayload::blockPos,
            ::FlyingRaijinPayload
        )
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> {
        return ID
    }
}