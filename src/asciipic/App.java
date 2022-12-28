package asciipic;

import javax.imageio.ImageIO;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.rwtodd.args.*;

class App {
    static BufferedImage scaleImage(BufferedImage orig, int width, int height) {
        var scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var gfx2d = scaled.createGraphics();
        gfx2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        gfx2d.drawImage(orig, 0, 0, width, height, null);
        gfx2d.dispose();
        return scaled;
    }

    static BufferedImage thumbnail(String fname, int scaledWidth, double aspectRatio) throws IOException {
        var image = ImageIO.read(new File(fname));
        var scaledHeight = (int) ((scaledWidth / aspectRatio / image.getWidth()) * image.getHeight());

        while (scaledWidth < image.getWidth()/2) {
            image = scaleImage(image, image.getWidth()/2, image.getHeight()/2);
        }

        return scaleImage(image, scaledWidth, scaledHeight);
    }


    // determine the brightness (0.0 to 255.0) of a Color
    private static double brightness(Color c) { return c.getRed() * 0.2126 + c.getGreen() * 0.7152 + c.getBlue() * 0.0722; }

    // select a character to use based on the given brightness
    private static char[] chars = "#A@%$+=*:,. ".toCharArray();

    private static char selectChar(Double b) { return chars[(int) (b * chars.length / 256.0)]; }

    // convert an entire image from RGB to ascii
    private static String convertImage(BufferedImage im) {
        StringBuilder sb = new StringBuilder((im.getWidth() + 1) * im.getHeight());
        for(int y = 0; y < im.getHeight(); ++y) {
          for(int x = 0; x < im.getWidth(); ++x) {
            sb.append(selectChar(brightness(new Color(im.getRGB(x, y)))));
          }
          sb.append("\n");
        }
        return sb.toString();
    }

    static void usage(Parser p) {
        System.err.println("Usage: asciipic [optons] fname\n");
        p.printHelpText(System.err);
        System.exit(1);
    }

    public static void main(String[] args) {
        final var fpath = new ExistingFileParam(List.of("fname", "f"), "<File Name>the image to convert");
        final var width = new ClampedParam<>(new IntParam(List.of("width", "w"), 72, "the width of the output (default 72)"), 1, 500);
        final var ar = new ClampedParam<>(new DoubleParam(List.of("aspect", "a"), 1.5d, "the aspect ratio of the output font (default 1.5)"), 0.1d, 5.0d);
        final var rev = new FlagParam(List.of("invert"),"invert the brightness scale (helps with some image-terminal combinations");
        final var help = new FlagParam(List.of("help"), "print this help text");
        final var p = new Parser(fpath, width, ar, rev, help);
        try {
            var extras = p.parse(args);
            if (fpath.getValue() == null && extras.size() == 1) {
                fpath.process("fname", extras.get(0));
            }
            if(help.getValue() || fpath.getValue() == null) { usage(p); }
            if(rev.getValue()) {
                char[] tmp = new char[chars.length];
                for(int i = 0; i < chars.length; ++i)  tmp[chars.length - i - 1] = chars[i];
                chars = tmp;
            }
            System.out.println(convertImage(thumbnail(fpath.getValue().toString(), width.getValue(), ar.getValue())));
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage() + "\n");
            usage(p);
        }
    }
}
