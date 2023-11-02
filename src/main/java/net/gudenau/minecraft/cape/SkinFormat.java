package net.gudenau.minecraft.cape;

import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static net.gudenau.minecraft.cape.GCape.MODID;

enum SkinFormat {
    VERSION_0 {
        @Override
        SkinData handle(ByteBuffer data) {
            var json = JsonParser.parseString(new String(data.array(), data.position(), data.remaining(), StandardCharsets.UTF_8)).getAsJsonObject();

            var cape = upload("cape", json.getAsJsonPrimitive("cape"));
            var elytra = upload("elytra", json.getAsJsonPrimitive("elytra"));
            var ears = bool(json.getAsJsonPrimitive("ears"));

            return new SkinData(cape, elytra, ears);
        }

        private boolean bool(@Nullable JsonPrimitive primitive) {
            return primitive != null && primitive.getAsBoolean();
        }

        private @Nullable Identifier upload(String type, @Nullable JsonPrimitive primitive) {
            if(primitive == null) {
                return null;
            }

            var payload = Base64.getDecoder().decode(primitive.getAsString());
            NativeImage image = null;
            try {
                image = NativeImage.read(payload);
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
            var textureManager = MinecraftClient.getInstance().getTextureManager();
            var identifier = new Identifier(MODID, type + "/asdf");
            textureManager.registerTexture(identifier, new NativeImageBackedTexture(image));
            return identifier;
        }
    },
    ;

    @Nullable
    static SkinFormat format(int format) {
        var values = values();
        if(format < 0 || format >= values.length) {
            return null;
        } else {
            return values[format];
        }
    }

    @Nullable
    abstract SkinData handle(ByteBuffer data);
}
