package io.netty.example.equipment;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class QueneStoreThread implements Runnable {

    Logger logger = LoggerFactory.getLogger(QueneStoreThread.class);

    public static BlockingQueue<Map<String, Object>> blockingQueue = new LinkedBlockingDeque<Map<String, Object>>();

    List<Map<String, Object>> documentMaps = new ArrayList<Map<String, Object>>();

    long lastInsertTime = 0l;

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Map<String, Object> message = blockingQueue.poll(5, TimeUnit.SECONDS);

                if (message != null) {
                    documentMaps.add(message);
                }

                if (documentMaps.size() >= 50 || System.currentTimeMillis() - lastInsertTime > 10000) {
                    documentMaps.forEach(info -> {
//                        MongoDBExample example = new MongoDBExample();
//                        example.setCollectionName("onlineEquitment");
//                        example.createCriteria().eq("remoteSensingStation", info.get("remoteSensingStation"));
//                        Map<String, Object> updateDocument = new HashMap<String, Object>();
//                        updateDocument.put("remoteSensingStation", info.get("remoteSensingStation"));
//                        updateDocument.put("sendTime", new Date());
//                        updateDocument.put("companyId", info.get("companyId"));
//                        MongoDBUtil.getInstance().update(example, updateDocument, true);
//                        logger.info("onlineEquitment: " + updateDocument);

                    });

                    if (documentMaps.size() > 0) {
//                        MongoDBUtil.getInstance().insertManyMap("equitmentRecord", documentMaps);
//                        logger.info("equitmentRecord: size: " + documentMaps.size());
//                        logger.info("equitmentRecord: size: " + documentMaps);
//                        lastInsertTime = System.currentTimeMillis();
//                        documentMaps.clear();
                    }
                }

            } catch (Exception e) {
                logger.error("", e);
            }
        }
    }

    public void cancel() {
        Thread.currentThread().interrupt();
    }

}
