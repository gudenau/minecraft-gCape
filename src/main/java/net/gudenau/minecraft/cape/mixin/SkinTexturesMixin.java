package net.gudenau.minecraft.cape.mixin;

import net.gudenau.minecraft.cape.SkinManager;
import net.gudenau.minecraft.cape.duck.PlayerSkinTextureDuck;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Debug(export = true)
@Mixin(SkinTextures.class)
public abstract class SkinTexturesMixin {
    @Shadow @Final @Mutable private @Nullable Identifier capeTexture;
    @Shadow @Final @Mutable private @Nullable Identifier elytraTexture;

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    private void init(Identifier texture, @Nullable String textureUrl, @Nullable Identifier capeTexture, @Nullable Identifier elytraTexture, SkinTextures.Model model, boolean secure, CallbackInfo ci) {
        var client = MinecraftClient.getInstance();
        var skin = client.getTextureManager().getTexture(texture);
        if(!(skin instanceof PlayerSkinTextureDuck skinDuck)) {
            return;
        }
        var data = skinDuck.gud_cape$data();
        if(data == null) {
            return;
        }

        if(data.cape() != null) {
            this.capeTexture = data.cape();
        }
        if(data.elytra() != null || data.cape() != null) {
            this.elytraTexture = data.elytra();
        }
    }
}
