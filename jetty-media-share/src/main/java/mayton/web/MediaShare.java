package mayton.web;

import org.apache.commons.cli.*;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Properties;

import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SECURITY;
import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SESSIONS;

public class MediaShare {

    static Logger logger = LoggerFactory.getLogger(MediaShare.class);

    public void  initJMX(Server server){
        // Setup JMX
        MBeanContainer mbContainer=new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
        server.addEventListener(mbContainer);
        server.addBean(mbContainer);
        server.addBean(Log.getLog());
    }

    @NotNull
    Options createOptions() {
        return new Options()
                .addRequiredOption("h", "host", true, "Ethernet Host Interace to listen, ex: 127.0.0.1, ::::1")
                .addRequiredOption("p", "port", true, "Port number to listen, ex: 8081")
                .addRequiredOption("r", "root", true, "File root, ex: /www/html");
    }


    public void go(Properties properties) throws Exception {
        Server server = new Server();

        initJMX(server);

        server.setRequestLog((request, response) ->
                logger.info("{} {}", request.getRemoteHost(), request.getRequestURI()
        ));

        try(ServerConnector connector = new ServerConnector(server)) {

            int port = Integer.parseInt(properties.getProperty("port"));
            String host = properties.getProperty("host");

            connector.setPort(port);
            connector.setHost(host);
            connector.setName("Connector-1");

            server.setConnectors(new Connector[]{connector});

            ServletContextHandler servletContextHandler = new ServletContextHandler(NO_SESSIONS | NO_SECURITY);

            servletContextHandler.addServlet(new ServletHolder(new DirectoryServlet(properties.getProperty("root"))), "/");

            ResourceHandler resourceHandler = new ResourceHandler();
            resourceHandler.setResourceBase("css");
            resourceHandler.setDirectoriesListed(false);

            HandlerCollection handlers = new HandlerCollection();

            handlers.setHandlers(new Handler[]{
                    resourceHandler,
                    servletContextHandler,
                    new DefaultHandler()}
            );

            server.setHandler(handlers);
            server.start();
            server.join();
        }
    }

    public MediaShare(String[] args) throws Exception {

        List<GarbageCollectorMXBean> gcMxBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcMxBean : gcMxBeans) {
            logger.info("GC name = {}, objectName = {}", gcMxBean.getName(), gcMxBean.getObjectName());
        }

        CommandLineParser parser = new DefaultParser();
        Options options = createOptions();
        if (args.length == 0) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Jetty-Media-Share", createOptions());
            return;
        } else {
            Properties properties = new Properties();
            CommandLine line = parser.parse(options, args);
            // Mandatory
            properties.put("host", line.getOptionValue("host"));
            properties.put("port", line.getOptionValue("port"));
            properties.put("root", line.getOptionValue("root"));
            go(properties);
        }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("log4j.configurationFile", "log4j2.xml");
        new MediaShare(args);
    }

}
