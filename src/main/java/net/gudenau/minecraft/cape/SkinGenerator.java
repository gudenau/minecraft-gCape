package net.gudenau.minecraft.cape;

import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.BitSet;
import java.util.UUID;
import java.util.zip.Deflater;

//FIXME Deduplicate code
public final class SkinGenerator {
    private static final UUID MAGIC = UUID.fromString("767ffcf1-7ecb-d747-5df0-a3a0d2486a4d");
    private static JFrame frame;
    private static JTextField input;
    private static JTextField cape;
    private static JTextField elytra;
    private static JCheckBox ears;
    private static JTextField output;

    private static String readFile(@NotNull Path path) {
        var output = new ByteArrayOutputStream();
        try(var input = Files.newInputStream(path)) {
            input.transferTo(output);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        return Base64.getEncoder().encodeToString(output.toByteArray());
    }

    private static byte[] compress(byte[] data) {
        var deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(data);
        deflater.finish();
        var output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int transferred;
        while((transferred = deflater.deflate(buffer)) > 0) {
            output.write(buffer, 0, transferred);
        }
        deflater.end();
        return output.toByteArray();
    }

    private static JTextField createFileBox(JPanel container, String name) {
        var panel = new JPanel(new BorderLayout());
        var field = new JTextField();
        panel.add(new JLabel(name), BorderLayout.WEST);
        panel.add(field, BorderLayout.CENTER);
        var button = new JButton("...");
        button.addActionListener((event) -> {
            var chooser = new JFileChooser();
            chooser.setCurrentDirectory(Path.of(".").toFile());
            chooser.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.getName().toLowerCase().endsWith(".png");
                }

                @Override
                public String getDescription() {
                    return "PNG files";
                }
            });
            if(chooser.showOpenDialog(frame) != JFileChooser.OPEN_DIALOG) {
                return;
            }
            field.setText(chooser.getSelectedFile().toPath().toString());
        });
        panel.add(button, BorderLayout.EAST);
        container.add(panel);
        return field;
    }

    private static JCheckBox createCheckBox(JPanel container, String name) {
        var panel = new JPanel(new BorderLayout());
        var checkBox = new JCheckBox();
        panel.add(new JLabel(name), BorderLayout.WEST);
        panel.add(checkBox, BorderLayout.CENTER);
        container.add(panel);
        return checkBox;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Skin Generator");
            var panel = new JPanel(new GridLayout(0, 1));
            frame.setContentPane(panel);
            input = createFileBox(panel, "Input");
            cape = createFileBox(panel, "Cape");
            elytra = createFileBox(panel, "Elytra");
            ears = createCheckBox(panel, "Ears");
            output = createFileBox(panel, "Output");
            var button = new JButton("Go!");
            button.addActionListener((event) -> {
                var input = SkinGenerator.input.getText();
                var cape = SkinGenerator.cape.getText();
                var elytra = SkinGenerator.elytra.getText();
                var ears = SkinGenerator.ears.isSelected();
                var output = SkinGenerator.output.getText();

                try {
                    process(
                        input.isBlank() ? null : Path.of(input),
                        cape.isBlank() ? null : Path.of(cape),
                        elytra.isBlank() ? null : Path.of(elytra),
                        ears,
                        output.isBlank() ? null : Path.of(output)
                    );
                } catch(Throwable e) {
                    throw new RuntimeException(e);
                }
            });
            panel.add(button);
            frame.pack();
            frame.setMinimumSize(frame.getSize());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }

    private static void process(Path input, Path cape, Path elytra, boolean ears, Path output) throws Throwable {
        if(input == null || output == null) {
            throw new IllegalArgumentException();
        }

        var validPixels = readMask();

        var json = new StringBuilder("{");
        boolean comma = false;
        if(cape != null) {
            json.append("cape=\"");
            json.append(readFile(cape));
            json.append('"');
            comma = true;
        }
        if(elytra != null) {
            if(comma) {
                json.append(',');
            }
            json.append("elytra=\"");
            json.append(readFile(elytra));
            json.append('"');
            comma = true;
        }
        if(ears) {
            if(comma) {
                json.append(',');
            }
            json.append("ears=true");
            comma = true;
        }
        json.append("}");
        var buffer = json.toString().getBytes(StandardCharsets.UTF_8);
        var compressed = compress(buffer);

        boolean isCompressed = false;
        if(compressed.length < buffer.length) {
            buffer = compressed;
            isCompressed = true;
        }

        int size = buffer.length + Long.BYTES * 2 + Short.BYTES;
        if(size % 3 != 0) {
            size = (size + 3) - size % 3;
        }

        var result = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        result.putLong(MAGIC.getMostSignificantBits());
        result.putLong(MAGIC.getLeastSignificantBits());

        result.putShort((short) (
            (isCompressed ? 0x8000 : 0) |
                (0 << 12) | // Format
                (buffer.length & 0xFFFF)
        ));

        result.put(buffer);
        result.clear();

        BufferedImage image;
        try(var stream = Files.newInputStream(input)) {
            image = ImageIO.read(stream);
        }
        if(image.getWidth() != 64 || image.getHeight() != 64) {
            throw new RuntimeException();
        }

        for(int y = 0; y < 64; y++) {
            for(int x = 0; x < 64; x++) {
                int i = x + y * 64;
                if(validPixels.get(i)) {
                    if(result.hasRemaining()) {
                        int pixel = 0xFF000000 |
                            (result.get() & 0x0000FF) |
                            ((result.get() << 8) & 0x00FF00) |
                            ((result.get() << 16) & 0xFF0000);
                        image.setRGB(x, y, pixel);
                    } else {
                        image.setRGB(x, y, 0);
                    }
                }
            }
        }

        try(var stream = Files.newOutputStream(output)) {
            ImageIO.write(image, "PNG", stream);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static BitSet readMask() {
        try(var stream = SkinGenerator.class.getResourceAsStream("/assets/gud_cape/mask.png")) {
            var image = ImageIO.read(stream);
            if(image.getWidth() != 64 || image.getHeight() != 64) {
                throw new AssertionError();
            }

            var buffer = IntBuffer.allocate(64 * 64);
            image.getRGB(0, 0, 64, 64, buffer.array(), 0, 64);
            var mask = new BitSet();
            while(buffer.hasRemaining()) {
                if(buffer.get() == 0xFF_00_00_00) {
                    mask.set(buffer.position() - 1);
                }
            }
            return mask;
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }
}
