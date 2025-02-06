package utilities;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;

public class ImageIO {

    private static ImageIO imageIO = null;
    private static HashMap<String, Image> images;

    public static ImageIO GetInstance() {
        if (imageIO == null) {
            imageIO = new ImageIO();
        }
        return imageIO;
    }

    private ImageIO()
    {
        images = new HashMap<>();
    }

    public Image getImage(String image_file) {
        return getImage(image_file, null, null);
    }

    public Image getImage(String image_file, Color replaceColorFrom, Color replaceColorTo) {
        String imageName = image_file;

        if (replaceColorFrom != null && replaceColorTo != null) {
            imageName = imageName + replaceColorFrom.getRGB() + replaceColorTo.getRGB();
        }

        if (images.containsKey(imageName)) {
            return images.get(imageName);
        }

        try {
            if ((new File(image_file).exists())) {
                BufferedImage g = javax.imageio.ImageIO.read(new File(image_file));
                if (replaceColorFrom != null && replaceColorTo != null) {
                    replaceColor(g, replaceColorFrom, replaceColorTo);
                }
                images.put(imageName, g);
                return g;
            }
        } catch (Exception ignored) {}

        return null;
    }

    public static void replaceColor(BufferedImage image, Color targetColor, Color replacementColor) {
        int width = image.getWidth();
        int height = image.getHeight();

        int targetRGB = targetColor.getRGB() & 0xFFFFFF; // Ignore alpha for matching

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = image.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xFF; // Extract original alpha

                if ((pixel & 0xFFFFFF) == targetRGB) { // Compare only RGB
                    int newRGB = (alpha << 24) | (replacementColor.getRGB() & 0xFFFFFF); // Keep original alpha
                    image.setRGB(x, y, newRGB);
                }
            }
        }
    }


}