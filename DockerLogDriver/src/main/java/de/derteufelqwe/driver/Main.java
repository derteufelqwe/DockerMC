package de.derteufelqwe.driver;

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
import lombok.SneakyThrows;
import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.File;
import java.util.Map;

public class Main {

    public static final String SOCKET_FILE_PATH = "/run/docker/plugins/dev.sock";

    public AFUNIXServerSocket serverSocket = createSocket();
    public EventLoopGroup bossGroup = new EpollEventLoopGroup();
    public EventLoopGroup workerGroup = new EpollEventLoopGroup();


    @SneakyThrows
    public AFUNIXServerSocket createSocket() {
        return AFUNIXServerSocket.newInstance();
    }

    @SneakyThrows
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
            System.out.println("Server: " + serverSocket);

            ChannelFuture f = b.bind(new DomainSocketAddress(socketFile)).sync();
            f.channel().closeFuture().sync();

        } finally {
            shutdown();
        }
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

    @SneakyThrows
    public void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();

        serverSocket.close();
    }


    @SneakyThrows
    public static void main(String[] args) {
        Main main = new Main();
        main.addSignalHook();
        main.startServer();
    }

}
