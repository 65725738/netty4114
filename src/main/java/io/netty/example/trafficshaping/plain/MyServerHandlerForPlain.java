package io.netty.example.trafficshaping.plain;

import io.netty.channel.ChannelHandlerContext;
import io.netty.example.trafficshaping.MyServerCommonHandler;

public class MyServerHandlerForPlain extends MyServerCommonHandler {

    @Override
    protected void sentData(ChannelHandlerContext ctx) {
        sentFlag = true;
        ctx.writeAndFlush(tempStr, getChannelProgressivePromise(ctx, future -> {
            if(ctx.channel().isWritable() && !sentFlag) {
                sentData(ctx);
            }
        }));
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        if(ctx.channel().isWritable()) {
            System.out.println(" ###### 重新开始写数据 ######");
            if (!sentFlag) {
                System.out.println(" ++++++++ 发送新数据包 ++++++++");
                sentData(ctx);
            }
        } else {
//            System.out.println(" ===== 写暂停 =====");
        }
    }
}
