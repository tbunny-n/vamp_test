package snuz.vamp

import net.minecraft.entity.LivingEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.RegistryWrapper
import net.minecraft.server.MinecraftServer
import net.minecraft.world.PersistentState
import net.minecraft.world.PersistentStateManager
import net.minecraft.world.World
import java.util.*

class StateSaverAndLoader : PersistentState() {
    override fun writeNbt(nbtOrNull: NbtCompound?, registryLookup: RegistryWrapper.WrapperLookup?): NbtCompound? {
        val nbt = nbtOrNull ?: return null
        nbt.putInt("totalDirtBlocksBroken", totalDirtBlocksBroken)

        val playersNbt = NbtCompound()
        players.forEach { (uuid, playerData) ->
            val playerNbt = NbtCompound()
            playerNbt.putInt("dirtBlocksBroken", playerData.dirtBlocksBroken)

            playerNbt.putInt("vampireLevel", playerData.vampireLevel)
            playerNbt.putBoolean("isVampire", playerData.isVampire)
            playerNbt.putBoolean("hasSanguine", playerData.hasSanguine)
            playerNbt.putFloat("sanguineProgress", playerData.sanguineProgress)

            playersNbt.put(uuid.toString(), playerNbt)
        }
        nbt.put("players", playersNbt)

        return nbt
    }

    var totalDirtBlocksBroken: Int = 0 // ? TEMP: ?
    var players: HashMap<UUID, PlayerData> = HashMap()

    companion object {
        private val stateType: Type<StateSaverAndLoader> = Type(
            ::StateSaverAndLoader, StateSaverAndLoader::createFromNbt, null
        )

        fun getPlayerState(player: LivingEntity): PlayerData? {
            val server = player.world.server ?: return null
            val serverState = getServerState(server) ?: return null
            val playerState = serverState.players.getOrPut(player.uuid) { PlayerData() }

            return playerState
        }

        fun createFromNbt(
            tag: NbtCompound?,
            @Suppress("UNUSED_PARAMETER") registryLookup: RegistryWrapper.WrapperLookup?
        ): StateSaverAndLoader {
            val state = StateSaverAndLoader()
            if (tag == null) {
                return state
            }

            state.totalDirtBlocksBroken = tag.getInt("totalDirtBlocksBroken")

            val playersNbt = tag.getCompound("players")
            playersNbt.keys.forEach { key ->
                val playerData = PlayerData()
                playerData.dirtBlocksBroken = playersNbt.getCompound(key).getInt("dirtBlocksBroken")

                val uuid = UUID.fromString(key)
                state.players[uuid] = playerData
            }

            return state
        }

        fun getServerState(server: MinecraftServer): StateSaverAndLoader? {
            val pStateManager: PersistentStateManager? = server.getWorld(World.OVERWORLD)?.persistentStateManager

            val state = pStateManager?.getOrCreate(stateType, Vamp.MOD_ID)
            state?.markDirty()

            return state
        }
    }

}