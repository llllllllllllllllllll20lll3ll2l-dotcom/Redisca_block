package ru.redisca.block;

/**
 * Height envelope used by this mod.
 *
 * <h2>Why these numbers</h2>
 * Minecraft 26.3 packs the Y coordinate as a 12-bit signed value, derived from
 * {@code SharedConstants}:
 *
 * <pre>
 * BITS_FOR_Y   = 12
 * Y_SIZE       = (1 &lt;&lt; 12) - 32   = 4096 - 32 = 4064
 * MAX_Y        = (Y_SIZE &gt;&gt; 1) - 1 = 2031
 * MIN_Y        = MAX_Y - Y_SIZE + 1 = -2032
 * </pre>
 *
 * The {@code DimensionType} record validates its constructor
 * ({@code minY + height &lt;= 2032, minY % 16 == 0, height % 16 == 0, height &gt;= 16}),
 * so {@code -2032 .. +2031} (4064 blocks total) is what the game can represent at all.
 * This is the only envelope that keeps an already-created world loadable and
 * prevents height-related crashes, so the mod can "remove" the vanilla limit
 * completely, but cannot go beyond the game's own hard envelope.
 *
 * <p>All values here are multiples of 16 and stay inside the 12-bit packing used
 * by {@code ChunkSerializer} (section Y is written as a byte, {@code -128..127}).</p>
 */
public final class Limits {

    /** Lowest placeable Y level of every dimension. */
    public static final int MIN_Y = -2032;

    /** Total vertical span in blocks (4064 = the vanilla hard envelope). */
    public static final int HEIGHT = 4064;

    private Limits() {
    }
}
