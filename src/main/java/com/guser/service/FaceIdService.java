package com.guser.service;

import com.github.sarxos.webcam.Webcam;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;

public class FaceIdService {

    private static final int FACE_SIZE = 96;
    private static final double MATCH_THRESHOLD = 0.90;

    public String captureTemplate() throws IOException {
        BufferedImage frame = captureFromCamera();
        BufferedImage square = cropCenterSquare(frame);
        BufferedImage grayscale = toGrayscale(square, FACE_SIZE, FACE_SIZE);
        float[] histogram = buildLbpHistogram(grayscale);
        return encodeHistogram(histogram);
    }

    public double compareTemplates(String liveTemplate, String enrolledTemplate) {
        if (liveTemplate == null || enrolledTemplate == null
                || liveTemplate.isBlank() || enrolledTemplate.isBlank()) {
            return 0.0;
        }

        float[] left = decodeHistogram(liveTemplate);
        float[] right = decodeHistogram(enrolledTemplate);
        if (left.length != right.length) {
            return 0.0;
        }

        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }

        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    public double getMatchThreshold() {
        return MATCH_THRESHOLD;
    }

    private BufferedImage captureFromCamera() throws IOException {
        Webcam webcam = Webcam.getDefault();
        if (webcam == null) {
            throw new IOException("Aucune camera detectee.");
        }

        try {
            webcam.open();
            BufferedImage image = webcam.getImage();
            if (image == null) {
                throw new IOException("Capture visage impossible.");
            }
            return image;
        } finally {
            if (webcam.isOpen()) {
                webcam.close();
            }
        }
    }

    private BufferedImage cropCenterSquare(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int size = Math.min(width, height);
        int x = (width - size) / 2;
        int y = (height - size) / 2;

        BufferedImage square = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = square.createGraphics();
        graphics.drawImage(image, 0, 0, size, size, x, y, x + size, y + size, null);
        graphics.dispose();
        return square;
    }

    private BufferedImage toGrayscale(BufferedImage image, int width, int height) {
        BufferedImage grayscale = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = grayscale.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(image, 0, 0, width, height, null);
        graphics.dispose();
        return grayscale;
    }

    private float[] buildLbpHistogram(BufferedImage image) {
        float[] histogram = new float[256];
        Raster raster = image.getRaster();
        int width = image.getWidth();
        int height = image.getHeight();
        int count = 0;

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int center = raster.getSample(x, y, 0);
                int code = 0;
                code |= (raster.getSample(x - 1, y - 1, 0) >= center ? 1 : 0) << 7;
                code |= (raster.getSample(x, y - 1, 0) >= center ? 1 : 0) << 6;
                code |= (raster.getSample(x + 1, y - 1, 0) >= center ? 1 : 0) << 5;
                code |= (raster.getSample(x + 1, y, 0) >= center ? 1 : 0) << 4;
                code |= (raster.getSample(x + 1, y + 1, 0) >= center ? 1 : 0) << 3;
                code |= (raster.getSample(x, y + 1, 0) >= center ? 1 : 0) << 2;
                code |= (raster.getSample(x - 1, y + 1, 0) >= center ? 1 : 0) << 1;
                code |= (raster.getSample(x - 1, y, 0) >= center ? 1 : 0);
                histogram[code] += 1.0f;
                count++;
            }
        }

        if (count > 0) {
            for (int i = 0; i < histogram.length; i++) {
                histogram[i] /= count;
            }
        }
        return histogram;
    }

    private String encodeHistogram(float[] histogram) {
        ByteBuffer buffer = ByteBuffer.allocate(histogram.length * Float.BYTES);
        for (float value : histogram) {
            buffer.putFloat(value);
        }
        return Base64.getEncoder().encodeToString(buffer.array());
    }

    private float[] decodeHistogram(String encodedHistogram) {
        byte[] bytes = Base64.getDecoder().decode(encodedHistogram);
        if (bytes.length % Float.BYTES != 0) {
            return new float[0];
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        float[] histogram = new float[bytes.length / Float.BYTES];
        for (int i = 0; i < histogram.length; i++) {
            histogram[i] = buffer.getFloat();
        }
        return histogram;
    }
}
