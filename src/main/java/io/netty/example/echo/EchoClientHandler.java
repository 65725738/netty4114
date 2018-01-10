/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
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
package io.netty.example.echo;

import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Handler implementation for the echo client.  It initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 */
public class EchoClientHandler extends SimpleChannelInboundHandler<Object> {

    private final ByteBuf firstMessage;
    
    
    @Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    	//如果此处设置false 把这个handler设置为 channel的handler 那么这个serverchannel将不能自动接收数据.需要手动调用
    	//channel.read方法   每次读完之后  都要调用一次 才能重新读取
    	ctx.channel().config().setAutoRead(false);
    	ctx.executor().scheduleAtFixedRate(()->{
			//System.out.println("#########################################read");
			ctx.channel().read();
		}, 10,30, TimeUnit.SECONDS);
	}

    /**
     * Creates a client-side handler.
     */
    public EchoClientHandler() {
        firstMessage = Unpooled.buffer(EchoClient.SIZE);
        for (int i = 0; i < 16; i ++) {
            firstMessage.writeByte((byte) i);
        }
        
        
    }

//    @Override
//    public void channelActive(ChannelHandlerContext ctx) {
//        ctx.writeAndFlush(firstMessage);
//    }
    
  @Override
  public void channelActive(ChannelHandlerContext ctx) {
	  ChannelFuture fh=  ctx.writeAndFlush(firstMessage);
	  if (fh.cause() != null) {
		  System.out.println(fh.cause());
	}
  }


	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		// TODO Auto-generated method stub
		//System.out.println("channel$$$$$$$$$$$$$$$$$$read!!!!!!!!::::::::::"+msg);

		
		//ctx.close();
	}
}
