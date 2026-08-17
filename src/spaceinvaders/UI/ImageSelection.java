package spaceinvaders.UI;

import spaceinvaders.GameExceptions;
import spaceinvaders.characters.Bullet;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.swing.ImageIcon;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class ImageSelection {
    private static final String LASER_BEAM_IMAGE_PATH = "/resources/sfx/LAZER.gif";
    private static final String BOSS_IMAGE_PATH = "/resources/Invader/Boss.png";
    private Image shooterImage;
    private Image invaderImage;
    private Image laserBeamImage;
    private Image bossImage;
    private final Map<String, Image> resourceImageCache = new HashMap<>();
    private Image bulletImage;
    private List<BufferedImage> bulletGifFrames = Collections.emptyList();
    private long[] bulletGifFrameEndTimesMs = new long[0];
    private long bulletGifDurationMs = 0;
    private Image backgroundImage;
    private Image deathSkinImage;
    private Image deathScreenImage;
    private long deathScreenGifDurationMs = 0;
    private boolean deathSkinFadeOut = true;
    private boolean starsBackgroundEnabled;
    private final StarsBackgroundPainter starsBackgroundPainter = new StarsBackgroundPainter();

    public Image getShooterImage() {
        return shooterImage;
    }

    public Image getInvaderImage() {
        return invaderImage;
    }

    public Image getLaserBeamImage() {
        if (laserBeamImage == null) {
            laserBeamImage = loadImageIfPresent(LASER_BEAM_IMAGE_PATH);
        }
        return laserBeamImage;
    }

    public Image getBossImage() {
        if (bossImage == null) {
            bossImage = loadImageIfPresent(BOSS_IMAGE_PATH);
        }
        return bossImage;
    }

    public Image getImageFromResourcePath(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return null;
        }

        Image cached = resourceImageCache.get(resourcePath);
        if (cached != null) {
            return cached;
        }

        Image loaded = loadImageIfPresent(resourcePath);
        if (loaded != null) {
            resourceImageCache.put(resourcePath, loaded);
        }
        return loaded;
    }

    public Image getBulletImage() {
        return bulletImage;
    }

    public Image getBulletImage(Bullet bullet) {
        if (bullet == null || bulletGifDurationMs <= 0 || bulletGifFrames.isEmpty()) {
            return bulletImage;
        }

        long animationTimeMs = Math.floorMod(System.currentTimeMillis() + bullet.getAnimationOffsetMs(),
                bulletGifDurationMs);
        for (int i = 0; i < bulletGifFrameEndTimesMs.length; i++) {
            if (animationTimeMs < bulletGifFrameEndTimesMs[i]) {
                return bulletGifFrames.get(i);
            }
        }

        return bulletGifFrames.get(bulletGifFrames.size() - 1);
    }

    public Image getBackgroundImage() {
        return backgroundImage;
    }

    public Image getDeathSkinImage() {
        return deathSkinImage;
    }

    public Image getDeathScreenImage() {
        return deathScreenImage;
    }

    public long getDeathScreenGifDurationMs() {
        return deathScreenGifDurationMs;
    }

    public boolean isDeathSkinFadeOutEnabled() {
        return deathSkinFadeOut;
    }

    public void setDeathSkinImageFromResourcePath(String resourcePath) {
        deathSkinImage = loadImageIfPresent(resourcePath);
    }

    public void setDeathSkinFadeOut(boolean deathSkinFadeOut) {
        this.deathSkinFadeOut = deathSkinFadeOut;
    }

    public void clearDeathSkinImage() {
        deathSkinImage = null;
        deathSkinFadeOut = true;
    }

    public void setDeathScreenImageFromResourcePath(String resourcePath) {
        Image loadedImage = loadImage("death screen", resourcePath);
        if (loadedImage != null) {
            deathScreenImage = loadedImage;
        }
        URL url = ImageSelection.class.getResource(resourcePath);
        if (url != null && resourcePath.toLowerCase().endsWith(".gif")) {
            deathScreenGifDurationMs = computeGifDurationMs(url);
        } else {
            deathScreenGifDurationMs = 0;
        }
    }

    public void clearDeathScreenImage() {
        deathScreenImage = null;
        deathScreenGifDurationMs = 0;
    }

    private static long computeGifDurationMs(URL url) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(url.openStream())) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) return 0;
            ImageReader reader = readers.next();
            reader.setInput(iis);
            int numFrames = reader.getNumImages(true);
            long totalMs = 0;
            for (int i = 0; i < numFrames; i++) {
                IIOMetadata meta = reader.getImageMetadata(i);
                Node root = meta.getAsTree("javax_imageio_gif_image_1.0");
                Node child = root.getFirstChild();
                while (child != null) {
                    if ("GraphicControlExtension".equals(child.getNodeName())) {
                        NamedNodeMap attrs = child.getAttributes();
                        Node delayNode = attrs.getNamedItem("delayTime");
                        if (delayNode != null) {
                            totalMs += Long.parseLong(delayNode.getNodeValue()) * 10L;
                        }
                    }
                    child = child.getNextSibling();
                }
            }
            reader.dispose();
            return totalMs;
        } catch (IOException | RuntimeException e) {
            return 0;
        }
    }

    public boolean isStarsBackgroundEnabled() {
        return starsBackgroundEnabled;
    }

    public List<Point> getStarsSnapshot() {
        if (!starsBackgroundEnabled) {
            return Collections.emptyList();
        }
        return starsBackgroundPainter.getStarsSnapshot();
    }

    public void setGameImages() {
        shooterImage = loadImage("shooter", "/resources/Shooter/ShooterImage.png");
        invaderImage = loadImage("invader", "/resources/Invader/InvaderImage.png");
        // bulletImage starts null; falls back to triangle shape until user picks one
    }

    public void restoreDefaultThemeState(SpaceInvadersUI game) {
        setGameImages();
        bulletImage = null;
        clearBulletGifFrames();
        clearDeathSkinImage();
        clearDeathScreenImage();
        enableStarsBackground(game);
    }

    public void setShooterImageFromResourcePath(String resourcePath) {
        Image loadedImage = loadImage("shooter", resourcePath);
        if (loadedImage != null) {
            shooterImage = loadedImage;
        }
    }

    public void setInvaderImageFromResourcePath(String resourcePath) {
        Image loadedImage = loadImage("invader", resourcePath);
        if (loadedImage != null) {
            invaderImage = loadedImage;
        }
    }

    public void setBulletImageFromResourcePath(String resourcePath) {
        Image loadedImage = loadImage("bullet", resourcePath);
        if (loadedImage != null) {
            bulletImage = loadedImage;
        }

        URL url = ImageSelection.class.getResource(resourcePath);
        if (url != null && resourcePath.toLowerCase().endsWith(".gif")) {
            loadBulletGifFrames(url);
        } else {
            clearBulletGifFrames();
        }
    }

    public void setBackgroundImageFromResourcePath(String resourcePath) {
        Image loadedImage = loadImage("background", resourcePath);
        if (loadedImage != null) {
            starsBackgroundEnabled = false;
            backgroundImage = loadedImage;
        }
    }

    public void enableStarsBackground(SpaceInvadersUI game) {
        starsBackgroundEnabled = true;
        backgroundImage = null;
        starsBackgroundPainter.attachGame(game);
        starsBackgroundPainter.startIfNeeded();
    }

    private static Image loadImage(String imageType, String resourcePath) {
        URL url = ImageSelection.class.getResource(resourcePath);
        if (url == null) {
            GameExceptions.showErrorDialog("Resource not found for " + imageType + ": " + resourcePath);
            return null;
        }
        // ImageIcon supports animated GIFs; ImageIO.read() only loads the first frame
        return new ImageIcon(url).getImage();
    }

    private static Image loadImageIfPresent(String resourcePath) {
        URL url = ImageSelection.class.getResource(resourcePath);
        if (url == null) {
            return null;
        }
        return new ImageIcon(url).getImage();
    }

    private void loadBulletGifFrames(URL url) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(url.openStream())) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) {
                clearBulletGifFrames();
                return;
            }

            ImageReader reader = readers.next();
            reader.setInput(iis);
            int numFrames = reader.getNumImages(true);
            List<BufferedImage> frames = new ArrayList<>(numFrames);
            long[] frameEndTimesMs = new long[numFrames];
            long totalMs = 0;

            for (int i = 0; i < numFrames; i++) {
                frames.add(reader.read(i));
                long delayMs = extractGifFrameDelayMs(reader.getImageMetadata(i));
                if (delayMs <= 0) {
                    delayMs = 100;
                }
                totalMs += delayMs;
                frameEndTimesMs[i] = totalMs;
            }

            reader.dispose();
            bulletGifFrames = frames;
            bulletGifFrameEndTimesMs = frameEndTimesMs;
            bulletGifDurationMs = totalMs;
        } catch (IOException | RuntimeException e) {
            clearBulletGifFrames();
        }
    }

    private void clearBulletGifFrames() {
        bulletGifFrames = Collections.emptyList();
        bulletGifFrameEndTimesMs = new long[0];
        bulletGifDurationMs = 0;
    }

    private static long extractGifFrameDelayMs(IIOMetadata meta) {
        Node root = meta.getAsTree("javax_imageio_gif_image_1.0");
        Node child = root.getFirstChild();
        while (child != null) {
            if ("GraphicControlExtension".equals(child.getNodeName())) {
                NamedNodeMap attrs = child.getAttributes();
                Node delayNode = attrs.getNamedItem("delayTime");
                if (delayNode != null) {
                    return Long.parseLong(delayNode.getNodeValue()) * 10L;
                }
            }
            child = child.getNextSibling();
        }
        return 0;
    }
}
