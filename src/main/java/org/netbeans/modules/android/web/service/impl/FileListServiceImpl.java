/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.modules.android.web.service.impl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.modules.android.web.service.FileListService;
import org.springframework.stereotype.Service;

/**
 *
 * @author arsi
 */
@Service
public class FileListServiceImpl implements FileListService {

    @Override
    public Map<String, File> getFiles() {
        Map<String, File> files = new HashMap<>();
        File root = new File("/update");
        if (root.exists() && root.isDirectory()) {
            File[] listFiles = root.listFiles();
            for (File file : listFiles) {
                if (file.isFile()) {
                    files.put(file.getName(), file);
                }
            }
        }
        return files;
    }

}
