package io.fission.undertow;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import sun.nio.ch.ChannelInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Logger;

public class Server
{
    private static Logger logger = Logger.getGlobal();
    private Function fn;

    private void error(HttpServerExchange exchange, final String msg)
    {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.setStatusCode(500);
        logger.severe("Error: " + msg);
        exchange.getResponseSender().send(msg);
    }

    private void run()
    {
        HttpHandler handler = exchange -> {
            long startTime = System.nanoTime();
            logger.info("Function handler is called!");

            if (fn == null)
            {
                error(exchange, "Container not specialized");
                return;
            }
            else
            {
                fn.call(exchange);
            }
            long elapsedTime = System.nanoTime() - startTime;
            logger.info("Function call done in: " + (elapsedTime / 1000000) + " ms");
        };

        Undertow server = Undertow.builder()
            .addHttpListener(8888, "0.0.0.0")
            .setHandler(Handlers.routing()
                .get("/", handler)
                .post("/", handler)
                .delete("/", handler)
                .put("/", handler)
                .post("/v2/specialize",
                    exchange ->
                    {
                        long startTime = System.nanoTime();
                        logger.info( "Specialize function is called!");

                        final ChannelInputStream cis = new ChannelInputStream(exchange.getRequestChannel());
                        JSONParser parser=new JSONParser();
                        Reader reader = new InputStreamReader(cis);
                        Object obj = parser.parse(reader);
                        JSONObject node = (JSONObject)obj;
                        logger.info("Request params: " + node.toJSONString());
                        final String filepath = node.get("filepath").toString();
                        final String functionName = node.get("functionName").toString();

                        logger.info("FilePath: " + filepath);
                        logger.info("FunctionName: " + functionName);
                        File file = new File(filepath);
                        if (!file.exists())
                        {
                            error(exchange, "Missing jar!");
                            return;
                        }

                        String entryPoint = functionName;
                        logger.info("Entrypoint class:" + entryPoint);
                        if (entryPoint == null)
                        {
                            error(exchange, "Entrypoint class is missing in the function!");
                            return;
                        }

                        ClassLoader cl = null;
                        try
                        {
                            URL[] urls = { new URL("jar:file:" + file + "!/") };
                            if (this.getClass().getClassLoader() == null)
                            {
                                cl = URLClassLoader.newInstance(urls);
                            }
                            else
                            {
                                cl = URLClassLoader.newInstance(urls, this.getClass().getClassLoader());
                            }
                            if (cl == null)
                            {
                                error(exchange, "Failed to initialize the classloader");
                                return;
                            }
                            // Instantiate the function class
                            fn = (Function) cl.loadClass(entryPoint).newInstance();
                        }
                        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException e)
                        {
                            e.printStackTrace();
                            error(exchange, "Error loading the Function class file");
                            return;
                        }

                        long elapsedTime = System.nanoTime() - startTime;
                        logger.info("Specialize call done in: " + (elapsedTime / 1000000) + " ms");
                        exchange.setStatusCode(200);
                        exchange.getResponseSender().send("Done");
                    })).build();
        server.start();
    }

    public static void main(final String[] args) {
        Server server = new Server();
        server.run();
    }

}