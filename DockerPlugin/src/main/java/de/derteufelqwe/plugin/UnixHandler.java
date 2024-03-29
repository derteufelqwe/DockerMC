package de.derteufelqwe.plugin;

import de.derteufelqwe.plugin.endpoints.*;
import de.derteufelqwe.plugin.exceptions.InvalidAPIDataException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import lombok.extern.log4j.Log4j2;

import java.io.Serializable;

@Log4j2
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

        log.debug("Request: {}", request.uri());

        if (!request.method().equals(HttpMethod.POST)) {
            log.error("Got invalid method {}.", request.method());
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
        ctx.close();
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
        try {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(endpoint.getResponse(), CharsetUtil.UTF_8));

            ctx.writeAndFlush(response);

        } catch (InvalidAPIDataException e) {
            log.error("Invalid API data: ", e);
        }
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

            // --- Volume driver ---

            case "/VolumeDriver.Capabilities":
                writeResponse(new VolumeDriverCapabilitiesEP(data));
                break;

            case "/VolumeDriver.Get":
                writeResponse(new VolumeDriverGetEP(data));
                break;

            case "/VolumeDriver.Create":
                writeResponse(new VolumeDriverCreateEP(data));
                break;

            case "/VolumeDriver.Remove":
                writeResponse(new VolumeDriverRemoveEP(data));
                break;

            case "/VolumeDriver.Mount":
                writeResponse(new VolumeDriverMountEP(data));
                break;

            case "/VolumeDriver.Unmount":
                writeResponse(new VolumeDriverUnmountEP(data));
                break;

            case "/VolumeDriver.Path":
                writeResponse(new VolumeDriverPathEP(data));
                break;

            case "/VolumeDriver.List":
                writeResponse(new VolumeDriverListEP(data));
                break;

            default:
                log.error("Received message on unknown URI {}.", request.uri());
        }
    }

}
