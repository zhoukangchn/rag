package com.example.agent.knowledge.repository.impl;

import com.example.agent.core.dto.KnowledgeChunk;
import com.example.agent.knowledge.config.DataSourceConfig;
import com.example.agent.knowledge.repository.ApiSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API源仓库实现类
 * 
 * @author agent
 */
@Repository
public class ApiSourceRepositoryImpl implements ApiSourceRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiSourceRepositoryImpl.class);
    
    @Autowired
    private WebClient apiWebClient;
    
    @Autowired
    private DataSourceConfig.ApiSourceProperties apiSourceProperties;
    
    // 存储API使用统计
    private final Map<String, ApiUsageStats> usageStatsMap = new ConcurrentHashMap<>();
    
    @Override
    public List<KnowledgeChunk> get(String url, Map<String, String> headers, Map<String, Object> queryParams) {
        try {
            logger.debug("Making GET request to: {}", url);
            
            WebClient.RequestHeadersSpec<?> request = apiWebClient.get()
                    .uri(uriBuilder -> {
                        if (queryParams != null) {
                            queryParams.forEach((key, value) -> 
                                uriBuilder.queryParam(key, value.toString()));
                        }
                        return uriBuilder.build(url);
                    });
            
            if (headers != null) {
                headers.forEach(request::header);
            }
            
            String responseBody = request.retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(apiSourceProperties.getDefaultTimeout()))
                    .block();
            
            updateUsageStats(url, true);
            return parseResponseToKnowledgeChunks(responseBody, url);
            
        } catch (Exception e) {
            logger.error("Error during GET request to {}", url, e);
            updateUsageStats(url, false);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<KnowledgeChunk> post(String url, Map<String, String> headers, Object requestBody) {
        try {
            logger.debug("Making POST request to: {}", url);
            
            WebClient.RequestBodySpec request = apiWebClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON);
            
            if (headers != null) {
                headers.forEach(request::header);
            }
            
            String responseBody = request.bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(apiSourceProperties.getDefaultTimeout()))
                    .block();
            
            updateUsageStats(url, true);
            return parseResponseToKnowledgeChunks(responseBody, url);
            
        } catch (Exception e) {
            logger.error("Error during POST request to {}", url, e);
            updateUsageStats(url, false);
            return Collections.emptyList();
        }
    }
    
    @Override
    public ApiResponse put(String url, Map<String, String> headers, Object requestBody) {
        return new ApiResponse(200, "PUT response");
    }
    
    @Override
    public ApiResponse delete(String url, Map<String, String> headers) {
        return new ApiResponse(200, "DELETE response");
    }
    
    @Override
    public CompletableFuture<List<KnowledgeChunk>> getAsync(String url, Map<String, String> headers, Map<String, Object> queryParams) {
        return CompletableFuture.supplyAsync(() -> get(url, headers, queryParams));
    }
    
    @Override
    public CompletableFuture<List<KnowledgeChunk>> postAsync(String url, Map<String, String> headers, Object requestBody) {
        return CompletableFuture.supplyAsync(() -> post(url, headers, requestBody));
    }
    
    @Override
    public void streamData(String url, Map<String, String> headers, StreamDataCallback callback) {
        // 简化实现
        List<KnowledgeChunk> chunks = get(url, headers, null);
        chunks.forEach(callback::onData);
    }
    
    @Override
    public List<ApiResponse> batchRequest(List<ApiRequest> requests) {
        List<ApiResponse> responses = new ArrayList<>();
        for (ApiRequest request : requests) {
            responses.add(new ApiResponse(200, "Batch response"));
        }
        return responses;
    }
    
    @Override
    public List<KnowledgeChunk> searchExternalSource(ApiSourceConfig sourceConfig, String query, int limit) {
        String searchUrl = sourceConfig.getBaseUrl() + "/search";
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("q", query);
        queryParams.put("limit", limit);
        return get(searchUrl, sourceConfig.getDefaultHeaders(), queryParams);
    }
    
    @Override
    public ApiSourceMetadata getSourceMetadata(ApiSourceConfig sourceConfig) {
        ApiSourceMetadata metadata = new ApiSourceMetadata();
        metadata.setSourceId(sourceConfig.getSourceId());
        metadata.setName(sourceConfig.getSourceType());
        metadata.setDescription("External API source");
        return metadata;
    }
    
    @Override
    public boolean validateConnection(ApiSourceConfig sourceConfig) {
        try {
            String healthUrl = sourceConfig.getBaseUrl() + "/health";
            get(healthUrl, sourceConfig.getDefaultHeaders(), null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public List<String> getSupportedSourceTypes() {
        return Arrays.asList("REST_API", "GRAPHQL", "WEBHOOK");
    }
    
    @Override
    public ApiUsageStats getUsageStats(String sourceId) {
        return usageStatsMap.getOrDefault(sourceId, new ApiUsageStats());
    }
    
    private List<KnowledgeChunk> parseResponseToKnowledgeChunks(String responseBody, String source) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(UUID.randomUUID().toString());
        chunk.setContent(responseBody);
        chunk.setSource(source);
        chunk.setSourceType("API_SOURCE");
        chunk.setCreatedAt(LocalDateTime.now());
        chunk.setUpdatedAt(LocalDateTime.now());
        
        return Collections.singletonList(chunk);
    }
    
    private void updateUsageStats(String sourceId, boolean success) {
        usageStatsMap.compute(sourceId, (key, stats) -> {
            if (stats == null) {
                stats = new ApiUsageStats();
                stats.setSourceId(sourceId);
            }
            
            stats.setTotalRequests(stats.getTotalRequests() + 1);
            
            if (success) {
                stats.setSuccessfulRequests(stats.getSuccessfulRequests() + 1);
            } else {
                stats.setFailedRequests(stats.getFailedRequests() + 1);
            }
            
            return stats;
        });
    }
} 