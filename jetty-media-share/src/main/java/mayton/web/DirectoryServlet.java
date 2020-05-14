package mayton.web;

import org.apache.http.HttpResponse;
import org.eclipse.jetty.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.Math.abs;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_PARTIAL_CONTENT;
import static mayton.web.Config.FILE_PATH_SEPARATOR;
import static mayton.web.HttpStringHelper.decodeRange;
import static mayton.web.JettyMediaDiskUtils.normalizeFilePath;
import static mayton.web.MediaStringUtils.*;
import static mayton.web.MimeHelper.*;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static org.eclipse.jetty.util.StringUtil.isBlank;

@SuppressWarnings({"java:S3457","java:S2226"})
public class DirectoryServlet extends HttpServlet {

    static Logger logger = LoggerFactory.getLogger(DirectoryServlet.class);

    // TODO : CERT, MSC11-J. - Do not let session information leak within a servlet
    private String root = "~";

    public DirectoryServlet(String root) {
        this.root = root;
    }

    //ETag: "33a64df551425fcc55e4d42a148795d9f25f89d4"
    private void upgradeForETag(@NotNull HttpServletResponse resp, @NotNull String etag) {
        resp.addHeader("ETag", "\"" + etag + "\"");
    }

    // If-Modified-Since: Wed, 21 Oct 2015 07:28:00 GMT
    // response.setStatus(SC_NOT_MODIFIED);

    private void upgradeForNonCaching(@NotNull HttpServletResponse resp) {
        resp.addHeader("Accept-Ranges", "bytes");
        resp.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        resp.addHeader("Expires", "0");
    }

    private void upgradeFor24HourCaching(@NotNull HttpServletResponse resp, @Range(from = 0, to = Long.MAX_VALUE) long expiresAfterSeconds) {
        resp.addHeader("Accept-Ranges", "bytes");
        resp.addHeader("Cache-Control", "max-age=" + expiresAfterSeconds);
        resp.addHeader("Expires", HttpStringHelper.formatRfc1123Date(new Date(new Date().getTime() + (expiresAfterSeconds * 1000))));
    }

    private void printDocumentHeader(@NotNull PrintWriter out, @Nullable String directory) {
        out.print("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "<meta charset=\"utf-8\">\n" +
                "<link href=\"css/jetty-dir.css\" rel=\"stylesheet\" />\n" +
                "<title>Directory # :");
        out.print(directory == null ? "/" : directory);
        out.print(
                "</title>\n" +
                        "</head>\n" +
                        "<body>\n");
    }

    private void printTableHeader(@NotNull PrintWriter out, @Nullable String directory, boolean withPlayer) {
        out.print("<h1 class=\"title\">Directory : ");
        out.print(directory == null ? "/" : directory);
        out.print("</h1>\n" +
                "<table class=\"listing\">\n" +
                "<thead>\n" +
                "<tr>" +
                " <th class=\"name\">Name&nbsp;</th>" +
                " <th class=\"lastmodified\">Last Modified&nbsp;</th>" +
                " <th class=\"size\">Size&nbsp;</th>");
        if (withPlayer) {
            out.print("<th class='player'>Player</th>");
        }
        out.println("</tr>");
        out.print("</thead>\n");
    }

    @SuppressWarnings("java:S3655")
    private void printRow(@NotNull PrintWriter out, String name, String url, String lastModified, String size, boolean withAudio) {
        // TODO: Check for URL encoding
        String normalizedUrl = StringUtil.replace(url, FILE_PATH_SEPARATOR, "/");
        out.printf(
                "<tr>\n" +
                " <td class=\"name\"><a href=\"%s\">%s&nbsp;</a></td>\n" +
                " <td class=\"lastmodified\">%s&nbsp;</td>\n" +
                " <td class=\"size\">%s&nbsp;</td>\n", normalizedUrl, name, lastModified, size);
        if (withAudio) {
            Optional<String> optionalExtension = MediaStringUtils.getExtension(url);
            if (getMimeByExtension(optionalExtension).isPresent()) {
                out.print(" <td class='player'>");
                out.print("  <audio controls preload=\"metadata\">");
                out.printf("   <source src=\"%s\" type=\"%s\">", normalizedUrl, getMimeByExtension(optionalExtension).get());
                out.print("  </audio>");
                out.print(" </td>");
            }
        }
        out.print("</tr>\n");
    }

    private void printTableFooter(PrintWriter out) {
        out.println("</table>");
    }

    private void printDocumentFooter(PrintWriter out) {
        out.println("</body>");
    }


    // globalPath ::= root + "/" + localPath

    @SuppressWarnings({"java:S3655", "java:S2674"})
    public void onLoad(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String url = request.getParameter("load");
        String range = request.getHeader("Range");
        response.setContentType(getMimeByExtenensionOrOctet(getExtension(url)));
        OutputStream outputStream = response.getOutputStream();
        String loadFilePath = normalizeFilePath(root + FILE_PATH_SEPARATOR + URLDecoder.decode(request.getParameter("load"), UTF_8));
        logger.trace("loadFilePath = {}", loadFilePath);
        if (range != null) {
            logger.info("Request range '{}' uploading of {}", range, url);
            Optional<HttpRequestRange> rangeOptional = decodeRange(range);
            if (rangeOptional.isPresent()) {
                HttpRequestRange requestRange = rangeOptional.get();
                response.addHeader("Content-Range", range + "/" + JettyMediaDiskUtils.detectFileLength(loadFilePath));
                if (requestRange.getLength().isPresent()) {
                    response.addHeader("Content-Length", String.valueOf(requestRange.getLength().get()));
                }
                response.setStatus(SC_PARTIAL_CONTENT);
                long res = 0;
                if (requestRange.to.isPresent() && requestRange.from.isPresent()) {
                    try (InputStream inputStream = new FileInputStream(loadFilePath)) {
                        res = JettyMediaDiskUtils.copyLarge(
                                inputStream,
                                outputStream,
                                requestRange.from.get(),
                                requestRange.getLength().get()
                        );
                    }
                } else if (requestRange.from.isPresent()) {
                    try (InputStream inputStream = new FileInputStream(loadFilePath)) {
                        inputStream.skip(requestRange.from.get());
                        res = JettyMediaDiskUtils.copyLarge(inputStream, outputStream);
                    }
                } else if (requestRange.to.isPresent()) {
                    try (InputStream inputStream = new FileInputStream(loadFilePath)) {
                        res = JettyMediaDiskUtils.copyLarge(
                                inputStream,
                                outputStream,
                                0,
                                requestRange.to.get()
                        );
                    }
                }
                logger.trace("loaded {} bytes", res);
            } else {
                logger.warn("Bad request!");
                response.setStatus(SC_BAD_REQUEST);
            }
        } else {
            logger.info("Request full uploading of {}", url);
            Optional<String> optionalMd5 = JettyMediaDiskUtils.extractMD5fromFile(loadFilePath);
            if (optionalMd5.isPresent()) {
                upgradeForETag(response, optionalMd5.get());
            }
            long res = JettyMediaDiskUtils.copyLarge(new FileInputStream(loadFilePath), outputStream);
            logger.trace("loaded {} bytes", res);
            response.setStatus(SC_OK);
        }
    }

    @Override
    @SuppressWarnings({"java:S3655"})
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        if (request.getParameterMap().containsKey("load")) {
            upgradeFor24HourCaching(response, Config.EXPIRES_AFTER_SECONDS);
            onLoad(request, response);
            return;
        }

        try(PrintWriter out = response.getWriter()) {

            // dumpRequestParams(request);

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

            String localPath = request.getParameter("lp");

            upgradeForNonCaching(response);

            logger.info("localPath = '{}'", localPath);

            File dir;

            String globalPath;

            if (isBlank(localPath) || localPath.equals("/")) {
                globalPath = normalizeFilePath(root);
            } else {
                globalPath = normalizeFilePath(root + FILE_PATH_SEPARATOR + localPath);
            }

            dir = new File(globalPath);

            response.setContentType("text/html");

            printDocumentHeader(out, localPath);

            printTableHeader(out, localPath, true);

            if (isBlank(localPath) || localPath.equals("/")) {
                printRow(out, "[..]", "", "", "", false);
            } else {
                printRow(out, "[..]", "?lp=" +
                                (cutLeaveFromWebPath(localPath).isPresent() ? cutLeaveFromWebPath(localPath).get() : ""),
                        "", "", false);
            }

            File[] listFiles = dir.listFiles();

            // TODO:  listFiles == null ?
            if (listFiles != null) {
                Arrays.stream(listFiles)
                        .filter(File::isDirectory)
                        .forEach(directory -> {
                            String nodeGlobalPath = directory.toPath().toString();
                            String localUrl = trimPrefix(root, nodeGlobalPath);
                            printRow(out,
                                    "[" + getLeaveFromFilePath(nodeGlobalPath).orElse("??????") + "]",
                                    "?lp=" + localUrl,
                                    simpleDateFormat.format(new Date(directory.lastModified())),
                                    "", false);
                        });


                Arrays.stream(listFiles)
                        .filter(node -> !node.isDirectory())
                        .forEach(node -> {
                            String nodeGlobalPath = node.toPath().toString();
                            Optional<String> optionalName = getLeaveFromFilePath(nodeGlobalPath);
                            optionalName.ifPresent(s -> printRow(out,
                                    s,
                                    "?load=" + trimPrefix(root, nodeGlobalPath),
                                    simpleDateFormat.format(new Date(node.lastModified())),
                                    byteCountToDisplaySize(node.length()),
                                    isAudio(s)));
                        });


                printTableFooter(out);

                if (directoryContainsVideo(listFiles)) {
                    logger.trace("Contains video");
                    Arrays.stream(listFiles)
                            .filter(node -> !node.isDirectory())
                            .filter(node -> MimeHelper.isVideo(node.toPath().toString()))
                            .forEach(node -> {
                                String nodeGlobalPath = node.toPath().toString();
                                out.printf("<h5>%s</h5>\n", getLeaveFromFilePath(nodeGlobalPath).orElse(""))
                                        .printf("<video width = \"640\" height = \"480\" controls preload=\"metadata\">\n")
                                        .printf("    <source src = \"?load=%s\" type = \"%s\"/>\n",
                                                trimPrefix(root, nodeGlobalPath),
                                                getMimeByExtenensionOrOctet(getExtension(nodeGlobalPath)))
                                        .printf("</video>\n")
                                        .printf("<br>\n");
                            });
                }

                if (directoryContainsPictures(listFiles)) {
                    writePictureBlockHeader(out);
                    out.println(" <div class=\"column\">");
                    for (File node : listFiles) {
                        String nodeGlobalPath = node.toPath().toString();
                        if (abs(nodeGlobalPath.hashCode()) % 2 == 0) {
                            if (!node.isDirectory() && MimeHelper.isPicture(nodeGlobalPath)) {
                                // TODO: Refine replacement with function
                                out.printf("  <img src=\"?load=%s\">\n", StringUtil.replace(trimPrefix(root, nodeGlobalPath), FILE_PATH_SEPARATOR, "/"));
                            }
                        }
                    }
                    out.println(" </div>");
                    out.println(" <div class=\"column\">");
                    for (File node : listFiles) {
                        String nodeGlobalPath = node.toPath().toString();
                        if (abs(nodeGlobalPath.hashCode()) % 2 == 1) {
                            if (!node.isDirectory() && MimeHelper.isPicture(nodeGlobalPath)) {
                                out.printf("  <img src=\"?load=%s\">\n", StringUtil.replace(trimPrefix(root, nodeGlobalPath), FILE_PATH_SEPARATOR, "/"));
                            }
                        }
                    }
                    out.println(" </div>");
                    writePictureBlockFooter(out);
                }

            }
            printDocumentFooter(out);
            response.setStatus(SC_OK);
        }
    }

    private void writePictureBlockFooter(PrintWriter out) {
        out.println("<div class=\"row\">");

    }

    private void writePictureBlockHeader(PrintWriter out) {
        out.println("</div>");
    }


}
