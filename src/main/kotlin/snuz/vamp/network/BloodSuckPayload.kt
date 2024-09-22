package snuz.vamp.network

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload

data class BloodSuckPayload(val entityId: Int) : CustomPayload {
    companion object {
        val ID = CustomPayload.Id<BloodSuckPayload>(VampNetworkingConstants.BLOOD_SUCK_ID)
        val CODEC: PacketCodec<RegistryByteBuf, BloodSuckPayload> =
            PacketCodec.tuple(PacketCodecs.INTEGER, BloodSuckPayload::entityId, ::BloodSuckPayload)
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> {
        return ID
    }
}