/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.android.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.netbeans.modules.android.web.service.FileListService;
import org.netbeans.modules.android.web.service.UsageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author arsi
 */
@RestController
public class AutoupdateService {

    public static final String ORIG_IMPL_SEARCH_PATTERN = "org.netbeans.modules.projectui = ";
    String re1 = "(d)";	// Any Single Character 1
    String re2 = "(i)";	// Any Single Character 2
    String re3 = "(s)";	// Any Single Character 3
    String re4 = "(t)";	// Any Single Character 4
    String re5 = "(r)";	// Any Single Character 5
    String re6 = "(i)";	// Any Single Character 6
    String re7 = "(b)";	// Any Single Character 7
    String re8 = "(u)";	// Any Single Character 8
    String re9 = "(t)";	// Any Single Character 9
    String re10 = "(i)";	// Any Single Character 10
    String re11 = "(o)";	// Any Single Character 11
    String re12 = "(n)";	// Any Single Character 12
    String re13 = "(=)";	// Any Single Character 13
    String re14 = "(\".*?\")";	// Double Quote String 1

    Pattern p = Pattern.compile(re1 + re2 + re3 + re4 + re5 + re6 + re7 + re8 + re9 + re10 + re11 + re12 + re13 + re14, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    @Autowired
    FileListService fileService;
    @Autowired
    UsageService usageService;

    @RequestMapping(value = "/hit/counter.svg", method = RequestMethod.GET)
    public void getHit(HttpServletResponse response) {
        InputStream resourceAsStream = AutoupdateService.class.getResourceAsStream("/org/netbeans/modules/android/web/counter.svg");
        response.setContentType("image/svg+xml");
        response.setHeader("Cache-Control", "no-cache, no-store");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        try {
            String content = IOUtils.toString(resourceAsStream, "UTF-8");
            content = content.replace("9999", "" + usageService.getMaxCount());
            InputStream targetStream = new ByteArrayInputStream(content.getBytes("UTF-8"));
            org.apache.commons.io.IOUtils.copy(targetStream, response.getOutputStream());
            response.flushBuffer();
        } catch (IOException ex) {
            Logger.getLogger(AutoupdateService.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @RequestMapping(value = "/hit/current.svg", method = RequestMethod.GET)
    public void getCurrentHit(HttpServletResponse response) {
        InputStream resourceAsStream = AutoupdateService.class.getResourceAsStream("/org/netbeans/modules/android/web/counter.svg");
        response.setContentType("image/svg+xml");
        response.setHeader("Cache-Control", "no-cache, no-store");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        try {
            String content = IOUtils.toString(resourceAsStream, "UTF-8");
            content = content.replace("usage", "last PR");
            content = content.replace("9999", "" + usageService.getCurrentCount());
            InputStream targetStream = new ByteArrayInputStream(content.getBytes("UTF-8"));
            org.apache.commons.io.IOUtils.copy(targetStream, response.getOutputStream());
            response.flushBuffer();
        } catch (IOException ex) {
            Logger.getLogger(AutoupdateService.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @RequestMapping(value = "/updates/{file_name}", method = RequestMethod.GET)
    public void getFile(
            @PathVariable("file_name") String fileName,
            HttpServletResponse response) {
        Map<String, File> files = fileService.getFiles();
        File updatesXml = files.get("updates.xml");
        String updatesXmlContent = readFileToString(updatesXml);
        String origImplVersion = getOrigImplVersion(updatesXmlContent);
        for (Map.Entry<String, File> entry : files.entrySet()) {
            String key = entry.getKey();
            File value = entry.getValue();
            if (fileName.endsWith(key)) {
                String implVersion = fileName.replace("-" + key, "");
                if ("updates.xml".equals(key)) {
                    try {
                        updatesXmlContent = updatesXmlContent.replace(origImplVersion, implVersion);
                        Matcher matcher = p.matcher(updatesXmlContent);
                        List<String> toReplace = new ArrayList<>();
                        while (matcher.find()) {
                            toReplace.add(matcher.group(14));
                        }
                        for (String replace : toReplace) {
                            updatesXmlContent = updatesXmlContent.replace(replace, "\"" + implVersion + "-" + replace.substring(1));
                        }
                        //add support for NB11.1
                        if ("netbeans-release-428-on-20190716".equals(implVersion)) {
                            updatesXmlContent = updatesXmlContent.replace("org.netbeans.modules.java.source.base = 4", "org.netbeans.modules.java.source.base = 5");
                        }
                        InputStream targetStream = new ByteArrayInputStream(updatesXmlContent.getBytes("UTF-8"));
                        org.apache.commons.io.IOUtils.copy(targetStream, response.getOutputStream());
                        response.flushBuffer();
                    } catch (IOException ex) {
                        Logger.getLogger(AutoupdateService.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    switch (key) {
                        case "Gradle-Android-support-maven-impl-01.00-SNAPSHOT.nbm":
                        case "Gradle-Android-support-core-01.00-SNAPSHOT.nbm":
                            handleModule(value, origImplVersion, implVersion, response);
                            break;
                        default: {
                            try {
                                InputStream targetStream = new FileInputStream(value);
                                org.apache.commons.io.IOUtils.copy(targetStream, response.getOutputStream());
                                response.flushBuffer();
                            } catch (FileNotFoundException ex) {
                                Logger.getLogger(AutoupdateService.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (IOException ex) {
                                Logger.getLogger(AutoupdateService.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        break;
                    }

                    System.out.println("org.netbeans.modules.android.web.AutoupdateService.getFile()");
                }
                System.out.println("org.netbeans.modules.android.web.AutoupdateService.getFile()");
            }
        }
        System.out.println("org.netbeans.modules.android.web.AutoupdateService.getFile()");
    }

    //"incubator-netbeans-release-380-on-20181217-Gradle-Android-support-maven-impl-01.00-SNAPSHOT.nbm"
    public void handleModule(File value, String origImplVersion, String implVersion, HttpServletResponse response) {
        try {
            Path tempFile = Files.createTempFile("tempfiles", ".tmp");
            Path tempJarFile = Files.createTempFile("tempJarFiles", ".tmp");
            Files.copy(value.toPath(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            Map<String, String> env = new HashMap<>();
            env.put("create", "false");
            URI uri = URI.create("jar:file:" + tempFile.toAbsolutePath());
            try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
                updateImplInModuleFile(zipfs.getPath("/Info/info.xml"), origImplVersion, implVersion);
                updateImplInModuleFile(zipfs.getPath("/Info/info.xml"), origImplVersion, implVersion);
                Path modules = zipfs.getPath("/netbeans/modules/");
                List<Path> collect = Files.list(modules).filter((patch) -> {
                    return patch.toString().toLowerCase().endsWith("jar");
                }).collect(Collectors.toList());
                for (Path jarPath : collect) {
                    Files.copy(jarPath, tempJarFile, StandardCopyOption.REPLACE_EXISTING);
                    URI jarUri = URI.create("jar:file:" + tempJarFile.toAbsolutePath());
                    try (FileSystem jarZipfs = FileSystems.newFileSystem(jarUri, env)) {
                        updateImplInManifestFile(jarZipfs.getPath("/META-INF/MANIFEST.MF"), origImplVersion, implVersion);
                    } catch (IOException ex) {
                        Logger.getLogger(AutoupdateService.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    Files.copy(tempJarFile, jarPath, StandardCopyOption.REPLACE_EXISTING);
                }

            } catch (IOException ex) {
                Logger.getLogger(AutoupdateService.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                InputStream targetStream = new FileInputStream(tempFile.toFile());
                org.apache.commons.io.IOUtils.copy(targetStream, response.getOutputStream());
                response.flushBuffer();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(AutoupdateService.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(AutoupdateService.class.getName()).log(Level.SEVERE, null, ex);
            }
            Files.deleteIfExists(tempFile);
            Files.deleteIfExists(tempJarFile);
        } catch (IOException iOException) {
        }
    }

    public void updateImplInModuleFile(Path info, String origImplVersion, String implVersion) throws IOException {
        String content = new String(Files.readAllBytes(info), "UTF-8");
        content = content.replace(origImplVersion, implVersion);
        //add support for NB11.1
        if ("netbeans-release-428-on-20190716".equals(implVersion)) {
            content = content.replace("org.netbeans.modules.java.source.base = 4", "org.netbeans.modules.java.source.base = 5");
        }
        Files.write(info, content.getBytes("UTF-8"), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    public void updateImplInManifestFile(Path info, String origImplVersion, String implVersion) throws IOException {
        String content = new String(Files.readAllBytes(info), "UTF-8");
        Manifest manifest = new Manifest(new ByteArrayInputStream(content.getBytes("UTF-8")));
        Attributes mainAttributes = manifest.getMainAttributes();
        //----------------
        String moduleName = mainAttributes.getValue("OpenIDE-Module-Name");
        if ("NbAndroid-Core".equals(moduleName)) {
            String moduleImpl = mainAttributes.getValue("OpenIDE-Module-Implementation-Version");
            usageService.increment(moduleImpl);
        }
        //----------------
        String value = mainAttributes.getValue("OpenIDE-Module-Module-Dependencies");
        value = value.replace(origImplVersion, implVersion);
        //add support for NB11.1
        if ("netbeans-release-428-on-20190716".equals(implVersion)) {
            value = value.replace("org.netbeans.modules.java.source.base = 4", "org.netbeans.modules.java.source.base = 5");
        }
        mainAttributes.putValue("OpenIDE-Module-Module-Dependencies", value);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        manifest.write(bo);
        byte[] toByteArray = bo.toByteArray();
        Files.write(info, toByteArray, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        bo.close();
    }

    public String readFileToString(File value) {
        try {
            String content = new String(Files.readAllBytes(value.toPath()), "UTF-8");
            return content;
        } catch (Exception ex) {
            Logger.getLogger(AutoupdateService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }

    public String getOrigImplVersion(String content) {
        int indexOf = content.indexOf(ORIG_IMPL_SEARCH_PATTERN);
        String origImplVersion = content.substring(indexOf);
        indexOf = origImplVersion.indexOf(',');
        origImplVersion = origImplVersion.substring(0, indexOf).replace(origImplVersion, content).replace(ORIG_IMPL_SEARCH_PATTERN, "");
        return origImplVersion;
    }

}
