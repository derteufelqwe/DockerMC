package de.derteufelqwe.driver;

import com.google.common.util.concurrent.MoreExecutors;
import de.derteufelqwe.commons.Utils;
import de.derteufelqwe.driver.workers.DatabaseWriter;
import de.derteufelqwe.driver.workers.LogDownloadEntry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.Errors;
import io.netty.channel.unix.UnixChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

@Log4j2
public class DMCLogDriver {

    /**
     * Path to the unix socket
     */
    public static final String SOCKET_FILE_PATH = "/run/docker/plugins/dev.sock";
    /**
     * Time in ms for which the LogConsumer must have not read new data before the log download is considered complete
     */
    public static final int FINISH_LOG_READ_DELAY = 2000;

    @Getter
    private static ExecutorService threadPool;
    @Getter
    private static final DatabaseWriter databaseWriter = new DatabaseWriter();
    /**
     * Key:   Filename
     * Value: Container with LogConsumer and its Future
     */
    @Getter
    private static final Map<String, LogDownloadEntry> logfileConsumers = new ConcurrentHashMap<>();

    public EventLoopGroup bossGroup;
    public EventLoopGroup workerGroup;


    public DMCLogDriver() throws RuntimeException {
        threadPool = createThreadPool();
        this.bossGroup = new EpollEventLoopGroup();
        this.workerGroup = new EpollEventLoopGroup();
        databaseWriter.start();
    }


    private static ExecutorService createThreadPool() {
        return MoreExecutors.getExitingExecutorService(
                (ThreadPoolExecutor) Executors.newCachedThreadPool(), 5, TimeUnit.SECONDS
        );
    }


    public void addSignalHook() {
        SignalHandler signalHandler = new SignalHandler() {
            @Override
            public void handle(Signal signal) {
                log.warn("HANDLING SIGNAL " + signal);
                shutdown();
            }
        };

        Signal.handle(new Signal("TERM"), signalHandler);
        Signal.handle(new Signal("INT"), signalHandler);
//        Signal.handle(new Signal("HUP"), signalHandler);
    }

    public void startServer() throws DMCLogDriverException {
        try {
            ServerBootstrap b = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(EpollServerDomainSocketChannel.class)
                    .childHandler(new ChannelInitializer<UnixChannel>() {
                        @Override
                        protected void initChannel(UnixChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new HttpRequestDecoder());
                            p.addLast(new HttpResponseEncoder());
                            p.addLast(new UnixHandler());
                        }
                    });

            try (AFUNIXServerSocket socket = AFUNIXServerSocket.newInstance()) {
                File socketFile = new File(SOCKET_FILE_PATH);
                log.info("Trying to bind to socket.");
                this.bindToSocket(socket, socketFile);
                log.info("Server socket: " + socket);

                ChannelFuture f = b.bind(new DomainSocketAddress(socketFile)).sync();
                log.info("Bound HTTP server to socket");
                f.channel().closeFuture().sync();
            }

        } catch (IOException e1) {
            log.error(ExceptionUtils.getStackTrace(e1));
            throw new DMCLogDriverException("Failed to bind to unix socket.", e1);

        } catch (InterruptedException e2) {
            throw new DMCLogDriverException("Netty server got interrupted.", e2);
        }
    }

    /**
     * Tries to bind to the socket and removes any existing sockets while doing so.
     *
     * @param socket
     * @param socketFile
     * @throws IOException
     */
    private void bindToSocket(AFUNIXServerSocket socket, File socketFile) throws IOException {

        try {
            socket.bind(new AFUNIXSocketAddress(socketFile));
            return;

        } catch (IOException e) {
            log.error("Initial bind error", e);
            if (!e.getMessage().startsWith("Address already in use")) {
                throw e;
            }
        }

        log.warn("Socket '{}' already exists. Removing it.", SOCKET_FILE_PATH);
        String result = Utils.executeCommandOnHost(new String[]{"unlink", SOCKET_FILE_PATH});
        if (!result.equals("")) {
            log.warn("Unlink returned: {}", result);
        }

        log.warn("Retrying to bind to socket.");
        socket.bind(new AFUNIXSocketAddress(socketFile));
        log.info("Rebind succeeded.");
    }

    public void shutdown() throws RuntimeException {
        log.warn("LogDriver shutting down!");

        if (workerGroup != null) {
            try {
                workerGroup.shutdownGracefully().sync();

            } catch (InterruptedException e) {
                log.error("Worker group shutdown interrupted!");
            }
        }
        if (bossGroup != null) {
            try {
                bossGroup.shutdownGracefully().sync();

            } catch (InterruptedException e) {
                log.error("Boss group shutdown interrupted!");
            }
        }

        if (threadPool != null) {
            threadPool.shutdownNow();
        }


        databaseWriter.flushAll();
        databaseWriter.interrupt();

    }

}
