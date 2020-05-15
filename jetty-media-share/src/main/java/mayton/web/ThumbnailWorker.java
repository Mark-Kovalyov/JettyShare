package mayton.web;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ThumbnailWorker implements Runnable {

    // mayton.web.ThumbnailManager
    static Logger logger = LoggerFactory.getLogger(ThumbnailWorker.class);

    private String imageFileUrl;
    private String root;

    public ThumbnailWorker(@NotNull String imageFileUrl, @NotNull String root) {
        this.root = root;                 //   /www/home
        this.imageFileUrl = imageFileUrl; //   /folder/img.jpeg
    }

    @Override
    public void run() {
        try {
            if (Files.exists(Path.of(Config.THUMBNAIL_HOME + "/" + imageFileUrl))) {
                logger.info("Image {} already has thumb.", imageFileUrl);
            } else {
                //BufferedImage source = ImageIO.read(imageFile);
                //logger.info("Start copying {} to {}", imageFile.getCanonicalPath(), Config.THUMBNAIL_HOME);
                IOUtils.copyLarge(InputStream.nullInputStream(), OutputStream.nullOutputStream());
                //logger.info("Finish");
            }
        } catch (IOException e) {
            logger.error("ThumbnailManager", e);
        }
    }
}
