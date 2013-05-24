package com.xx_dev.apn.proxy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;

import org.apache.log4j.Logger;

public class HttpProxyHandler extends ChannelInboundMessageHandlerAdapter<HttpObject> {

    private static Logger                 logger = Logger.getLogger(HttpProxyHandler.class);

    private Channel                       uaChannel;

    private String                        remoteAddr;

    private RemoteChannelInactiveCallback remoteChannelInactiveCallback;

    public HttpProxyHandler(Channel uaChannel, String remoteAddr,
                            RemoteChannelInactiveCallback remoteChannelInactiveCallback) {
        this.uaChannel = uaChannel;
        this.remoteAddr = remoteAddr;
        this.remoteChannelInactiveCallback = remoteChannelInactiveCallback;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Remote channel: " + remoteAddr + " active");
        }
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final HttpObject msg)
                                                                                      throws Exception {

        if (logger.isDebugEnabled()) {
            logger.debug("Recive From: " + remoteAddr + ", " + msg.getClass().getName());
        }

        HttpObject ho = msg;

        if (ho instanceof HttpResponse) {
            HttpResponse httpResponse = (HttpResponse) ho;
            httpResponse.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            httpResponse.headers().set("Proxy-Connection", HttpHeaders.Values.KEEP_ALIVE);
        }

        if (msg instanceof HttpContent) {
            ho = HttpContentCopyUtil.copy((HttpContent) msg);
            ((HttpContent) ho).retain();
        }

        uaChannel.write(ho);

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.warn("Remote channel: " + remoteAddr + " inactive");
        remoteChannelInactiveCallback.remoteChannelInactiveCallback(ctx, remoteAddr);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage(), cause);
        ctx.close();
    }

    public interface RemoteChannelInactiveCallback {
        public void remoteChannelInactiveCallback(ChannelHandlerContext remoteChannelCtx,
                                                  String remoeAddr) throws Exception;
    }

}