package snuz.vamp.network

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.math.BlockPos

data class AmaterasuPayload(val targetPos: BlockPos) : CustomPayload {
    companion object {
        val ID = CustomPayload.Id<AmaterasuPayload>(VampNetworkingConstants.AMATERASU_PACKET_ID)
        val CODEC: PacketCodec<RegistryByteBuf, AmaterasuPayload> =
            PacketCodec.tuple(BlockPos.PACKET_CODEC, AmaterasuPayload::targetPos, ::AmaterasuPayload)
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> {
        return AmaterasuPayload.ID
    }
}
