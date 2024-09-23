package snuz.vamp.network

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload

object CloakAbilityPayload : CustomPayload {
    val ID = CustomPayload.Id<CloakAbilityPayload>(VampNetworkingConstants.CLOAK_PACKET_ID)
    val CODEC: PacketCodec<RegistryByteBuf, CloakAbilityPayload> = PacketCodec.unit(CloakAbilityPayload)

    override fun getId(): CustomPayload.Id<out CustomPayload> {
        return ID
    }
}