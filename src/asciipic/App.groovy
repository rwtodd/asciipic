package asciipic

import groovy.transform.CompileStatic as CS
import javax.imageio.ImageIO
import java.awt.Color
import java.awt.RenderingHints
import org.rwtodd.args.*
import java.awt.image.BufferedImage


@CS
class App {
    static BufferedImage scaleImage(BufferedImage orig, int width, int height) {
        var scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        var gfx2d = scaled.createGraphics()
        gfx2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        gfx2d.drawImage(orig, 0, 0, width, height, null)
        gfx2d.dispose()
        scaled
    }

    static BufferedImage thumbnail(String fname, int scaledWidth, double aspectRatio) {
        var image = ImageIO.read(new File(fname))
        var scaledHeight = (int) ((scaledWidth / aspectRatio / image.width) * image.height)

        while (scaledWidth < image.width / 2) {
            image = scaleImage(image, (int) (image.width / 2), (int) (image.height / 2))
        }

        scaleImage(image, scaledWidth, scaledHeight)
    }


    // determine the brightness (0.0 to 255.0) of a Color
    private static double brightness(Color c) { c.red * 0.2126d + c.green * 0.7152d + c.blue * 0.0722d }

    // select a character to use based on the given brightness
    private static char[] chars = '#A@%$+=*:,. '.toCharArray()

    private static char selectChar(Double b) { chars[(int) (b * chars.length / 256.0d)] }

    // convert an entire image from RGB to ascii
    private static String convertImage(BufferedImage im) {
        StringBuilder sb = new StringBuilder((im.width + 1) * im.height)
        (0..<im.height).each { y ->
            (0..<im.width).each { x ->
                sb.append(selectChar(brightness(new Color(im.getRGB(x, y)))))
            }
            sb.append('\n')
        }
        sb.toString()
    }

    static void usage(Parser p) {
        System.err.println("Usage: asciipic [optons] fname\n")
        p.printHelpText(System.err)
        System.exit(1)
    }

    @groovy.transform.CompileDynamic
    static void main(String[] args) {
        final var fpath = new ExistingFileParam(['fname', 'f'], '<File Name>the image to convert')
        final var width = new ClampedParam(new IntParam(['width', 'w'], 72, 'the width of the output (default 72)'), 1, 500)
        final var ar = new ClampedParam(new DoubleParam(['aspect', 'a'], 1.5d, 'the aspect ratio of the output font (default 1.5)'), 0.1d, 5.0d)
        final var rev = new FlagParam(['invert'],'invert the brightness scale (helps with some image-terminal combinations')
        final var help = new FlagParam(['help'], 'print this help text')
        Parser p = [fpath, width, ar, rev, help]
        try {
            var extras = p.parse(args)
            if(help.value) { usage(p) }

            if (fpath.value == null && extras.size() == 1) {
                fpath.process('fname', extras[0])
            }
            if(rev.value) {
                char[] tmp = new char[chars.length];
                chars.eachWithIndex{ char ch, int i -> tmp[chars.length - i - 1] = ch }
                chars = tmp
            }
            println(convertImage(thumbnail(fpath.value.toString(), width.value, ar.value)))
        } catch (Exception e) {
            System.err.println("Error: ${e.message}\n")
            usage(p)
        }
    }
}
