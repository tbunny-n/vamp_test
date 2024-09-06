package snuz.vamp

import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.RegistryWrapper
import net.minecraft.server.MinecraftServer
import net.minecraft.world.PersistentState
import net.minecraft.world.PersistentStateManager
import net.minecraft.world.World

class StateSaverAndLoader : PersistentState() {
    var totalDirtBlocksBroken: Int = 0

    override fun writeNbt(nbt: NbtCompound?, registryLookup: RegistryWrapper.WrapperLookup?): NbtCompound? {
        nbt?.putInt("totalDirtBlocksBroken", totalDirtBlocksBroken)
        return nbt
    }

    companion object {
        private val stateType: Type<StateSaverAndLoader> = Type(
            ::StateSaverAndLoader, StateSaverAndLoader::createFromNbt, null
        )

        fun createFromNbt(tag: NbtCompound?, registryLookup: RegistryWrapper.WrapperLookup?): StateSaverAndLoader {
            val state = StateSaverAndLoader()
            state.totalDirtBlocksBroken = tag?.getInt("totalDirtBlocksBroken") ?: 0
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