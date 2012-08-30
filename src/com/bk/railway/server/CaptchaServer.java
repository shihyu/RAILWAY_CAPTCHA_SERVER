
package com.bk.railway.server;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.thread.QueuedThreadPool;

import com.bk.railway.servlet.CaptchaServlet;

public class CaptchaServer {
    private final static Logger LOG = Logger.getLogger(Server.class.getName());
    private final static String PORT = "port";

    private static CommandLine buildCommandLineParser(String[] args) throws ParseException {
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        OptionBuilder.withLongOpt(PORT);
        options.addOption(OptionBuilder.withDescription("port used to start the serer").hasArg()
                .withArgName("SIZE").create());

        return parser.parse(options, args);
    }

    public static void main(String[] args) {
        try {
            final CommandLine commandLine = buildCommandLineParser(args);
            final int port = commandLine.hasOption(PORT) ? Integer.parseInt(commandLine
                    .getOptionValue(PORT)) : 8016;

            Server server = new Server(port);
            final Context context = new Context(server, "/servlets", Context.SESSIONS);
            context.addServlet(new ServletHolder(new CaptchaServlet()), "/handle");
            
            LOG.info("port=" + port);
            server.setThreadPool(new QueuedThreadPool(2));
            
            server.start();

        } catch (ParseException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
