package de.derteufelqwe.driver;

import com.google.common.util.concurrent.MoreExecutors;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.driver.exceptions.DMCDriverException;
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
import io.netty.channel.unix.UnixChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.File;
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

    public static final String VOLUME_PATH = "/home/arne/Plugin/volumes/";

    @Getter
    private static ExecutorService threadPool;
    // Manual getter
    private static SessionBuilder sessionBuilder;
    @Getter
    private static DatabaseWriter databaseWriter;
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
        sessionBuilder = new SessionBuilder("dockermc", "admin", "ubuntu1", Constants.POSTGRESDB_PORT);
        databaseWriter = new DatabaseWriter();
        databaseWriter.start();

        this.bossGroup = new EpollEventLoopGroup();
        this.workerGroup = new EpollEventLoopGroup();
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
    }

    public void startServer() throws DMCDriverException {
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

            File socketFile = new File(SOCKET_FILE_PATH);

            ChannelFuture f = b.bind(new DomainSocketAddress(socketFile)).sync();
            log.info("Bound HTTP server to socket {}.", SOCKET_FILE_PATH);
            f.channel().closeFuture().sync();

        } catch (InterruptedException e2) {
            throw new DMCDriverException("Netty server got interrupted.", e2);
        }
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

        log.info("Shutdown complete. Goodbye.");
    }


    // -----  Getter / setters  -----

    public static SessionBuilder getSessionBuilder() {
        return sessionBuilder;
    }

}
