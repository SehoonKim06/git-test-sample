package com.twolinecloud.MCP_gateway.openapi.parser;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;
import java.util.Map;


@Component
public class OpenAPIDownloader {
	
	private static final Logger log = LoggerFactory.getLogger(OpenAPIDownloader.class);

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);;
    
    private final RestClient restClient;

    public OpenAPIDownloader(RestClient restClient) {
        this.restClient = restClient;
    }
    

    /**
     * Download OpenAPI Spec from URL
     * 
     * @param url  
     * @return 
     * @throws OpenAPIDownloadException
     */
    public DownloadResult downloadSpec(String url) {
        return downloadSpec(url, new DownloadOptions());
    }

    /**
     * Download OpenAPI Spec from URL with option
     * 
     * @param url 
     * @param options 
     * @return 
     * @throws OpenAPIDownloadException 
     */
    public DownloadResult downloadSpec(String url, DownloadOptions options) {
        // validation check URL
        validateUrl(url);

        log.info("OpenAPI 명세서 다운로드 시작: " + url);

        try {
            var requestSpec = restClient.get()
                .uri(url)
                .headers(headers -> {
                    headers.setAccept(List.of(
                        MediaType.APPLICATION_JSON,
                        MediaType.valueOf("application/yaml"),
                        MediaType.valueOf("application/x-yaml"),
                        MediaType.valueOf("text/yaml"),
                        MediaType.TEXT_PLAIN
                    ));
                    
                    // Add custom header
                    if (options.getHeaders() != null) {
                        options.getHeaders().forEach(headers::add);
                    }
                });

            String data = requestSpec
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new OpenAPIDownloadException(
                        "다운로드 실패: HTTP " + response.getStatusCode() + 
                        " " + response.getStatusText()
                    );
                })
                .body(String.class);

            // Check if empty
            if (data == null || data.trim().isEmpty()) {
                throw new OpenAPIDownloadException("다운로드한 명세서가 비어있습니다");
            }

            log.info("다운로드 완료: " + data.length() + " byte");

            return new DownloadResult(data, "application/json", url);

        } catch (Exception e) {
            if (e instanceof OpenAPIDownloadException) {
                throw (OpenAPIDownloadException) e;
            }
            throw new OpenAPIDownloadException("다운로드 실패: " + e.getMessage(), e);
        }
    }

    /**
     * Check URL validation
     * 
     * @param url 
     * @throws OpenAPIDownloadException 
     */
    private void validateUrl(String url) {
        try {
            URI uri = new URI(url);

            // Allow only HTTP and HTTPS
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
                throw new OpenAPIDownloadException(
                    "지원하지 않는 프로토콜입니다: " + scheme + 
                    ". HTTP 또는 HTTPS를 사용하세요."
                );
            }

            // Check host
            if (uri.getHost() == null || uri.getHost().isEmpty()) {
                throw new OpenAPIDownloadException("유효하지 않은 URL: 호스트명이 없습니다");
            }
        } catch (URISyntaxException e) {
            throw new OpenAPIDownloadException("유효하지 않은 URL 형식입니다: " + url, e);
        }
    }

    /**
     * Download options
     */
    public static class DownloadOptions {
        private Duration timeout = DEFAULT_TIMEOUT;
        private Map<String, String> headers;
        private boolean followRedirects = true;

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }

        public boolean isFollowRedirects() {
            return followRedirects;
        }

        public void setFollowRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
        }
    }

    /**
     * Download result
     */
    public record DownloadResult(
        String data,        // Downloaded Spec data
        String contentType, // Content-Type
        String url          // Final URL
    ) {}

    /**
     * Download Exception
     */
    public static class OpenAPIDownloadException extends RuntimeException {
        public OpenAPIDownloadException(String message) {
            super(message);
        }

        public OpenAPIDownloadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}