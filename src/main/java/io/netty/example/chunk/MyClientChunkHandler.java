package io.netty.example.chunk;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.*;
import io.netty.handler.stream.ChunkedNioStream;
import io.netty.handler.stream.ChunkedStream;
import io.netty.util.ReferenceCountUtil;

import java.nio.charset.Charset;

public class MyClientChunkHandler extends ChannelOutboundHandlerAdapter {

    Charset charset = Charset.forName("utf-8");
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if(msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf)msg;
            ByteInputStream in = new ByteInputStream();
            byte[] data = null;
            if(buf.hasArray()) {
                System.out.println("+++ is array");
                data = buf.array();
            } else {
                System.out.println("--- is direct");
                data = new byte[buf.readableBytes()];
                buf.writeBytes(data);

            }
            System.out.println("===== data length : " + data.length);
            in.setBuf(data);

            // 第一种方式：使用 ByteInputStream
            ChunkedStream stream = new ChunkedStream(in);

            // 第二种方式：使用 ByteBufInputStream
//            ByteBufInputStream byteBufInputStream = new ByteBufInputStream(buf);
//            ChunkedStream stream = new ChunkedStream(byteBufInputStream);

            ChannelProgressivePromise progressivePromise =  ctx.channel().newProgressivePromise();
            progressivePromise.addListener(new ChannelProgressiveFutureListener(){
                @Override
                public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) throws Exception {
//                    System.out.println("数据正在发送中。。。");
                }

                @Override
                public void operationComplete(ChannelProgressiveFuture future) throws Exception {
                    if(future.isSuccess()){
                        promise.setSuccess();
                    } else {
                        promise.setFailure(future.cause());
                    }
                    System.out.println("数据已经发送完了！");
                }
            });

            ReferenceCountUtil.release(msg);
            ctx.write(stream, progressivePromise);
        } else {
            super.write(ctx, msg, promise);
        }
    }
}
