package mayton.web;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static org.eclipse.jetty.util.StringUtil.isBlank;

public class DirectoryServlet extends HttpServlet {

    static Logger logger = LoggerFactory.getLogger("DirectoryServlet");

    ServletConfig servletConfig = getServletConfig();

    String root = "/storage"; //servletConfig.getInitParameter("root");

    private String trimPrefix(String prefix, String arg) {
        int prefixLength = prefix.length();
        return arg.substring(prefixLength);
    }

    private String getLeaveFromPath(String path) {
        if (path.isBlank() || !path.contains("/")) {
            return "";
        }
        return path.substring(path.lastIndexOf("/") + 1);
    }

    private String cutLeaveFromPath(String path) {
        if (path.isBlank() || !path.contains("/")) {
            return "";
        }
        return path.substring(0, path.lastIndexOf("/"));
    }

    private void printHeader(PrintWriter out, String directory, boolean withPlayer) {
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
                "<body>\n" +
                "<h1 class=\"title\">Directory: ");
        out.print(directory);
        out.print("</h1>\n" +
                "<table class=\"listing\">\n" +
                "<thead>\n" +
                "<tr>" +
                "<th class=\"name\"><a href=\"?C=N&O=D\">Name&nbsp; &#8679;</a></th>" +
                "<th class=\"lastmodified\"><a href=\"?C=M&O=A\">Last Modified</a></th>" +
                "<th class=\"size\"><a href=\"?C=S&O=A\">Size</a></th></tr>\n");
        if (withPlayer) {
            out.print("<th class='player'>Player</th>");
        }
        out.print("</thead>\n" +
                "<tbody>");
    }

    private void printRow(PrintWriter out, String name, String url, String lastModified, String size, boolean withAudio) {
        out.printf(
                "<tr>\n" +
                " <td class=\"name\"><a href=\"%s\">%s&nbsp;</a></td>\n" +
                " <td class=\"lastmodified\">%s&nbsp;</td>\n" +
                " <td class=\"size\">%s&nbsp;</td>\n", url, name, lastModified, size);
        if (withAudio) {
            out.print(" <td class='player'>");
            out.print("  <audio controls>");
            out.printf("   <source src=\"%s\" type=\"%s\">", url, MimeHelper.getMimeByExtension(url));
            out.print("  </audio>");
            out.print(" </td>");
        }
        out.print("</tr>\n");
    }

    private void printFooter(PrintWriter out) {
        out.println("</tbody>\n" +
                "</table>\n" +
                "</body></html>");
    }


    // globalPath ::= root + "/" + localPath
    //

    // TODO: Add content-range
    public void onLoad(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String url = request.getParameter("load");
        // Accept-Ranges: bytes
        // curl http://i.imgur.com/z4d4kWk.jpg -i -H "Range: bytes=0-1023"
        logger.info("Start uploading of {}", url);
        response.setContentType(MimeHelper.getMimeByExtension(url)); //);
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

        printHeader(out, localPath, true);

        if (isBlank(localPath) || localPath.equals("/")) {
            printRow(out, "[..]", "", "", "", false);
        } else {
            printRow(out, "[..]", "?lp=" + cutLeaveFromPath(localPath), "", "", false);
        }

        if (dir != null || dir.listFiles() == null) {
            File[] listFiles = dir.listFiles();
            logger.info("listfiles.length = {}", listFiles.length);
            for (File node : listFiles) {
                if (node.isDirectory()) {
                    String nodeGlobalPath = node.toPath().toString();
                    String cpath = trimPrefix(root, nodeGlobalPath);
                    printRow(out,
                            "[" + getLeaveFromPath(nodeGlobalPath) + "]",
                            "?lp=" + cpath,
                            simpleDateFormat.format(new Date(node.lastModified())),
                            "", false);
                }
            }
            for (File node : listFiles) {
                if (!node.isDirectory()) {
                    String nodeGlobalPath = node.toPath().toString();
                    if (isAudio(node)) {
                        printRow(out,
                                getLeaveFromPath(nodeGlobalPath),
                                "?load=" + trimPrefix(root, nodeGlobalPath),
                                simpleDateFormat.format(new Date(node.lastModified())),
                                byteCountToDisplaySize(node.length()),
                                true);
                        //printAudioControlBlock(out, "?load=" + trimPrefix(root, nodeGlobalPath));
                    } else {
                        printRow(out,
                                getLeaveFromPath(nodeGlobalPath),
                                "?load=" + trimPrefix(root, nodeGlobalPath),
                                simpleDateFormat.format(new Date(node.lastModified())),
                                byteCountToDisplaySize(node.length()),
                                false);
                    }
                }
            }
        } else {
            logger.warn("dir is null or listFiles is null for node = {}!", dir);
        }
        printFooter(out);
        response.setStatus(SC_OK);
    }

    private void printAudioControlBlock(PrintWriter out, String url) {

    }

    private boolean isAudio(File node) throws IOException {
        String lowerFileName = node.getCanonicalFile().toString().toLowerCase();
        return (lowerFileName.endsWith(".mp3") || lowerFileName.endsWith(".ogg"));
    }
}
