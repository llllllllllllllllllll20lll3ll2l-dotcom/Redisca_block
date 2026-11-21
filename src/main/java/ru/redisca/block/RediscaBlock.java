package ru.redisca.block;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Redisca Block — removes the vanilla build height limits (both the floor and the ceiling)
 * of every dimension type in Minecraft 26.3, for the whole world.
 *
 * <p>This mod is a pure patch: it extends the inter-dimensional envelope from
 * the vanilla {@code -64 .. 319} (384 blocks) up to the maximum vanilla-representable
 * envelope {@code -2032 .. 2031} (4064 blocks). It does not add any blocks, items,
 * entities or dimensions.</p>
 */
@Mod(RediscaBlock.MOD_ID)
public final class RediscaBlock {

    public static final String MOD_ID = "redisca_block";
    public static final Logger LOGGER = LogManager.getLogger("RediscaBlock");

    public RediscaBlock(FMLJavaModLoadingContext context) {
        LOGGER.info("[{}] Build height limits extended: Y = {} .. {} ({} blocks total)",
                MOD_ID, Limits.MIN_Y, Limits.MIN_Y + Limits.HEIGHT - 1, Limits.HEIGHT);
    }
}
