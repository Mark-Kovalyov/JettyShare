package mayton.web;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import static java.util.Arrays.asList;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static mayton.web.MediaStringUtils.*;
import static mayton.web.MimeHelper.*;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static org.eclipse.jetty.util.StringUtil.isBlank;

public class DirectoryServlet extends HttpServlet {

    static Logger logger = LoggerFactory.getLogger("DirectoryServlet");

    ServletConfig servletConfig = getServletConfig();

    String root = "/storage"; //servletConfig.getInitParameter("root");

    private void printDocumentHeader(PrintWriter out, String directory) {
        if (directory == null) {
            directory = "/";
        }
        out.print("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "<meta charset=\"utf-8\">\n" +
                "<link href=\"css/jetty-dir.css\" rel=\"stylesheet\" />\n" +
                "<title>Directory:");
        out.print(directory);
        out.print(
                "</title>\n" +
                        "</head>\n" +
                        "<body>\n");
    }

    private void printTableHeader(PrintWriter out, String directory, boolean withPlayer) {
        out.print("<h1 class=\"title\">Directory: ");
        out.print(directory);
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

    private void printRow(PrintWriter out, String name, String url, String lastModified, String size, boolean withAudio) {
        out.printf(
                "<tr>\n" +
                " <td class=\"name\"><a href=\"%s\">%s&nbsp;</a></td>\n" +
                " <td class=\"lastmodified\">%s&nbsp;</td>\n" +
                " <td class=\"size\">%s&nbsp;</td>\n", url, name, lastModified, size);
        if (withAudio) {
            Optional<String> optionalExtension = MediaStringUtils.getExtension(url);
            if (getMimeByExtension(optionalExtension).isPresent()) {
                out.print(" <td class='player'>");
                out.print("  <audio controls>");
                out.printf("   <source src=\"%s\" type=\"%s\">", url, getMimeByExtension(optionalExtension).get());
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
    //

    // TODO: Add content-range
    public void onLoad(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String url = request.getParameter("load");
        // Accept-Ranges: bytes
        // curl http://i.imgur.com/z4d4kWk.jpg -i -H "Range: bytes=0-1023"
        logger.info("Start uploading of {}", url);
        response.setContentType(getMimeByExtenensionOrOctet(getExtension(url))); //);
        OutputStream outputStream = response.getOutputStream();
        long res = IOUtils.copyLarge(new FileInputStream(root + "/" + request.getParameter("load")), outputStream);
        logger.info("Finished uploading of {} bytes", res);
        // Content-Range: bytes 0-1023/146515
        // Content-Length: 1024
        response.setStatus(SC_OK);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        String localPath = request.getParameter("lp");

        if (request.getParameterMap().containsKey("load")) {
            onLoad(request, response);
            return;
        }

        logger.info("localPath = {}", localPath);

        File dir;

        String globalPath;

        if (isBlank(localPath) || localPath.equals("/")) {
            globalPath = root;
        } else {
            globalPath = root + "/" + localPath;
        }

        dir = new File(globalPath);

        response.setContentType("text/html");

        PrintWriter out = response.getWriter();

        printDocumentHeader(out, localPath);

        printTableHeader(out, localPath, true);

        if (isBlank(localPath) || localPath.equals("/")) {
            printRow(out, "[..]", "", "", "", false);
        } else {
            printRow(out, "[..]", "?lp=" + cutLeaveFromPath(localPath), "", "", false);
        }

            File[] listFiles = dir.listFiles();
            logger.info("listfiles.length = {}", listFiles.length);

            Arrays.stream(listFiles)
                    .filter(File::isDirectory)
                    .forEach(directory -> {
                        String nodeGlobalPath = directory.toPath().toString();
                        String localUrl = trimPrefix(root, nodeGlobalPath);
                        printRow(out,
                                "[" + getLeaveFromPath(nodeGlobalPath).orElse("[empty]") + "]",
                                "?lp=" + localUrl,
                                simpleDateFormat.format(new Date(directory.lastModified())),
                                "", false);
                    });


            Arrays.stream(listFiles)
                    .filter(node -> !node.isDirectory())
                    .forEach(node -> {
                        String nodeGlobalPath = node.toPath().toString();
                        Optional<String> optionalName = getLeaveFromPath(nodeGlobalPath);
                        optionalName.ifPresent(s -> printRow(out,
                                s,
                                "?load=" + trimPrefix(root, nodeGlobalPath),
                                simpleDateFormat.format(new Date(node.lastModified())),
                                byteCountToDisplaySize(node.length()),
                                isAudio(s)));
                    });


            printTableFooter(out);

            if (directoryContainsVideo(listFiles)) {
                logger.info("Contains video");
                Arrays.stream(listFiles)
                        .filter(node -> !node.isDirectory())
                        .filter(node -> MimeHelper.isVideo(node.toPath().toString()))
                        .forEach(node -> {
                            String nodeGlobalPath = node.toPath().toString();
                            out.printf("<h5>%s</h5>\n", getLeaveFromPath(nodeGlobalPath));
                            out.printf("<video width = \"640\" height = \"480\" controls>\n");
                            out.printf("    <source src = \"?load=%s\" type = \"%s\"/>\n",
                                    trimPrefix(root, nodeGlobalPath),
                                    getMimeByExtenensionOrOctet(getExtension(nodeGlobalPath)));
                            out.printf("</video>\n");
                            out.printf("<br>\n");
                        });
            } else {
                logger.info("Doesn't contains");
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
