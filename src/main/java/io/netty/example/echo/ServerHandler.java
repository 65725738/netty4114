package io.netty.example.echo;

import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ServerHandler extends SimpleChannelInboundHandler<Object>{

	
    
    
    
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		// TODO Auto-generated method stub
		

		System.out.println("ServerHandler channelRead0");
		
		ctx.executor().scheduleAtFixedRate(()->{
			//System.out.println("#########################################"+msg);
			 ByteBuf firstMessage = Unpooled.buffer(10000);
			    for (int i = 0; i < firstMessage.capacity(); i ++) {
			        firstMessage.writeByte((byte) i);
			    }
				ChannelFuture fh=  ctx.writeAndFlush(firstMessage);
			  if (fh.cause() != null) {
				  System.out.println(fh.cause());
			}
			  
		}, 1,1, TimeUnit.SECONDS);
	}
	
	

}
