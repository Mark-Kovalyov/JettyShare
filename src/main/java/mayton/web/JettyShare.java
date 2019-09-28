package mayton.web;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.RegEx;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.exit;
import static java.lang.System.getProperty;

public class JettyShare {

    static Logger logger = LoggerFactory.getLogger("JettyShare");

    public void  initJMX(Server server){
        // Setup JMX
        MBeanContainer mbContainer=new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
        server.addEventListener(mbContainer);
        server.addBean(mbContainer);
        server.addBean(Log.getLog());
    }

    public void initMimeTypes(ContextHandler contextHandler) throws IOException {
        Properties props = new Properties();
        MimeTypes mime = new MimeTypes();
        InputStream in = JettyShare.class.getResourceAsStream("mime.properties");
        props.load(in);
        Enumeration<Object> keys = props.keys();
        while(keys.hasMoreElements()){
            String key = (String)keys.nextElement();
            String value = props.getProperty(key);
            logger.info("Add mime type {} for extension {}",value,key);
            mime.addMimeMapping(key,value);
        }
        contextHandler.setMimeTypes(mime);
    }


    @RegEx
    public static final String IPV_4_ADDRESS_EXPR =
            "(?<host>\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})" +
            "(\\:(?<port>\\d{1,5}))?";

    @RegEx
    public static final String IPV_6_ADDRESS_EXPR =
            "(\\[(?<host>[0-9a-f\\:]+)\\])" +
            "(\\:(?<port>\\d{1,5}))?";

    public Pattern ipv4address = Pattern.compile(IPV_4_ADDRESS_EXPR);

    public Pattern ipv6address = Pattern.compile(IPV_6_ADDRESS_EXPR);

    public JettyShare(){

    }

    public JettyShare(String[] args) throws Exception {

        Server server = new Server();

        initJMX(server);

        server.setRequestLog((request, response) ->
                logger.info("{} {}", request.getRemoteHost(), request.getRequestURI()
                ));

        ServerConnector connector = new ServerConnector(server);

        int port    = 80;
        String host = "127.0.0.1";

        if (args.length > 0) {
            String arg0 = args[0];
            Matcher ipv4matcher = ipv4address.matcher(arg0);
            Matcher ipv6matcher = ipv6address.matcher(arg0);
            if (ipv4matcher.matches()) {
                host = ipv4matcher.group("host");
                String portString = ipv4matcher.group("port");
                if (portString!=null) {
                    port = Integer.valueOf(portString);
                }
                logger.info("Custom host = {} detected with port = {}", host,port);
            } else if (ipv6matcher.matches()) {
                host = ipv6matcher.group("host");
                String portString = ipv6matcher.group("port");
                if (portString!=null) {
                    port = Integer.valueOf(portString);
                }
                logger.info("Custom host = {} detected with port = {}", host,port);
            } else {
                logger.error("The argument '{}' doesn't matches to Ipv4 or Ipv6 adddress patterns",arg0);
                exit(1);
            }
        }

        connector.setPort(port);
        connector.setHost(host);
        connector.setName("Connector-1");

        server.addConnector(connector);

        ResourceHandler staticResourceHandler = new ResourceHandler();

        if (args.length > 1) {
            logger.info("Custom resource base '{}' detected",args[1]);
            staticResourceHandler.setResourceBase(args[1]);
        } else {
            String userHome = getProperty("user.home");
            logger.info("Default resource base is user.home = '{}' folder",userHome);
            staticResourceHandler.setResourceBase(userHome);
        }

        staticResourceHandler.setDirectoriesListed(true);
        staticResourceHandler.setWelcomeFiles(new String[]{"index.html"});

        ContextHandler staticContextHandler = new ContextHandler();
        staticContextHandler.setContextPath("/*");

        staticContextHandler.setHandler(staticResourceHandler);
        staticContextHandler.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "true");

        initMimeTypes(staticContextHandler);

        staticContextHandler.setMaxFormKeys(0);
        staticContextHandler.setMaxFormContentSize(0);

        //staticContextHandler.setProtectedTargets(new String[]{"/doc/.index",".index",".git"});

        //staticContextHandler.setInitParameter("","");

        // Create WebAppContext for JSP files.
        //WebAppContext webAppContext = new WebAppContext();
        //webAppContext.setContextPath("/jsp");
        //webAppContext.setResourceBase("./jsp/");

        // ??? THIS DOES NOT STOP DIR LISTING OF ./webapps/jsp/ ???
        // webAppContext.setInitParameter("dirAllowed", "false");

        // Create a handler list to store our static and servlet context handlers.

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{staticContextHandler});

        // Add the handlers to the server and start jetty.
        server.setHandler(handlers);
        server.start();
        server.join();
    }

    public static void main(String[] args) throws Exception {

        new JettyShare(args);

    }

}
