package de.derteufelqwe.driver;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.derteufelqwe.driver.endpoints.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.io.Serializable;
import java.util.concurrent.*;

public class UnixHandler extends ChannelInboundHandlerAdapter {

    private final String ERROR_NOT_POST = "{\"error\": \"Method must be POST\"}";

    private HttpRequest request;
    private StringBuilder requestData = new StringBuilder();
    private ChannelHandlerContext ctx;


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.ctx = ctx;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }


    /**
     * Called when a message gets send to the connection
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            this.request = (HttpRequest) msg;
            this.onHttpRequest(request);
        }

        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
            this.onHttpContent(httpContent);
        }

        if (msg instanceof LastHttpContent) {
            LastHttpContent trailer = (LastHttpContent) msg;
            this.onLastHttpContent(trailer);
        }
    }

    /**
     * Handles the basic request.
     * Checks that the method was actually a POST method
     * @param request
     */
    private void onHttpRequest(HttpRequest request) {
        // Just continue if the client wants it
        if (HttpUtil.is100ContinueExpected(request)) {
            writeResponse(new StringBuilder());
        }

        System.out.printf("Request: %s%n", request.uri());

        if (!request.method().equals(HttpMethod.POST)) {
            System.err.println("Got invalid method " + request.method());
            writeResponse(new StringBuilder(ERROR_NOT_POST).append("\r\n"));
            ctx.close();
        }
    }

    /**
     * Handles content packages.
     * Combines them into on request data buffer.
     */
    private void onHttpContent(HttpContent content) {
        ByteBuf data = content.content();
        if (data.isReadable()) {
            requestData.append(data.toString(CharsetUtil.UTF_8));
        }
    }

    /**
     * Responds to the request when it's finished.
     * Returns the processed response to the request.
     * @param trailer
     */
    private void onLastHttpContent(LastHttpContent trailer) {
        this.dispatchData();
    }


    /**
     * Writes a string response to the client
     * @param responseData
     */
    private void writeResponse(StringBuilder responseData) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(responseData.toString(), CharsetUtil.UTF_8));

        ctx.writeAndFlush(response);
    }

    /**
     * Writes the result of an endpoint to the client
     * @param endpoint
     */
    private void writeResponse(Endpoint<? extends Serializable, ? extends Serializable> endpoint) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(endpoint.getResponse(), CharsetUtil.UTF_8));

        ctx.writeAndFlush(response);
    }


    /**
     * Dispatches the deserializes events from the client and answers them accordingly.
     */
    @SuppressWarnings("unchecked")
    private void dispatchData() {
        String data = requestData.toString();

        switch (request.uri()) {
            case "/Plugin.Activate":
                writeResponse(new PluginActivateEP(data));
                break;

            case "/LogDriver.Capabilities":
                writeResponse(new LogDriverCapabilitiesEP(data));
                break;

            case "/LogDriver.StartLogging":
                writeResponse(new LogDriverStartLoggingEP(data));
                break;

            case "/LogDriver.StopLogging":
                writeResponse(new LogDriverStopLoggingEP(data));
                break;

            default:
                System.err.println("Received message on unknown uri " + request.uri());
        }

        ctx.close();
    }

}
