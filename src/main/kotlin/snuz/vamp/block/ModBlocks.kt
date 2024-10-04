package snuz.vamp.block

import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.MapColor
import net.minecraft.block.piston.PistonBehavior
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import snuz.vamp.Vamp

object ModBlocks {
    private val logger = Vamp.LOGGER

    val BLACK_FLAME_BLOCK: Block = registerBlock(
        "black_flame_block",
        BlackFlameBlock(
            AbstractBlock.Settings.create()
                .mapColor(MapColor.BLACK)
                .noCollision()
                .luminance { _ -> 10 }
                .pistonBehavior(PistonBehavior.IGNORE)
        )
    )

    private fun registerBlock(name: String, block: Block): Block {
        registerBlockItem(name, block)
        return Registry.register(Registries.BLOCK, Identifier.of(Vamp.MOD_ID, name), block)
    }

    private fun registerBlockItem(name: String, block: Block) {
        Registry.register(
            Registries.ITEM, Identifier.of(Vamp.MOD_ID, name),
            BlockItem(block, Item.Settings())
        )
    }

    fun registerModBlocks() {
        logger.info("Registering mod blocks for ${Vamp.MOD_ID}")
    }
}