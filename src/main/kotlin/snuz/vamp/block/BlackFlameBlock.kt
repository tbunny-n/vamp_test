package snuz.vamp.block

import com.mojang.serialization.MapCodec
import net.minecraft.block.AbstractFireBlock
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.WorldAccess

const val BLACK_FLAME_DAMAGE = 9.0f // TODO: Make configurable or dynamic

class BlackFlameBlock(settings: Settings) : AbstractFireBlock(settings, BLACK_FLAME_DAMAGE) {
    companion object {
        val CODEC: MapCodec<BlackFlameBlock> = createCodec(::BlackFlameBlock)
    }

    override fun getCodec(): MapCodec<out AbstractFireBlock> {
        return CODEC
    }

    override fun isFlammable(state: BlockState?): Boolean {
        return true
    }

    // ? Not sure if I need this. Copied from SoulFireBlock
    override fun getStateForNeighborUpdate(
        state: BlockState?,
        direction: Direction?,
        neighborState: BlockState?,
        world: WorldAccess?,
        pos: BlockPos?,
        neighborPos: BlockPos?
    ): BlockState {
        return if (this.canPlaceAt(state, world, pos)) this.defaultState else Blocks.AIR.defaultState
    }
}