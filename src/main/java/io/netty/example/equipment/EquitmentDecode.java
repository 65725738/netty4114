package io.netty.example.equipment;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.util.AttributeKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EquitmentDecode extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(EquitmentDecode.class);

    private boolean isFirst = false;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        isFirst = true;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        try {
            AttributeKey<String> akey = AttributeKey.valueOf("remoteSensingStation");
            String remoteSensingStation = ctx.channel().attr(akey).get();
//            if (StringUtils.isNotBlank(remoteSensingStation)) {
//                MongoDBExample example = new MongoDBExample();
//                example.setCollectionName("onlineEquitment");
//                example.createCriteria().eq("remoteSensingStation", remoteSensingStation);
//                MongoDBUtil.getInstance().delete(example);
//            }
        } catch (Exception e) {
            logger.error("", e);
        } finally {
            super.channelInactive(ctx);
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        if (isFirst == true) {
            //首次连上 有一个48字节的注册包 忽略
            if (in.readableBytes() < 48) {
                logger.info("<48");
                return;
            } else {
                byte[] bytes = new byte[48];
                in.readBytes(bytes);
                String tmp = new String(bytes);
                logger.info("注册包:" + tmp);
                isFirst = false;
                return;
            }
        }

        if (in.readableBytes() < 23) {
            logger.info("<23");
            return;
        }
        Map<String, Object> data = new HashMap<String, Object>();
        in.markReaderIndex();

        //        byte[] bytes = new byte[23];
        //        in.getBytes(in.readerIndex(), bytes);
        //        String tmp = new String(bytes, "GBK");
        //        logger.info("23:" + tmp);

        int length = readHead(in, data);
        if (length < 0) {
            throw new CorruptedFrameException("negative length: " + length);
        }

        if (in.readableBytes() < length + 6) {
            logger.info("<length + 6");
            in.resetReaderIndex();
            return;
        }
        //
        //        bytes = new byte[length];
        //        in.getBytes(in.readerIndex(), bytes);
        //        tmp = new String(bytes, "GBK");
        //        logger.info(":" + tmp);

        readBody(in, data, length);

        out.add(data);

    }

    private int readHead(ByteBuf in, Map<String, Object> data) throws Exception {
        try {
            byte startChar = in.readByte();

            if (startChar != 1)
                throw new Exception("开始字符不正确应该是1,实际是：" + startChar);
            byte[] bytes = new byte[2];
            in.readBytes(bytes);
            String centerAddr = new String(bytes);
            data.put("centerAddr", centerAddr);
            bytes = new byte[10];
            in.readBytes(bytes);
            String remoteSensingStation = new String(bytes);
            data.put("remoteSensingStation", remoteSensingStation);
            bytes = new byte[4];
            in.readBytes(bytes);
            String passwd = new String(bytes);
            data.put("passwd", passwd);
            bytes = new byte[2];
            in.readBytes(bytes);
            String functionCode = new String(bytes);
            data.put("functionCode", functionCode);
            bytes = new byte[4];
            in.readBytes(bytes);
            String signLength = new String(bytes);
            data.put("signLength", signLength);
            String sign = signLength.substring(0, 1);
            String tmp = signLength.substring(1);
            int length = Integer.parseInt(tmp, 16);
            data.put("sign", sign);
            data.put("length", length);
            return length;
        } catch (Exception e) {
            throw e;
        }

    }

    private void readBody(ByteBuf in, Map<String, Object> data, int length) throws  Exception {
        byte stx = in.readByte();
        if (stx != 2)
            throw new Exception("开始字符不正确应该是2,实际是：" + stx);
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        String reportedData = new String(bytes, "GBK");
        data.put("reportedData", reportedData);
        byte etx = in.readByte();
        if (etx != 3)
            throw new Exception("开始字符不正确应该是3,实际是：" + etx);
        bytes = new byte[4];
        in.readBytes(bytes);
        String crc = new String(bytes);
        data.put("crc", crc);
    }
}
