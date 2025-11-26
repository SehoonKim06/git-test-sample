package com.twolinecloud.MCP_gateway.openapi.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;


@Component
public class OpenAPIFileManager {
	private static final Logger log = LoggerFactory.getLogger(OpenAPIFileManager.class);

    @Value("${openapi.storage.directory:downloaded-specs}")
    private String storageDirectory;

    /**
     * @param content (YAML or JSON)
     * @param fileName 
     */
    public void saveSpec(String content, String fileName) {
        try {
            Path storagePath = getStoragePath();
            
            // Create if no directory
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
                log.info("저장 디렉토리 생성: " + storagePath);
            }

            // Add if no extension
            String finalFileName = ensureFileExtension(fileName, content);
            Path filePath = storagePath.resolve(finalFileName);

            // Save file
            Files.writeString(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("저장 완료: " + filePath);

        } catch (IOException e) {
            throw new RuntimeException("스펙 파일 저장 실패: " + fileName, e);
        }
    }

    /**
     * @param content
     * @param url
     */
    public void saveSpecFromUrl(String content, String url) {
        String fileName = generateFileNameFromUrl(url);
        saveSpec(content, fileName);
    }

    /**
     * @param fileName
     * @return 
     */
    public String readSpec(String fileName) {
        try {
            Path filePath = getStoragePath().resolve(fileName);

            if (!Files.exists(filePath)) {
                throw new RuntimeException("파일을 찾을 수 없습니다: " + fileName);
            }

            String content = Files.readString(filePath);
            log.info("읽기 완료: " + filePath + " (" + content.length() + " bytes)");
            return content;

        } catch (IOException e) {
            throw new RuntimeException("스펙 파일 읽기 실패: " + fileName, e);
        }
    }

    /**
     * @return directory Path
     */
    private Path getStoragePath() {
        return Paths.get(storageDirectory);
    }

    /**
     * @param url 
     * @return file name
     */
    private String generateFileNameFromUrl(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            
            // Extract file name from URL path
            if (path != null && !path.isEmpty()) {
                String[] parts = path.split("/");
                String lastPart = parts[parts.length - 1];
                
                // Use if there is file name 
                if (!lastPart.isEmpty() && lastPart.contains(".")) {
                    return lastPart;
                }
            }
            
            // Create file name by host
            String host = uri.getHost();	
            if (host != null) {
                host = host.replace(".", "_");
                return host + "_openapi.yaml";
            }
            
            // Default file name
            return "openapi_" + System.currentTimeMillis() + ".yaml";

        } catch (Exception e) {
            return "openapi_" + System.currentTimeMillis() + ".yaml";
        }
    }

    /**
     * @param fileName 
     * @param content (check JSON/YAML)
     * @return file name including extension
     */
    private String ensureFileExtension(String fileName, String content) {
        String lowerFileName = fileName.toLowerCase();
        
        if (lowerFileName.endsWith(".yaml") || 
            lowerFileName.endsWith(".yml") || 
            lowerFileName.endsWith(".json")) {
            return fileName;
        }
        
        //Choose extension
        String trimmedContent = content.trim();
        if (trimmedContent.startsWith("{") || trimmedContent.startsWith("[")) {
            return fileName + ".json";
        } else {
            return fileName + ".yaml";
        }
    }

}