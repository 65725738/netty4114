package io.netty.example.equipment;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.AttributeKey;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EquitmentHandler extends MessageToMessageDecoder<Map<String, Object>> {

    private static final Logger logger = LoggerFactory.getLogger(EquitmentHandler.class);


    @Override
    protected void decode(ChannelHandlerContext ctx, Map<String, Object> msg, List<Object> out) throws Exception {
        logger.info("recieve:" + msg);
        String functionCode = (String) msg.get("functionCode");
        if ("2F".equalsIgnoreCase(functionCode)) {
            logger.info("recieve:心跳包 " + msg);
        } else {
            logger.info("recieve:" + msg);
            String reportedData = (String) msg.get("reportedData");
            /* 0001180925155409ST 1000000001 H TT 1809251554 PN05 0.6 PJ 21.0 Z 37.730
             * 要素名称，空格，要素值，空格，要素名称，空格，要素值，空格
             * */
            String seq = reportedData.substring(0, 4);
            String sendTime = reportedData.substring(4, 16);
            String remoteSensingStation = reportedData.substring(19, 29);
            String remoteSensingStationType = reportedData.substring(29, 31);
            String observeTime = reportedData.substring(35, 45);

            String data = reportedData.substring(45);
            String tmp[] = data.trim().split(" ");
            Map<String, String> map = new HashMap<String, String>();
            for (int i = 0; i < tmp.length; i = i + 2) {
                map.put(tmp[i], tmp[i + 1]);
            }

            logger.info("map:" + map);

            Map<String, Object> result = new HashMap<String, Object>();

            result.put("remoteSensingStation", remoteSensingStation);
            result.put("remoteSensingStationType", remoteSensingStationType);



            map.forEach((key, value) -> {


            });

            QueneStoreThread.blockingQueue.add(result);
            AttributeKey<String> akey = AttributeKey.valueOf("remoteSensingStation");
            ctx.channel().attr(akey).set(remoteSensingStation);
            logger.info("result:" + result);

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("", cause);
        ctx.close();
    }

}
