package ru.redisca.block.mixins;

import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.redisca.block.Limits;

/**
 * The one place every height limit flows through.
 *
 * <p>In Minecraft 1.18+ (including 26.3) the whole vertical model is derived from two
 * methods of the single {@code DimensionType} record used by each dimension:</p>
 * <ul>
 *     <li>{@code minY()} — lowest placeable block level,</li>
 *     <li>{@code height()} — total vertical span.</li>
 * </ul>
 * Everything else — {@code getMinBuildHeight()}, {@code getMaxBuildHeight()},
 * sections array capacity, {@code Heightmap} bit-width, chunk section Y encoding,
 * and the client's copy of the dimension (synced through the registry, which codes the
 * <em>live</em> {@code DimensionType} accessor methods) — is computed from these two.
 * Overriding them therefore removes both the floor ({@code -64}) and the ceiling
 * ({@code +320} / {@code 319}) for every dimension, in one shot.</p>
 *
 * <p>{@code logicalHeight()} is deliberately <em>not</em> modified: it is the separate
 * gameplay limit of "maximum height to which chorus fruits and nether portals can bring
 * players", not a build limit, and removing it would change gameplay beyond the request.</p>
 *
 * <p>Minecraft 26.1 and later ship unobfuscated: there is no obfuscated build with SRG
 * names to remap to, so every inject/annotation must say {@code remap = false} — the
 * written method name ({@code "minY"}) is exactly the name the game and the mixin
 * processor use at runtime. Forget this and the Mixin annotation processor fails with
 * {@code Unable to locate obfuscation mapping for @Inject target minY}.</p>
 */
@Mixin(DimensionType.class)
public abstract class DimensionTypeMixin {

    @Inject(method = "minY", at = @At("HEAD"), cancellable = true, remap = false)
    private void rediscaBlock$minY(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Limits.MIN_Y);
    }

    @Inject(method = "height", at = @At("HEAD"), cancellable = true, remap = false)
    private void rediscaBlock$height(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Limits.HEIGHT);
    }
}
