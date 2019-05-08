/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.android.web.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static final SortedMap<Date, AtomicInteger> COUNTERS_ORDERED = new ConcurrentSkipListMap<>();
    private static final String re1 = ".*?";	// Non-greedy match on filler
    private static final String re2 = "(-)";	// Any Single Character 1
    private static final String re3 = "(\\d+)";	// Integer Number 1
    private static final Pattern PATTERN = Pattern.compile(re1 + re2 + re3, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public UsageServiceImpl() {
        File ini = new File("/opt/NBANDROID/counter.ini");
        if (ini.exists()) {
            Properties prop = new Properties();
            try (FileInputStream fi = new FileInputStream(ini)) {
                prop.load(fi);
                prop.forEach((t, u) -> {
                    String id = (String) t;
                    String count = (String) u;
                    AtomicInteger atomicInteger = new AtomicInteger(Integer.parseInt(count));
                    COUNTERS.put(id, atomicInteger);
                    Date version = getVersion(id);
                    if (version != null) {
                        COUNTERS_ORDERED.put(version, atomicInteger);
                    }
                });
            } catch (Exception ex) {
                Logger.getLogger(UsageServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private static final DateFormat FORMAT = new SimpleDateFormat("yyMMddHH");

    private Date getVersion(String version) {
        Matcher m = PATTERN.matcher(version);
        if (m.find()) {
            String c1 = m.group(1);
            String int1 = m.group(2);
            try {
                return FORMAT.parse(int1);
            } catch (ParseException ex) {
                Logger.getLogger(UsageServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    @Override
    public void increment(String id) {
        try {
            AtomicInteger counter = COUNTERS.get(id);
            if (counter == null) {
                counter = new AtomicInteger();
                COUNTERS.put(id, counter);
                Date version = getVersion(id);
                if (version != null) {
                    COUNTERS_ORDERED.put(version, counter);
                }
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

    @Override
    public int getCurrentCount() {
        Date lastKey = COUNTERS_ORDERED.lastKey();
        AtomicInteger integer = COUNTERS_ORDERED.get(lastKey);
        if(integer!=null){
            return integer.get();
        }
        return 0;
    }

}
