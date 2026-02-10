package net.mervyn.mattercannonscaling;

import net.fabricmc.api.ModInitializer;
import net.mervyn.mattercannonscaling.config.ScalingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatterCannonScaling implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("mattercannon-scaling");

    @Override
    public void onInitialize() {
        ScalingConfig.load();
        LOGGER.info("Matter Cannon Scaling loaded. Multiplier={}, Additive={}, Entries={}",
                ScalingConfig.get().damageMultiplier,
                ScalingConfig.get().damageAdditive,
                ScalingConfig.get().scalingEntries.size());
    }
}
