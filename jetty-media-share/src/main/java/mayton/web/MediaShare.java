package mayton.web;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.RegEx;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Pattern;

import static java.lang.System.getProperty;
import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SECURITY;
import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SESSIONS;

public class MediaShare {

    static Logger logger = LoggerFactory.getLogger("JettyShare");

    public void  initJMX(Server server){
        // Setup JMX
        MBeanContainer mbContainer=new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
        server.addEventListener(mbContainer);
        server.addBean(mbContainer);
        server.addBean(Log.getLog());
    }

    public void upgradeContextHandlerWithMime(ContextHandler contextHandler) throws IOException {
        Properties props = new Properties();
        MimeTypes mime = new MimeTypes();
        InputStream in = MediaShare.class.getResourceAsStream("mime.properties");
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


    public MediaShare(){

    }

    public MediaShare(String[] args) throws Exception {


        Server server = new Server();

        initJMX(server);

        server.setRequestLog((request, response) ->
                logger.info("{} {}", request.getRemoteHost(), request.getRequestURI()
        ));

        ServerConnector connector = new ServerConnector(server);

        int port    = 8081;
        String host = "192.168.1.103";

        connector.setPort(port);
        connector.setHost(host);
        connector.setName("Connector-1");

        server.setConnectors(new Connector[] { connector });

        ServletContextHandler servletContextHandler = new ServletContextHandler(NO_SESSIONS | NO_SECURITY);

        //servletContextHandler.setContextPath("/root");
        servletContextHandler.addServlet(DirectoryServlet.class, "/");

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setResourceBase("css");
        resourceHandler.setDirectoriesListed(false);

        HandlerCollection handlers = new HandlerCollection();

        handlers.setHandlers(new Handler[] {
                resourceHandler,
                servletContextHandler,
                new DefaultHandler() }
        );

        server.setHandler(handlers);

        // Add the handlers to the server and start jetty.
        server.setHandler(handlers);
        server.start();
        server.join();
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("log4j.configurationFile", "log4j2.xml");
        new MediaShare(args);
    }

}
