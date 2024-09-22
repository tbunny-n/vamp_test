package snuz.vamp.network

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload

object FlyingRaijinPayload : CustomPayload {
    val ID = CustomPayload.Id<FlyingRaijinPayload>(VampNetworkingConstants.RAIJIN_PACKET_ID)
    val CODEC: PacketCodec<RegistryByteBuf, FlyingRaijinPayload> = PacketCodec.unit(FlyingRaijinPayload)

    override fun getId(): CustomPayload.Id<out CustomPayload> {
        return ID
    }
}