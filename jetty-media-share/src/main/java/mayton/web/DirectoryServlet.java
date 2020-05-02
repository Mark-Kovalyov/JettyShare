package mayton.web;

import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

import static javax.servlet.http.HttpServletResponse.SC_OK;

public class DirectoryServlet extends HttpServlet {

    static Logger logger = LoggerFactory.getLogger("DirectoryServlet");

    ServletConfig servletConfig = getServletConfig();

    String root = "/storage"; //servletConfig.getInitParameter("root");

    private String trimPrefix(String prefix, String arg) {
        return arg.substring(prefix.length());
    }

    private void printHeader(PrintWriter out, String directory) {
        out.printf("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "<meta charset=\"utf-8\">\n" +
                "<link href=\"jetty-dir.css\" rel=\"stylesheet\" />\n" +
                "<title>Directory: %s</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "<h1 class=\"title\">Directory: /</h1>\n" +
                "<table class=\"listing\">\n" +
                "<thead>\n" +
                "<tr>" +
                "<th class=\"name\"><a href=\"?C=N&O=D\">Name&nbsp; &#8679;</a></th>" +
                "<th class=\"lastmodified\"><a href=\"?C=M&O=A\">Last Modified</a></th>" +
                "<th class=\"size\"><a href=\"?C=S&O=A\">Size</a></th></tr>\n" +
                "</thead>\n" +
                "<tbody>", directory);
    }

    private void printRow(PrintWriter out, String name, String url, String lastModified, String size) {
        out.printf(
                "<tr><td class=\"name\"><a href=\"%s\">%s&nbsp;</a></td>" +
                "<td class=\"lastmodified\">%s&nbsp;</td>" +
                "<td class=\"size\">%s&nbsp;</td></tr>", url, name, lastModified, size);
    }

    private void printFooter(PrintWriter out) {
        out.println("</tbody>\n" +
                "</table>\n" +
                "</body></html>");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        String path = root + (req == null ? "/" : ((Request) req).getHttpURI().getPath());

        String localPath = trimPrefix(root, path).substring(1);

        logger.info("path = {}", path);
        File dir = new File(path);
        response.setContentType("text/html");


        PrintWriter out = response.getWriter();

        printHeader(out, localPath);

        if (dir != null) {
            File[] listFiles = dir.listFiles();
            logger.info("listfiles.length = {}", listFiles.length);
            for (File node : listFiles) {
                String cpath = node.toPath().toString();
                if (node.isDirectory()) {
                    printRow(out,
                            trimPrefix(path, cpath),
                            trimPrefix(path, cpath),
                            simpleDateFormat.format(new Date(node.lastModified())),
                            "");
                }
            }
            for (File node : listFiles) {
                String cpath = node.toPath().toString();
                if (!node.isDirectory()) {
                    printRow(out,
                            trimPrefix(path, cpath),
                            trimPrefix(path, cpath),
                            simpleDateFormat.format(new Date(node.lastModified())),
                            String.valueOf(node.length()));
                }
            }
        } else {
            logger.warn("dir is null!");
        }
        printFooter(out);
        response.setStatus(SC_OK);
    }
}
