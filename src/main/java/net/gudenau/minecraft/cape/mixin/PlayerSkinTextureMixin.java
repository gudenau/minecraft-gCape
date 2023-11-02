package net.gudenau.minecraft.cape.mixin;

import net.gudenau.minecraft.cape.SkinData;
import net.gudenau.minecraft.cape.SkinManager;
import net.gudenau.minecraft.cape.duck.PlayerSkinTextureDuck;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.PlayerSkinTexture;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerSkinTexture.class)
public abstract class PlayerSkinTextureMixin implements PlayerSkinTextureDuck {
    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final private String url;

    @Shadow private static void stripAlpha(NativeImage image, int x1, int y1, int x2, int y2) {}
    @Shadow private static void stripColor(NativeImage image, int x1, int y1, int x2, int y2) {}

    @Inject(
        method = "remapTexture",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/texture/PlayerSkinTexture;stripAlpha(Lnet/minecraft/client/texture/NativeImage;IIII)V",
            ordinal = 0
        )
    )
    private void remapTexture(NativeImage image, CallbackInfoReturnable<NativeImage> cir) {
        int height = image.getHeight();
        int width = image.getWidth();
        if (width != 64 || height != 64) {
            return;
        }

        SkinManager.processSkin((PlayerSkinTexture)(Object) this, image);
    }

    @Unique
    private SkinData gud_cape$data;

    @Unique
    @Override
    public void gud_cape$data(@Nullable SkinData skinData) {
        gud_cape$data = skinData;
    }

    @Unique
    @Override
    public @Nullable SkinData gud_cape$data() {
        return gud_cape$data;
    }
}
