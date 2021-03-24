package de.derteufelqwe.driver;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.unix.UnixChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.nio.channels.SocketChannel;

public class CustomHttpServerHandler extends SimpleChannelInboundHandler<SocketChannel> {
    private HttpRequest request;
    StringBuilder responseData = new StringBuilder();


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
    protected void channelRead0(ChannelHandlerContext ctx, SocketChannel msg) throws Exception {
        System.out.println("read");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = this.request = (HttpRequest) msg;
            this.onHttpRequest(request, ctx);
        }

        responseData.append(RequestUtils.evaluateDecoderResult(request));

        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
            this.onHttpContent(httpContent, ctx);
        }

        if (msg instanceof LastHttpContent) {
            LastHttpContent trailer = (LastHttpContent) msg;
            this.onLastHttpContent(trailer, ctx);
        }
    }

    private void onHttpRequest(HttpRequest request, ChannelHandlerContext ctx) {
        // Just continue if the client wants it
        if (HttpUtil.is100ContinueExpected(request)) {
            writeResponse(ctx);
        }

        responseData.setLength(0);
        responseData.append(RequestUtils.formatParams(request));
    }

    private void onHttpContent(HttpContent content, ChannelHandlerContext ctx) {
        responseData.append(RequestUtils.formatBody(content));
        responseData.append(RequestUtils.evaluateDecoderResult(request));
    }

    private void onLastHttpContent(LastHttpContent lastContent, ChannelHandlerContext ctx) {
        responseData.append(RequestUtils.prepareLastResponse(request, lastContent));
        writeResponse(ctx, lastContent, responseData);
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


}
