/*
 * Copyright (c) 2014 The APN-PROXY Project
 *
 * The APN-PROXY Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.xx_dev.apn.proxy;

import com.xx_dev.apn.proxy.ApnProxyRemoteHandler.RemoteChannelInactiveCallback;
import com.xx_dev.apn.proxy.config.ApnProxyListenType;
import com.xx_dev.apn.proxy.remotechooser.ApnProxyRemote;
import com.xx_dev.apn.proxy.remotechooser.ApnProxySslRemote;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;

import javax.net.ssl.SSLEngine;
import java.util.concurrent.TimeUnit;

/**
 * @author xmx
 * @version $Id: com.xx_dev.apn.proxy.ApnProxyRemoteChannelInitializer 14-1-8 16:13 (xmx) Exp $
 */
public class ApnProxyRemoteChannelInitializer extends ChannelInitializer<SocketChannel> {

    private ApnProxyRemote apnProxyRemote;

    private ChannelHandlerContext uaChannelCtx;
    private RemoteChannelInactiveCallback remoteChannelInactiveCallback;

    public ApnProxyRemoteChannelInitializer(ApnProxyRemote apnProxyRemote, ChannelHandlerContext uaChannelCtx,
                                            RemoteChannelInactiveCallback remoteChannelInactiveCallback) {
        this.apnProxyRemote = apnProxyRemote;
        this.uaChannelCtx = uaChannelCtx;
        this.remoteChannelInactiveCallback = remoteChannelInactiveCallback;
    }

    @Override
    public void initChannel(SocketChannel channel) throws Exception {

        ChannelPipeline pipeline = channel.pipeline();

        pipeline.addLast("idlestate", new IdleStateHandler(0, 0, 3, TimeUnit.MINUTES));
        pipeline.addLast("idlehandler", new ApnProxyIdleHandler());

        if (apnProxyRemote.getRemoteListenType() == ApnProxyListenType.SSL) {
            ApnProxySslRemote sslRemote = (ApnProxySslRemote) apnProxyRemote;
            SSLEngine engine = ApnProxySSLContextFactory.createClientSSLEnginForRemoteAddress(
                    sslRemote.getRemoteHost(), sslRemote.getRemotePort());
            engine.setUseClientMode(true);

            pipeline.addLast("ssl", new SslHandler(engine));
        }

        pipeline.addLast("codec", new HttpClientCodec());

        pipeline.addLast(ApnProxyRemoteHandler.HANDLER_NAME, new ApnProxyRemoteHandler(uaChannelCtx,
                remoteChannelInactiveCallback));

    }
}
