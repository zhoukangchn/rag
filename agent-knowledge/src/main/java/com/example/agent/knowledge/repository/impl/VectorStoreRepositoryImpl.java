package com.example.agent.knowledge.repository.impl;

import com.example.agent.core.dto.KnowledgeChunk;
import com.example.agent.knowledge.config.DataSourceConfig;
import com.example.agent.knowledge.repository.VectorStoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 向量存储仓库实现类
 * 基于内存的简单向量存储实现，支持余弦相似度计算
 * 
 * @author agent
 */
@Repository
public class VectorStoreRepositoryImpl implements VectorStoreRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(VectorStoreRepositoryImpl.class);
    
    // 内存存储
    private final Map<String, KnowledgeChunk> vectorStore = new ConcurrentHashMap<>();
    
    @Autowired
    private DataSourceConfig.VectorStoreProperties vectorStoreProperties;
    
    @Override
    public List<KnowledgeChunk> similaritySearch(List<Double> queryEmbedding, int topK, double threshold) {
        logger.debug("Performing similarity search with embedding size: {}, topK: {}, threshold: {}",
                queryEmbedding.size(), topK, threshold);
        
        return vectorStore.values().stream()
                .filter(chunk -> chunk.getEmbedding() != null && !chunk.getEmbedding().isEmpty())
                .map(chunk -> {
                    double similarity = calculateCosineSimilarity(queryEmbedding, chunk.getEmbedding());
                    if (similarity >= threshold) {
                        KnowledgeChunk result = new KnowledgeChunk();
                        copyChunkProperties(chunk, result);
                        result.setSimilarity(similarity);
                        return result;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
                .limit(topK)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<KnowledgeChunk> semanticSearch(String query, int topK, double threshold) {
        logger.debug("Performing semantic search with query: {}, topK: {}, threshold: {}",
                query, topK, threshold);
        
        // 简单的文本匹配实现，实际场景中应该使用嵌入模型
        return vectorStore.values().stream()
                .filter(chunk -> chunk.getContent() != null && 
                        chunk.getContent().toLowerCase().contains(query.toLowerCase()))
                .map(chunk -> {
                    double similarity = calculateTextSimilarity(query, chunk.getContent());
                    if (similarity >= threshold) {
                        KnowledgeChunk result = new KnowledgeChunk();
                        copyChunkProperties(chunk, result);
                        result.setSimilarity(similarity);
                        return result;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
                .limit(topK)
                .collect(Collectors.toList());
    }
    
    @Override
    public String store(KnowledgeChunk knowledgeChunk) {
        if (knowledgeChunk.getId() == null) {
            knowledgeChunk.setId(UUID.randomUUID().toString());
        }
        knowledgeChunk.setCreatedAt(LocalDateTime.now());
        knowledgeChunk.setUpdatedAt(LocalDateTime.now());
        
        vectorStore.put(knowledgeChunk.getId(), knowledgeChunk);
        
        logger.debug("Stored knowledge chunk with ID: {}", knowledgeChunk.getId());
        return knowledgeChunk.getId();
    }
    
    @Override
    public List<String> batchStore(List<KnowledgeChunk> knowledgeChunks) {
        List<String> ids = new ArrayList<>();
        for (KnowledgeChunk chunk : knowledgeChunks) {
            String id = store(chunk);
            ids.add(id);
        }
        logger.debug("Batch stored {} knowledge chunks", knowledgeChunks.size());
        return ids;
    }
    
    @Override
    public Optional<KnowledgeChunk> findById(String id) {
        KnowledgeChunk chunk = vectorStore.get(id);
        return Optional.ofNullable(chunk);
    }
    
    @Override
    public List<KnowledgeChunk> findBySource(String source) {
        return vectorStore.values().stream()
                .filter(chunk -> source.equals(chunk.getSource()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<KnowledgeChunk> findByTags(List<String> tags) {
        return vectorStore.values().stream()
                .filter(chunk -> chunk.getTags() != null && 
                        chunk.getTags().stream().anyMatch(tags::contains))
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean update(KnowledgeChunk knowledgeChunk) {
        if (knowledgeChunk.getId() == null || !vectorStore.containsKey(knowledgeChunk.getId())) {
            return false;
        }
        
        knowledgeChunk.setUpdatedAt(LocalDateTime.now());
        vectorStore.put(knowledgeChunk.getId(), knowledgeChunk);
        
        logger.debug("Updated knowledge chunk with ID: {}", knowledgeChunk.getId());
        return true;
    }
    
    @Override
    public boolean deleteById(String id) {
        KnowledgeChunk removed = vectorStore.remove(id);
        boolean success = removed != null;
        if (success) {
            logger.debug("Deleted knowledge chunk with ID: {}", id);
        }
        return success;
    }
    
    @Override
    public int deleteBySource(String source) {
        List<String> idsToDelete = vectorStore.values().stream()
                .filter(chunk -> source.equals(chunk.getSource()))
                .map(KnowledgeChunk::getId)
                .collect(Collectors.toList());
        
        int deletedCount = 0;
        for (String id : idsToDelete) {
            if (vectorStore.remove(id) != null) {
                deletedCount++;
            }
        }
        
        logger.debug("Deleted {} knowledge chunks from source: {}", deletedCount, source);
        return deletedCount;
    }
    
    @Override
    public VectorStoreStats getStats() {
        long totalCount = vectorStore.size();
        long storageSize = vectorStore.values().stream()
                .mapToLong(this::estimateChunkSize)
                .sum();
        
        int dimensions = vectorStoreProperties.getDimensions();
        String indexType = vectorStoreProperties.getIndexType();
        
        return new VectorStoreStats(totalCount, storageSize, dimensions, indexType);
    }
    
    @Override
    public boolean isHealthy() {
        try {
            // 执行简单的健康检查
            int size = vectorStore.size();
            logger.debug("Vector store health check: {} chunks stored", size);
            return true;
        } catch (Exception e) {
            logger.error("Vector store health check failed", e);
            return false;
        }
    }
    
    /**
     * 计算余弦相似度
     */
    private double calculateCosineSimilarity(List<Double> vectorA, List<Double> vectorB) {
        if (vectorA.size() != vectorB.size()) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += Math.pow(vectorA.get(i), 2);
            normB += Math.pow(vectorB.get(i), 2);
        }
        
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    /**
     * 计算文本相似度（简单实现）
     */
    private double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }
        
        String[] words1 = text1.toLowerCase().split("\\s+");
        String[] words2 = text2.toLowerCase().split("\\s+");
        
        Set<String> set1 = new HashSet<>(Arrays.asList(words1));
        Set<String> set2 = new HashSet<>(Arrays.asList(words2));
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    /**
     * 复制知识块属性
     */
    private void copyChunkProperties(KnowledgeChunk source, KnowledgeChunk target) {
        target.setId(source.getId());
        target.setContent(source.getContent());
        target.setSource(source.getSource());
        target.setSourceType(source.getSourceType());
        target.setMetadata(source.getMetadata());
        target.setEmbedding(source.getEmbedding());
        target.setSummary(source.getSummary());
        target.setTags(source.getTags());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
    }
    
    /**
     * 估算知识块大小（字节）
     */
    private long estimateChunkSize(KnowledgeChunk chunk) {
        long size = 0;
        if (chunk.getContent() != null) {
            size += chunk.getContent().length() * 2; // Unicode字符
        }
        if (chunk.getSummary() != null) {
            size += chunk.getSummary().length() * 2;
        }
        if (chunk.getEmbedding() != null) {
            size += chunk.getEmbedding().size() * 8; // Double类型
        }
        return size;
    }
} 