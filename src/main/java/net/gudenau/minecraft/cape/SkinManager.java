package net.gudenau.minecraft.cape;

import com.sun.jna.platform.unix.X11;
import io.netty.buffer.ByteBuf;
import net.gudenau.minecraft.cape.duck.PlayerSkinTextureDuck;
import net.gudenau.minecraft.cape.mixin.NativeImageAccessor;
import net.gudenau.minecraft.cape.mixin.PlayerSkinTextureMixin;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.PlayerSkinTexture;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;

public final class SkinManager {
    private static final UUID MAGIC = UUID.fromString("767ffcf1-7ecb-d747-5df0-a3a0d2486a4d");

    private static BitSet validPixels;

    private SkinManager() {
        throw new AssertionError();
    }

    static void init() {
        CompletableFuture.runAsync(SkinManager::doInit);
    }

    private static void doInit() {
        BitSet validPixels;

        try(
            var stream = SkinManager.class.getResourceAsStream("/assets/gud_cape/mask.png");
            var image = NativeImage.read(stream)
        ) {
            if(image.getWidth() != 64 || image.getHeight() != 64) {
                GCape.LOGGER.error("Failed to load skin mask: was not 64x64");
                System.exit(1);
            }

            validPixels = new BitSet();
            @SuppressWarnings("DataFlowIssue")
            var pixels = MemoryUtil.memIntBuffer(((NativeImageAccessor)(Object) image).getPointer(), 64 * 64);
            while(pixels.hasRemaining()) {
                if(pixels.get() == 0xFF_00_00_00) {
                    validPixels.set(pixels.position() - 1);
                }
            }
        } catch(IOException e) {
            GCape.LOGGER.error("Failed to load skin mask", e);
            System.exit(1);
            return;
        }

        SkinManager.validPixels = validPixels;
    }

    @NotNull
    private static ByteBuffer extractData(@NotNull NativeImage image) {
        @SuppressWarnings("DataFlowIssue")
        var pixels = MemoryUtil.memIntBuffer(((NativeImageAccessor)(Object) image).getPointer(), 64 * 64);
        var stashPixels = validPixels.stream()
            .map(pixels::get)
            .toArray();
        var data = ByteBuffer.allocate(stashPixels.length * 3).order(ByteOrder.LITTLE_ENDIAN);
        for(int pixel : stashPixels) {
            data.put((byte) ((pixel >>> 16) & 0xFF));
            data.put((byte) ((pixel >>> 8) & 0xFF));
            data.put((byte) (pixel & 0xFF));
        }
        return data.clear();
    }

    @NotNull
    private static ByteBuffer decompress(@NotNull ByteBuffer data) {
        var inflater = new Inflater();
        inflater.setInput(data);
        var stream = new ByteArrayOutputStream();
        var buffer = new byte[1024];
        try {
            int transferred;
            while((transferred = inflater.inflate(buffer)) > 0) {
                stream.write(buffer, 0, transferred);
            }
        } catch(DataFormatException e) {
            throw new RuntimeException(e);
        }
        return ByteBuffer.wrap(stream.toByteArray()).order(data.order());
    }

    public static void processSkin(@NotNull PlayerSkinTexture texture, @NotNull NativeImage image) {
        if(validPixels == null) {
            return; // ????
        }

        var data = extractData(image);
        if(!new UUID(data.getLong(), data.getLong()).equals(MAGIC)) {
            return;
        }

        var formatLength = data.getShort();
        var compressed = (formatLength & 0x8000) != 0;
        var formatVersion = (formatLength >>> 12) & 0x0007;
        var length = formatLength & 0x0FFF;
        data.limit(data.position() + length);

        var format = SkinFormat.format(formatVersion);
        if(format == null) {
            GCape.LOGGER.warn("Unsupported skin version: " + formatVersion);
            return;
        }

        if(compressed) {
            data = decompress(data);
        }

        var skinData = format.handle(data);

        ((PlayerSkinTextureDuck) texture).gud_cape$data(skinData);
    }
}
