package net.gudenau.minecraft.cape.mixin;

import net.gudenau.minecraft.cape.duck.PlayerSkinTextureDuck;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.Deadmau5FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Deadmau5FeatureRenderer.class)
public abstract class Deadmau5FeatureRendererMixin {
    @ModifyConstant(
        method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFF)V"
    )
    private String earCheck(String original, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, AbstractClientPlayerEntity abstractClientPlayerEntity, float f, float g, float h, float j, float k, float l) {
        var textureId = abstractClientPlayerEntity.getSkinTextures().texture();
        var texture = MinecraftClient.getInstance().getTextureManager().getTexture(textureId);
        if(texture instanceof PlayerSkinTextureDuck duck) {
            var data = duck.gud_cape$data();
            if(data != null && data.ears()) {
                return abstractClientPlayerEntity.getName().getString();
            }
        }
        return original;
    }
}
