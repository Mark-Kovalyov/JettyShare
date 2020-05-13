package mayton.web;

import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_PARTIAL_CONTENT;
import static mayton.web.Config.FILE_PATH_SEPARATOR;
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

    private void upgradeForNonCaching(HttpServletResponse resp) {
        resp.addHeader("Accept-Ranges", "bytes");
        resp.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        //resp.addHeader("Pragma", "no-cache");
        resp.addHeader("Expires", "0");
    }

    private void upgradeFor24HourCaching(HttpServletResponse resp) {
        resp.addHeader("Accept-Ranges", "bytes");
        resp.addHeader("Cache-Control", "max-age=" + (60 * 60 * 24));
        //resp.addHeader("Pragma", "cache");
        resp.addHeader("Expires", "0");
    }

    private void printDocumentHeader(PrintWriter out, String directory) {
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

    private void printTableHeader(PrintWriter out, String directory, boolean withPlayer) {
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
    private void printRow(PrintWriter out, String name, String url, String lastModified, String size, boolean withAudio) {
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
                out.print("  <audio controls>");
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
        String loadFilePath = normalizeFilePath(root + FILE_PATH_SEPARATOR + URLDecoder.decode(request.getParameter("load"), "UTF-8"));
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
            long res = JettyMediaDiskUtils.copyLarge(new FileInputStream(loadFilePath), outputStream);
            logger.trace("loaded {} bytes", res);
            response.setStatus(SC_OK);
        }
    }

    private void dumpRequestParams(HttpServletRequest request) {
        logger.info("GET uri = {} user = {}, host = {}, url = {}",
                request.getRequestURI(),
                request.getRemoteUser(),
                request.getRemoteHost(),
                request.getRequestURL());
        request.getParameterMap().entrySet().stream().forEach(item -> logger.info(" {} : '{}'", item.getKey(), item.getValue()));
    }

    @Override
    @SuppressWarnings({"java:S3655"})
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        dumpRequestParams(request);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        String localPath = request.getParameter("lp");

        if (request.getParameterMap().containsKey("load")) {
            upgradeFor24HourCaching(response);
            onLoad(request, response);
            return;
        } else {
            upgradeForNonCaching(response);
        }

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

        PrintWriter out = response.getWriter();

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

            Arrays.stream(listFiles)
                    .filter(File::isDirectory)
                    .forEach(directory -> {
                        String nodeGlobalPath = directory.toPath().toString();
                        String localUrl = trimPrefix(root, nodeGlobalPath);
                        printRow(out,
                                "[" + getLeaveFromFilePath(nodeGlobalPath).orElse("[empty]") + "]",
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

        printDocumentFooter(out);
        response.setStatus(SC_OK);
    }

    private boolean directoryContainsVideo(File[] listFiles) {
        Optional<String> res = Arrays.stream(listFiles)
                .filter(node -> !node.isDirectory())
                .map(node -> node.toPath().toString())
                .filter(MimeHelper::isVideo)
                .findAny();

        return res.isPresent();
    }


}
