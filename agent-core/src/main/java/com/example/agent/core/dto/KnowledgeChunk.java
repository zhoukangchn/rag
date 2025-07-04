package com.example.agent.core.dto;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 知识块 DTO
 * 
 * @author agent
 */
public class KnowledgeChunk {
    
    /**
     * 唯一标识符
     */
    private String id;
    
    /**
     * 知识内容
     */
    private String content;
    
    /**
     * 数据源标识
     */
    private String source;
    
    /**
     * 知识来源类型（VECTOR_STORE, SQL_DATABASE, API_SOURCE等）
     */
    private String sourceType;
    
    /**
     * 元数据信息
     */
    private Map<String, Object> metadata;
    
    /**
     * 向量嵌入（用于相似度计算）
     */
    private List<Double> embedding;
    
    /**
     * 相似度分数（搜索结果中使用）
     */
    private Double similarity;
    
    /**
     * 内容摘要
     */
    private String summary;
    
    /**
     * 关键词标签
     */
    private List<String> tags;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 检索时间
     */
    private Date retrievalTime;
    
    /**
     * 质量评分（0-1之间）
     */
    private Double score;
    
    // 默认构造函数
    public KnowledgeChunk() {}
    
    // 便利构造函数
    public KnowledgeChunk(String id, String content, String source, String sourceType) {
        this.id = id;
        this.content = content;
        this.source = source;
        this.sourceType = sourceType;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getSourceType() {
        return sourceType;
    }
    
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public List<Double> getEmbedding() {
        return embedding;
    }
    
    public void setEmbedding(List<Double> embedding) {
        this.embedding = embedding;
    }
    
    public Double getSimilarity() {
        return similarity;
    }
    
    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Date getRetrievalTime() {
        return retrievalTime;
    }
    
    public void setRetrievalTime(Date retrievalTime) {
        this.retrievalTime = retrievalTime;
    }
    
    public Double getScore() {
        return score != null ? score : (similarity != null ? similarity : 0.0);
    }
    
    public void setScore(Double score) {
        this.score = score;
    }
    
    @Override
    public String toString() {
        return "KnowledgeChunk{" +
                "id='" + id + '\'' +
                ", content='" + (content != null && content.length() > 100 ? 
                    content.substring(0, 100) + "..." : content) + '\'' +
                ", source='" + source + '\'' +
                ", sourceType='" + sourceType + '\'' +
                ", similarity=" + similarity +
                ", score=" + score +
                ", createdAt=" + createdAt +
                '}';
    }
} 