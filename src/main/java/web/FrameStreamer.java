package web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Sends what a frame shows to the browser. Every tick the frame's root pane is painted into an image at a scale set by
 * the session (the browser's device pixel ratio, times the scale it shows the frame at); the image is split into
 * square tiles, and only the tiles that changed since the last tick are sent, each as a PNG.
 * <p>
 * Each frame is one binary message: a 4-byte big-endian length, a JSON header of that length
 * ({@code {seq, w, h, fw, fh, tiles: [{x, y, w, h, len}]}}), then the tiles' PNG bytes in header order. The image and
 * tiles are in device pixels; fw and fh are the frame's own size, in the frame's (CSS) pixels, which may be larger than
 * the browser's space for it (the browser then scales it down).
 */
class FrameStreamer {

    static final int TILE = 128;
    static final long TICK_MS = 100;

    private final JFrame frame;
    private final Sender out;
    private final ScheduledExecutorService ticker = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "web-frames");
        t.setDaemon(true);
        return t;
    });
    private volatile double scale;
    private int[] previous;
    private int previousW, previousH;
    private long seq;
    private Dimension frameSize;

    FrameStreamer(JFrame frame, Sender out, double scale) {
        this.frame = frame;
        this.out = out;
        this.scale = scale;
    }

    void start() {
        ticker.scheduleWithFixedDelay(this::tick, 0, TICK_MS, TimeUnit.MILLISECONDS);
    }

    void stop() {
        ticker.shutdownNow();
    }

    /**
     * Image pixels per frame pixel.
     */
    void setScale(double scale) {
        this.scale = scale;
    }

    private void tick() {
        if (!out.isOpen()) return;
        try {
            BufferedImage image = paint();
            if (image != null) send(image);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // a failed tick is retried on the next one; it must not end the schedule
            e.printStackTrace();
        }
    }

    private BufferedImage paint() throws Exception {
        BufferedImage[] result = new BufferedImage[1];
        Dimension[] size = new Dimension[1];
        double scale = this.scale;
        SwingUtilities.invokeAndWait(() -> {
            JRootPane root = frame.getRootPane();
            size[0] = root.getSize();
            int w = (int) Math.ceil(root.getWidth() * scale), h = (int) Math.ceil(root.getHeight() * scale);
            if (w <= 0 || h <= 0) return;
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.scale(scale, scale);
            root.paint(g);
            g.dispose();
            result[0] = image;
        });
        frameSize = size[0];
        return result[0];
    }

    private void send(BufferedImage image) throws IOException {
        int w = image.getWidth(), h = image.getHeight();
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        boolean full = previous == null || w != previousW || h != previousH;

        List<Rectangle> changed = new ArrayList<>();
        for (int y = 0; y < h; y += TILE)
            for (int x = 0; x < w; x += TILE) {
                Rectangle tile = new Rectangle(x, y, Math.min(TILE, w - x), Math.min(TILE, h - y));
                if (full || tileChanged(pixels, tile, w))
                    changed.add(tile);
            }
        previous = pixels;
        previousW = w;
        previousH = h;
        if (changed.isEmpty()) return;

        List<byte[]> pngs = changed.parallelStream()
                .map(t -> png(image.getSubimage(t.x, t.y, t.width, t.height)))
                .toList();

        JsonObject header = new JsonObject();
        header.addProperty("seq", ++seq);
        header.addProperty("w", w);
        header.addProperty("h", h);
        header.addProperty("fw", frameSize.width);
        header.addProperty("fh", frameSize.height);
        JsonArray tiles = new JsonArray();
        int total = 0;
        for (int i = 0; i < changed.size(); i++) {
            Rectangle t = changed.get(i);
            JsonObject tile = new JsonObject();
            tile.addProperty("x", t.x);
            tile.addProperty("y", t.y);
            tile.addProperty("w", t.width);
            tile.addProperty("h", t.height);
            tile.addProperty("len", pngs.get(i).length);
            tiles.add(tile);
            total += pngs.get(i).length;
        }
        header.add("tiles", tiles);
        byte[] headerBytes = header.toString().getBytes(StandardCharsets.UTF_8);

        ByteBuffer message = ByteBuffer.allocate(4 + headerBytes.length + total);
        message.putInt(headerBytes.length).put(headerBytes);
        for (byte[] png : pngs) message.put(png);
        message.flip();
        out.sendBinary(message);
    }

    private boolean tileChanged(int[] pixels, Rectangle tile, int w) {
        for (int row = tile.y; row < tile.y + tile.height; row++) {
            int from = row * w + tile.x, to = from + tile.width;
            if (!Arrays.equals(pixels, from, to, previous, from, to))
                return true;
        }
        return false;
    }

    private static byte[] png(BufferedImage tile) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(tile, "png", bytes);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
