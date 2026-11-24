package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class ImageContentInspectorTest {

    private final ImageContentInspector inspector = new ImageContentInspector();

    @Test
    void png_300x200_isImage() throws IOException {
        assertTrue(inspector.isImage(encode(300, 200, "png")));
    }

    @Test
    void largeJpeg_isImage() throws IOException {
        // Más de 20 MB: JPEG válido con relleno final tras el marcador EOI.
        byte[] jpeg = encode(64, 64, "jpg");
        byte[] big = new byte[21 * 1024 * 1024];
        System.arraycopy(jpeg, 0, big, 0, jpeg.length);
        assertTrue(inspector.isImage(big));
    }

    @Test
    void bmp_isImage() throws IOException {
        assertTrue(inspector.isImage(encode(10, 10, "bmp")));
    }

    @Test
    void webp_isImage() {
        byte[] webp = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);
        assertTrue(inspector.isImage(webp));
    }

    @Test
    void text_isNotImage() {
        assertFalse(inspector.isImage("hola, esto no es una imagen".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void textDisguisedAsPng_isNotImage() {
        // El nombre y el tipo declarado no intervienen: solo cuentan los bytes.
        assertFalse(inspector.isImage("texto con extension .png".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void pngWithOtherExtension_isImage() throws IOException {
        assertTrue(inspector.isImage(encode(5, 5, "png")));
    }

    @Test
    void emptyOrNull_isNotImage() {
        assertFalse(inspector.isImage(new byte[0]));
        assertFalse(inspector.isImage(null));
    }

    @Test
    void identify_pngJpegBmp() throws IOException {
        assertEquals(new ImageContentInspector.ImageFormat("PNG", ".png"), inspector.identify(encode(5, 5, "png")).orElseThrow());
        assertEquals(new ImageContentInspector.ImageFormat("JPEG", ".jpg"), inspector.identify(encode(5, 5, "jpg")).orElseThrow());
        assertEquals(new ImageContentInspector.ImageFormat("BMP", ".bmp"), inspector.identify(encode(5, 5, "bmp")).orElseThrow());
    }

    @Test
    void identify_ignoresDeclaredNameAndType() throws IOException {
        // Los bytes son PNG aunque el fichero se llame .jpg y declare image/jpeg: solo cuenta el contenido.
        byte[] pngNamedJpg = encode(5, 5, "png");
        assertEquals(".png", inspector.identify(pngNamedJpg).orElseThrow().extension());
    }

    @Test
    void identify_webpAndGif() throws IOException {
        byte[] webp = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);
        assertEquals(".webp", inspector.identify(webp).orElseThrow().extension());
        assertEquals(".gif", inspector.identify(encode(5, 5, "gif")).orElseThrow().extension());
    }

    @Test
    void everyRecognizedFormatHasExtension() throws IOException {
        for (String format : new String[] { "png", "jpg", "bmp", "gif" }) {
            var found = inspector.identify(encode(5, 5, format)).orElseThrow();
            assertFalse(found.extension().isBlank());
            assertTrue(found.extension().startsWith("."));
        }
    }

    @Test
    void identify_textOrEmpty_isEmpty() {
        assertTrue(inspector.identify("texto con extension .png".getBytes(StandardCharsets.UTF_8)).isEmpty());
        assertTrue(inspector.identify(new byte[0]).isEmpty());
        assertTrue(inspector.identify(null).isEmpty());
    }

    @Test
    void identify_doesNotModifyBytes() throws IOException {
        byte[] png = encode(8, 8, "png");
        byte[] copy = png.clone();
        inspector.identify(png);
        assertArrayEquals(copy, png);
    }

    private byte[] encode(int w, int h, String format) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, format, out);
        return out.toByteArray();
    }
}
