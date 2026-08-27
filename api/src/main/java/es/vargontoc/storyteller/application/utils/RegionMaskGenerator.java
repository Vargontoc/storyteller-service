package es.vargontoc.storyteller.application.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import javax.imageio.ImageIO;

public class RegionMaskGenerator {
    
    public static Path[] generateMasks(int width, int height, int feather, Path outputDir) throws IOException {
        int mid = width / 2;

        BufferedImage left = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        BufferedImage right = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for(int x = 0; x < width; x++) {
            int leftValue;
            if(x < mid - feather)
                leftValue = 255;
            else if(x >= mid + feather)
                leftValue = 0;
            else {
                double t = (double)(x - (mid - feather)) / (2.0 * feather);
                leftValue = (int) Math.round(255 *  (1 - t));
            }
            int rigtValue = 255 - leftValue;

            for(int y = 0; y < height; y++) {
                left.getRaster().setSample(x, y, 0, leftValue);
                right.getRaster().setSample(x, y, 0, rigtValue);
            }
        }

        Path leftPath = outputDir.resolve("mask_left.png");
        Path rightPath = outputDir.resolve("mask_right.png");
        ImageIO.write(left, "png", leftPath.toFile());
        ImageIO.write(right, "png", rightPath.toFile());
        
        return new Path[]{ leftPath, rightPath};
    }
}
