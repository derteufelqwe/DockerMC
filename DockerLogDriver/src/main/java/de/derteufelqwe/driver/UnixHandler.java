package de.derteufelqwe.driver;

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

public class UnixHandler extends ChannelInboundHandlerAdapter {

    private final String ERROR_NOT_POST = "{\"error\": \"Method must be POST\"}";

    private static final Gson gson = new GsonBuilder().create();

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


    private void onHttpRequest(HttpRequest request) {
        // Just continue if the client wants it
        if (HttpUtil.is100ContinueExpected(request)) {
            writeResponse(ctx);
        }

        System.out.printf("Request: URI=%s%n", request.uri());
    }

    private void onHttpContent(HttpContent content) {
        ByteBuf data = content.content();
        if (data.isReadable()) {
            requestData.append(data.toString(CharsetUtil.UTF_8));
        }
    }

    private void onLastHttpContent(LastHttpContent trailer) {
        // Only allow POST method
        if (!request.method().equals(HttpMethod.POST)) {
            System.err.println("Got invalid method " + request.method());
            writeResponse(ctx, trailer, new StringBuilder(ERROR_NOT_POST).append("\r\n"));
            return;
        }

        this.dispatchData(trailer);
    }


    private void writeResponse(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE, Unpooled.EMPTY_BUFFER);
        ctx.write(response);
    }

    private void writeResponse(ChannelHandlerContext ctx, LastHttpContent trailer, StringBuilder responseData) {
        boolean keepAlive = HttpUtil.isKeepAlive(request);

        FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, ((HttpObject) trailer).decoderResult()
                .isSuccess() ? HttpResponseStatus.OK : HttpResponseStatus.BAD_REQUEST, Unpooled.copiedBuffer(responseData.toString(), CharsetUtil.UTF_8));

        httpResponse.headers()
                .set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

        if (keepAlive) {
            httpResponse.headers()
                    .setInt(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content()
                            .readableBytes());
            httpResponse.headers()
                    .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        ctx.write(httpResponse);

        if (!keepAlive) {
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
                    .addListener(ChannelFutureListener.CLOSE);
        }
    }


    private void writeResponse(Endpoint<? extends Serializable, ? extends Serializable> endpoint) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(endpoint.getResponse(), CharsetUtil.UTF_8));

        ctx.writeAndFlush(response);
    }


    @SuppressWarnings("unchecked")
    private void dispatchData(LastHttpContent trailer) {
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
