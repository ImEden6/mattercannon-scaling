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

        // 1. Base calculation: (base * multiplier)
        double scaledBase = baseDamage * config.damageMultiplier;

        // 2. Attribute bonus calculation
        double attributeBonus = 0.0;
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

                double attrValue = inst.getValue();
                switch (entry.op()) {
                    case ADD -> attributeBonus += attrValue * entry.multiplier();
                    // MULTIPLY in this context adds a percentage of the *scaled base damage* based
                    // on the attribute
                    // e.g. if attribute is 10 and multiplier is 0.1 (10%), add 100% of scaledBase
                    // to the bonus?
                    // Or if multiplier is 0.01 (1%), add 10% of scaledBase?
                    // Logic: bonus += scaledBase * (attrValue * multiplier)
                    case MULTIPLY -> attributeBonus += scaledBase * (attrValue * entry.multiplier());
                }
            }
        }

        // 3. Final summation: scaledBase + additive + attributeBonus
        double finalValue = scaledBase + config.damageAdditive + attributeBonus;

        int finalDamage = Math.max(0, (int) Math.round(finalValue));
        MatterCannonScaling.LOGGER.debug(
                "MatterCannon damage: base={}, scaledBase={}, additive={}, attrBonus={}, final={}",
                baseDamage, scaledBase, config.damageAdditive, attributeBonus, finalDamage);
        return finalDamage;
    }
}
