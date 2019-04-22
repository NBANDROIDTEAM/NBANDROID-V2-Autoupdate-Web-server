/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.android.web.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.android.web.service.UsageService;
import org.springframework.stereotype.Service;

/**
 *
 * @author arsi
 */
@Service
public class UsageServiceImpl implements UsageService {

    private static final ExecutorService POOL = Executors.newFixedThreadPool(1);
    private static final Map<String, AtomicInteger> COUNTERS = new ConcurrentHashMap<>();

    public UsageServiceImpl() {
        File ini = new File("/opt/NBANDROID/counter.ini");
        if (ini.exists()) {
            Properties prop = new Properties();
            try(FileInputStream fi = new FileInputStream(ini)){
                prop.load(fi);
                prop.forEach((t, u) -> {
                    String id = (String) t;
                    String count = (String) u;
                    COUNTERS.put(id, new AtomicInteger(Integer.parseInt(count)));
                });
            } catch (Exception ex) {
                Logger.getLogger(UsageServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            } 
        }
    }

    @Override
    public void increment(String id) {
        try {
            AtomicInteger counter = COUNTERS.get(id);
            if (counter == null) {
                counter = new AtomicInteger();
                COUNTERS.put(id, counter);
            }
            counter.incrementAndGet();
            POOL.execute(() -> {
                Properties prop = new Properties();
                Map<String, AtomicInteger> tmp = new HashMap<>(COUNTERS);
                for (Map.Entry<String, AtomicInteger> entry : COUNTERS.entrySet()) {
                    String key = entry.getKey();
                    AtomicInteger value = entry.getValue();
                    prop.put(key, "" + value.get());
                }
                try (FileOutputStream fo = new FileOutputStream("/opt/NBANDROID/counter.ini")) {
                    prop.store(fo, "");
                } catch (Exception ex) {
                    Logger.getLogger(UsageServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
                }                
            });
        } catch (Exception e) {
        }
    }

    @Override
    public int getMaxCount() {
        try {
            int max = 0;
            Map<String, AtomicInteger> tmp = new HashMap<>(COUNTERS);
            for (Map.Entry<String, AtomicInteger> entry : COUNTERS.entrySet()) {
                AtomicInteger value = entry.getValue();
                int val = value.get();
                if (val > max) {
                    max = val;
                }
            }
            return max;
        } catch (Exception e) {
        }
        return 0;
    }

}
