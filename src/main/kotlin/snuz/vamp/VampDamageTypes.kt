package snuz.vamp

import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.damage.DamageType
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
import net.minecraft.world.World

object VampDamageTypes {
    /*
     * Store `RegistryKey` of `DamageType`s into constants
    */
    val BAT_DAMAGE_TYPE: RegistryKey<DamageType> =
        RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of("vamp", "bat_damage"))

    fun of(world: World, key: RegistryKey<DamageType>): DamageSource {
        return DamageSource(world.registryManager[RegistryKeys.DAMAGE_TYPE].entryOf(key))
    }
}