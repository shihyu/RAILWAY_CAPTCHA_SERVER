
package com.bk.railway.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.imageio.ImageIO;

public class CaptchaResolver {
    protected final static int RANGE = 60;
    protected final static int FONTSIZE = 26;
    protected static CaptchaResolver m_self;

    protected final BufferedImage[][] m_comparsionImages = new BufferedImage[10][(RANGE << 1) + 1];
    protected final int[][][][] m_accImages = new int[10][(RANGE << 1) + 1][][];

    public synchronized static CaptchaResolver getInstance() throws Exception {
        if (null == m_self) {
            m_self = new CaptchaResolver();
        }

        return m_self;
    }

    protected CaptchaResolver() throws Exception {
        prepare();
    }

    class GrayPix {
        int color;
        int volume;
    }

    class GrayPixComparator implements Comparator<GrayPix> {

        @Override
        public int compare(GrayPix a, GrayPix b) {
            return a.volume - b.volume;
        }

    }

    public String getCaptcha(BufferedImage rawImg) throws Exception {
        final BufferedImage grayRawImage = toGray(rawImg);
        final int[] hist = histogram(grayRawImage);
        final GrayPix[] pixHist = new GrayPix[hist.length];
        for (int i = 0; i < hist.length; i++) {
            pixHist[i] = new GrayPix();
            pixHist[i].color = i;
            pixHist[i].volume = hist[i];
        }

        Arrays.sort(pixHist, new GrayPixComparator());
        final Set<Integer> reserverColor = new HashSet<Integer>();
        for (int i = 0; i < pixHist.length; i++) {
            if (pixHist[i].volume < 200 && pixHist[i].color < 128) {
                reserverColor.add(new Color(pixHist[i].color, pixHist[i].color,
                        pixHist[i].color).getRGB());
                // System.out.println("color reserve " + pixHist[i].color + " "
                // + pixHist[i].volume);
            }
            // System.out.println("color " + pixHist[i].color + " " +
            // pixHist[i].volume);
        }

        final BufferedImage binaryRawImage = toBinaryImage(removedColorImage(grayRawImage,
                reserverColor, Color.BLACK));
        final Graphics2D g = binaryRawImage.createGraphics();
        final HashMap<Rectangle, Float> totalEntry = new HashMap<Rectangle, Float>();
        final HashMap<Rectangle, Integer> totalIntegerEntry = new HashMap<Rectangle, Integer>();
        final int[][] verfImgAccArrays = toAccArrays(binaryRawImage);

        g.setColor(Color.WHITE);

        for (int v = 0; v < 10; v++) {
            System.out.print(v);
            for (Map.Entry<Rectangle, Float> entry : findNumber(v, binaryRawImage, verfImgAccArrays)
                    .entrySet()) {
                totalIntegerEntry.put(entry.getKey(), v);
                joinToRectangle(totalEntry, entry.getKey(), entry.getValue(), 5);
            }
        }
        System.out.println();

        final TreeMap<Integer, Rectangle> sortedRectangle = new TreeMap<Integer, Rectangle>();
        final StringBuffer answerSB = new StringBuffer();
        for (Map.Entry<Rectangle, Float> entry : totalEntry.entrySet()) {
            sortedRectangle.put(entry.getKey().x, entry.getKey());
            System.out.println("\tv=" + totalIntegerEntry.get(entry.getKey()) + " Rect "
                    + entry.getKey() + "," + entry.getValue());
            g.drawRect(entry.getKey().x, entry.getKey().y, entry.getKey().width,
                    entry.getKey().height);
        }

        for (Map.Entry<Integer, Rectangle> entry : sortedRectangle.entrySet()) {
            answerSB.append(totalIntegerEntry.get(entry.getValue()));
        }
        final String answer = answerSB.toString();
        if (5 == answer.length()) {
            return answer.toString();
        }
        else {
            return null;
        }
    }

    private Map<Rectangle, Float> findNumber(int v, BufferedImage verf_img, int[][] verfImgAccArrays)
            throws IOException {
        final float threshold = 0.6f;
        final HashMap<Rectangle, Float> mapOfRectangle = new HashMap<Rectangle, Float>();

        int debug_reduce = 0;
        int debug_total = 0;

        // final String tempdir =
        // "/Users/bbkb/Documents/workspace/RAILWAY_CAPTCHA/numbers";

        for (int degree = -RANGE; degree <= RANGE; degree++) {

            final BufferedImage rotatedImage = m_comparsionImages[v][degree + RANGE];

            final int[][] rotateImagesAccArrays = m_accImages[v][degree + RANGE];

            final int sumOfRawNumberPix = 1 + calculateSumPixles(rotateImagesAccArrays,
                    new Rectangle(0, 0, rotatedImage.getWidth(), rotatedImage.getHeight()));

            final int sumOfRawTopHalf = 1 + calculateSumPixles(rotateImagesAccArrays,
                    new Rectangle(0, 0, rotatedImage.getWidth(), rotatedImage.getHeight() >> 1));
            final int sumOfRawBottomHalf = 1 + calculateSumPixles(rotateImagesAccArrays,
                    new Rectangle(0, rotatedImage.getHeight() >> 1, rotatedImage.getWidth(),
                            rotatedImage.getHeight() >> 1));
            final int sumOfRawLeftHalf = 1 + calculateSumPixles(rotateImagesAccArrays,
                    new Rectangle(0, 0, rotatedImage.getWidth() >> 1, rotatedImage.getHeight()));
            final int sumOfRawRightHalf = 1 + calculateSumPixles(rotateImagesAccArrays,
                    new Rectangle(rotatedImage.getWidth() >> 1, 0, rotatedImage.getWidth() >> 1,
                            rotatedImage.getHeight()));

            final int sumleftTopOfRawNumberPix = 1 + calculateSumPixles(
                    rotateImagesAccArrays,
                    new Rectangle(0, 0, rotatedImage.getWidth() >> 1, rotatedImage.getHeight() >> 1));
            final int sumleftDownOfRawNumberPix = 1 + calculateSumPixles(rotateImagesAccArrays,
                    new Rectangle(0, rotatedImage.getHeight() >> 1, rotatedImage.getWidth() >> 1,
                            rotatedImage.getHeight() >> 1));
            final int sumrightTopOfRawNumberPix = 1 + calculateSumPixles(rotateImagesAccArrays,
                    new Rectangle(rotatedImage.getWidth() >> 1, 0, rotatedImage.getWidth() >> 1,
                            rotatedImage.getHeight() >> 1));
            final int sumrightDownOfRawNumberPix = 1 + calculateSumPixles(rotateImagesAccArrays,
                    new Rectangle(rotatedImage.getWidth() >> 1, rotatedImage.getHeight(),
                            rotatedImage.getWidth() >> 1, rotatedImage.getHeight() >> 1));

            for (int x = 0; x < verf_img.getWidth() - rotatedImage.getWidth(); x++) {
                for (int y = 0; y < verf_img.getHeight() - rotatedImage.getHeight(); y++) {
                    debug_total++;

                    final int sumOfRectangle = 1 + calculateSumPixles(verfImgAccArrays,
                            new Rectangle(x, y, rotatedImage.getWidth(), rotatedImage.getHeight()));

                    final int sumOfTopHalf = 1 + calculateSumPixles(verfImgAccArrays,
                            new Rectangle(x, y, rotatedImage.getWidth(),
                                    rotatedImage.getHeight() >> 1));
                    final int sumOfBottomHalf = 1 + calculateSumPixles(
                            verfImgAccArrays,
                            new Rectangle(x, y + (rotatedImage.getHeight() >> 1), rotatedImage
                                    .getWidth(), rotatedImage.getHeight() >> 1));
                    final int sumOfLeftHalf = 1 + calculateSumPixles(
                            verfImgAccArrays,
                            new Rectangle(x, y, rotatedImage.getWidth() >> 1, rotatedImage
                                    .getHeight()));
                    final int sumOfRightHalf = 1 + calculateSumPixles(
                            verfImgAccArrays,
                            new Rectangle(x + (rotatedImage.getWidth() >> 1), y, rotatedImage
                                    .getWidth() >> 1, rotatedImage.getHeight()));

                    final int sumleftTopRectangle = 1 + calculateSumPixles(
                            verfImgAccArrays,
                            new Rectangle(x, y, rotatedImage.getWidth() >> 1, rotatedImage
                                    .getHeight() >> 1));
                    final int sumleftDownRectangle = 1 + calculateSumPixles(
                            verfImgAccArrays,
                            new Rectangle(x, y + (rotatedImage.getHeight() >> 1), rotatedImage
                                    .getWidth() >> 1, rotatedImage.getHeight() >> 1));
                    final int sumrightTopRectangle = 1 + calculateSumPixles(
                            verfImgAccArrays,
                            new Rectangle(x + (rotatedImage.getWidth() >> 1), y, rotatedImage
                                    .getWidth() >> 1, rotatedImage.getHeight() >> 1));
                    final int sumrightDownRectangle = 1 + calculateSumPixles(
                            verfImgAccArrays,
                            new Rectangle(x + (rotatedImage.getWidth() >> 1), y
                                    + (rotatedImage.getHeight() >> 1),
                                    rotatedImage.getWidth() >> 1, rotatedImage.getHeight() >> 1));

                    if ((float) sumOfRectangle / (float) sumOfRawNumberPix > threshold
                            &&
                            (float) sumOfTopHalf / (float) sumOfRawTopHalf > threshold
                            &&
                            (float) sumOfBottomHalf / (float) sumOfRawBottomHalf > threshold
                            &&
                            (float) sumOfLeftHalf / (float) sumOfRawLeftHalf > threshold
                            &&
                            (float) sumOfRightHalf / (float) sumOfRawRightHalf > threshold
                            &&
                            (float) sumleftTopRectangle / (float) sumleftTopOfRawNumberPix > threshold
                            &&
                            (float) sumleftDownRectangle / (float) sumleftDownOfRawNumberPix > threshold
                            &&
                            (float) sumrightTopRectangle / (float) sumrightTopOfRawNumberPix > threshold
                            &&
                            (float) sumrightDownRectangle / (float) sumrightDownOfRawNumberPix > threshold) {

                        float score = score(verf_img, rotatedImage, x, y);

                        if (score > threshold) {
                            joinToRectangle(
                                    mapOfRectangle,
                                    new Rectangle(x, y, rotatedImage.getWidth(), rotatedImage
                                            .getHeight()), score, 5);
                        }
                    }
                    else {
                        debug_reduce++;
                    }

                }
            }

        }
        // System.out.println("redeuce=" + (float)debug_reduce / (float)
        // debug_total);

        return mapOfRectangle;
    }

    private int[][] toAccArrays(BufferedImage img) {
        int[][] accArrays = new int[img.getWidth()][img.getHeight()];
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {

                final int left = 0 == x ? 0 : accArrays[x - 1][y];
                final int top = 0 == y ? 0 : accArrays[x][y - 1];
                final int lefttop = 0 == x || 0 == y ? 0 : accArrays[x - 1][y - 1];

                accArrays[x][y] = left + top - lefttop
                        + (img.getRGB(x, y) != Color.BLACK.getRGB() ? 1 : 0);

            }
        }
        return accArrays;
    }

    private int calculateSumPixles(int[][] accArrays, Rectangle r) {
        int startX = r.x < 0 ? 0 : r.x;
        int startY = r.y < 0 ? 0 : r.y;
        int endX = r.x + r.width - 1 >= accArrays.length ? accArrays.length - 1 : r.x + r.width - 1;
        int endY = r.y + r.height - 1 >= accArrays[0].length ? accArrays[0].length - 1 : r.y
                + r.height - 1;

        return accArrays[endX][endY] - (0 == startY ? 0 : accArrays[endX][startY - 1])
                - (0 == startX ? 0 : accArrays[startX - 1][endY])
                + (0 == startX || 0 == startY ? 0 : accArrays[startX - 1][startY - 1]);
    }

    private float score(BufferedImage verf_img, BufferedImage rotateImage, int x, int y) {
        float score = 0.0f;
        for (int rx = 0; rx < rotateImage.getWidth(); rx++) {
            for (int ry = 0; ry < rotateImage.getHeight(); ry++) {
                final int a = verf_img.getRGB(x + rx, y + ry);
                final int b = rotateImage.getRGB(rx, ry);

                score += a == b ? 1 : -1;
            }
        }
        return score / (float) (rotateImage.getWidth() * rotateImage.getHeight());
    }

    private BufferedImage toBinaryImage(BufferedImage img) {
        final BufferedImage newimg = new BufferedImage(img.getWidth(), img.getHeight(),
                BufferedImage.TYPE_BYTE_BINARY);
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                if (img.getRGB(x, y) != Color.BLACK.getRGB()) {
                    newimg.setRGB(x, y, Color.WHITE.getRGB());
                }
                else {
                    newimg.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }

        return newimg;
    }

    private BufferedImage toGray(BufferedImage rawImg) {

        for (int x = 0; x < rawImg.getWidth(); x++) {
            for (int y = 0; y < rawImg.getHeight(); y++) {
                final Color color = new Color(rawImg.getRGB(x, y));
                final int grayColorValue = (color.getBlue() + color.getRed() + color.getGreen()) / 3;

                rawImg.setRGB(x, y,
                        new Color(grayColorValue, grayColorValue, grayColorValue).getRGB());
            }
        }

        return rawImg;
    }

    private int[] histogram(BufferedImage grayImg) {
        final int[] hist = new int[256];
        for (int x = 0; x < grayImg.getWidth(); x++) {
            for (int y = 0; y < grayImg.getHeight(); y++) {
                final Color color = new Color(grayImg.getRGB(x, y));
                hist[color.getBlue()]++;
            }
        }

        return hist;
    }

    private BufferedImage removedColorImage(BufferedImage img, Set<Integer> reserverColor,
            Color replaceColor) {
        if (null == replaceColor) {
            replaceColor = Color.BLACK;
        }
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                if (!reserverColor.contains(img.getRGB(x, y))) {
                    img.setRGB(x, y, replaceColor.getRGB());
                }
                else {
                    // img.setRGB(x, y, Color.WHITE.getRGB());
                }
            }
        }
        return img;
    }

    private boolean joinToRectangle(HashMap<Rectangle, Float> map, Rectangle r, float score,
            int maxNumbrOfElement) {
        Rectangle replaceKey = null;
        float minScore = Float.MAX_VALUE;
        Rectangle minScoreRectangle = null;

        for (Map.Entry<Rectangle, Float> entry : map.entrySet()) {
            if (entry.getValue() < minScore) {
                minScore = entry.getValue();
                minScoreRectangle = entry.getKey();
            }

            if (entry.getKey().intersects(r)) {
                if (score > entry.getValue()) {
                    replaceKey = entry.getKey();
                    break;
                }
                else {
                    return false;
                }
            }
        }
        if (null == replaceKey) {
            if (map.size() >= maxNumbrOfElement) {
                if (score > minScore) {
                    map.remove(minScoreRectangle);
                    map.put(r, score);
                    return true;
                }
                else {
                    return false;
                }
            }
            else {
                map.put(r, score);
                return true;
            }

        }
        else {
            map.remove(replaceKey);
            map.put(r, score);
            return true;
        }

    }

    private void prepare() throws Exception {

        for (int v = 0; v < 10; v++) {
            int c = 0;
            for (int degree = -RANGE; degree <= RANGE; degree++, c++) {
                InputStream imageIn = null;
                try {
                    imageIn = getClass().getResourceAsStream(
                            String.valueOf(v) + "_" + String.valueOf(degree) + ".png");
                    m_comparsionImages[v][c] = ImageIO.read(imageIn);
                    m_accImages[v][c] = toAccArrays(m_comparsionImages[v][c]);
                } finally {
                    if (imageIn != null) {
                        imageIn.close();
                    }
                }
            }
        }
    }
}
