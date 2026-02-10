package net.mervyn.mattercannonscaling.mixin;

import appeng.items.tools.powered.MatterCannonItem;
import net.mervyn.mattercannonscaling.MatterCannonScaling;
import net.mervyn.mattercannonscaling.config.ScalingConfig;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MatterCannonItem.class)
public abstract class MatterCannonItemMixin {

    // getDamageFromPenetration is PUBLIC STATIC in AE2 — shadow must match
    @Shadow(remap = false)
    public static int getDamageFromPenetration(float penetration) {
        throw new AssertionError();
    }

    /**
     * Redirects the getDamageFromPenetration call inside standardAmmo to apply
     * config scaling and attribute-based bonuses.
     *
     * CRITICAL: getDamageFromPenetration is a STATIC method (invokestatic in
     * bytecode).
     * For static method redirects, the handler must NOT have an instance parameter.
     * Parameters are: [target method args] + [captured enclosing method args].
     */
    @Redirect(method = "standardAmmo", at = @At(value = "INVOKE", target = "Lappeng/items/tools/powered/MatterCannonItem;getDamageFromPenetration(F)I"), remap = false)
    private int modifyCalculatedDamage(
            // getDamageFromPenetration's argument (static call — NO instance param):
            float penetration,
            // Captured enclosing method (standardAmmo) parameters:
            float enclosingPenetration, World level,
            PlayerEntity p, Vec3d v1, Vec3d v2,
            Vec3d dir, double d0, double d1, double d2) {

        int baseDamage = getDamageFromPenetration(penetration);

        ScalingConfig config = ScalingConfig.get();
        double scaled = (baseDamage * config.damageMultiplier) + config.damageAdditive;

        if (p != null) {
            for (ScalingConfig.CachedEntry entry : config.cachedEntries) {
                EntityAttribute attr = Registries.ATTRIBUTE.get(entry.attributeId());
                if (attr == null) {
                    continue;
                }

                EntityAttributeInstance inst = p.getAttributeInstance(attr);
                if (inst == null) {
                    continue;
                }

                double value = inst.getValue();
                switch (entry.op()) {
                    case ADD -> scaled += value * entry.multiplier();
                    case MULTIPLY -> scaled *= 1.0 + (value * entry.multiplier());
                }
            }
        }

        int finalDamage = Math.max(0, (int) Math.round(scaled));
        MatterCannonScaling.LOGGER.debug(
                "MatterCannon damage: base={}, scaled={}, final={}",
                baseDamage, scaled, finalDamage);
        return finalDamage;
    }
}
