package mayton.web;

import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@ThreadSafe
public class ThumbnailComponent {

    private ThreadPoolExecutor thumnailPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(Config.THUMBNAIL_THREADS);

    public synchronized void scheduleThumbWork(@NotNull File imageFile) {
        //thumnailPool.execute(new ThumbnailWorker(imageFile));
    }

}
