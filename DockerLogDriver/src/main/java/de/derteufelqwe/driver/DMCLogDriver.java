package de.derteufelqwe.driver;

import com.google.common.util.concurrent.MoreExecutors;
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
import lombok.SneakyThrows;
import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class DMCLogDriver {

    public static final String SOCKET_FILE_PATH = "/run/docker/plugins/dev.sock";

    @Getter
    private static final ThreadPoolExecutor threadPool = createThreadPool();
    @Getter
    private static final DatabaseWriter databaseWriter = new DatabaseWriter();
    /**
     * Key:   Filename
     * Value: Thread future
     */
    @Getter
    private static final Map<String, Future<?>> logfileFutures = new HashMap<>();

    public AFUNIXServerSocket serverSocket;
    public EventLoopGroup bossGroup;
    public EventLoopGroup workerGroup;



    public DMCLogDriver() throws RuntimeException {
        databaseWriter.start();
        this.serverSocket = createSocket();
        this.bossGroup = new EpollEventLoopGroup();
        this.workerGroup = new EpollEventLoopGroup();
    }


    public AFUNIXServerSocket createSocket() throws RuntimeException {
        try {
            return AFUNIXServerSocket.newInstance();

        } catch (IOException e) {
            throw new RuntimeException("Failed to create unix socket.", e);
        }
    }

    private static ThreadPoolExecutor createThreadPool() {
        return (ThreadPoolExecutor) MoreExecutors.getExitingExecutorService(
                (ThreadPoolExecutor) Executors.newCachedThreadPool(), 5, TimeUnit.SECONDS
        );
    }


    public void addSignalHook() {
        SignalHandler signalHandler = new SignalHandler() {
            @Override
            public void handle(Signal signal) {
                System.err.println("HANDLING SIGNAL " + signal);
                shutdown();
            }
        };

        Signal.handle(new Signal("TERM"), signalHandler);
        Signal.handle(new Signal("INT"), signalHandler);
        Signal.handle(new Signal("HUP"), signalHandler);
    }

    public void startServer() {
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
            serverSocket.bind(new AFUNIXSocketAddress(socketFile));
            System.out.println("Server socket: " + serverSocket);

            ChannelFuture f = b.bind(new DomainSocketAddress(socketFile)).sync();
            f.channel().closeFuture().sync();

        } catch (IOException e1) {
            throw new RuntimeException("Failed to bind to unix socket.", e1);

        } catch (InterruptedException e2) {
            throw new RuntimeException("Webserver interrupted.", e2);

        } finally {
            shutdown();
        }
    }

    public void shutdown() throws RuntimeException {
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }

        if (serverSocket != null) {
            try {
                serverSocket.close();

            } catch (IOException e) {
                throw new RuntimeException("Failed to shutdown unix socket.", e);
            }
        }
    }

}
