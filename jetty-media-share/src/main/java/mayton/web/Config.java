package mayton.web;

import java.io.File;

public final class Config {

    public static final long EXPIRES_AFTER_SECONDS = 60L * 60 * 24;

    public static String FILE_PATH_SEPARATOR = File.separator;

    public static String THUMBNAIL_HOME = System.getProperty("java.io.tmpdir") + "/jetty-media-share";

    public static int THUMBNAIL_THREADS = 2;

    private Config(){}

}
