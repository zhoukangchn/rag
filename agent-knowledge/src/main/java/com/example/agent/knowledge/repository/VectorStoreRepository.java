package com.example.agent.knowledge.repository;

import com.example.agent.core.dto.KnowledgeChunk;
import java.util.List;
import java.util.Optional;

/**
 * 向量数据库访问仓库接口
 * 
 * @author agent
 */
public interface VectorStoreRepository {
    
    /**
     * 根据查询向量进行相似度搜索
     * 
     * @param queryEmbedding 查询向量
     * @param topK 返回的最相似结果数量
     * @param threshold 相似度阈值
     * @return 按相似度排序的知识块列表
     */
    List<KnowledgeChunk> similaritySearch(List<Double> queryEmbedding, int topK, double threshold);
    
    /**
     * 根据文本内容进行语义搜索
     * 
     * @param query 查询文本
     * @param topK 返回的最相似结果数量
     * @param threshold 相似度阈值
     * @return 按相似度排序的知识块列表
     */
    List<KnowledgeChunk> semanticSearch(String query, int topK, double threshold);
    
    /**
     * 存储单个知识块
     * 
     * @param knowledgeChunk 要存储的知识块
     * @return 存储后的知识块ID
     */
    String store(KnowledgeChunk knowledgeChunk);
    
    /**
     * 批量存储知识块
     * 
     * @param knowledgeChunks 要存储的知识块列表
     * @return 存储后的知识块ID列表
     */
    List<String> batchStore(List<KnowledgeChunk> knowledgeChunks);
    
    /**
     * 根据ID获取知识块
     * 
     * @param id 知识块ID
     * @return 知识块对象，如果不存在则返回空
     */
    Optional<KnowledgeChunk> findById(String id);
    
    /**
     * 根据来源获取知识块列表
     * 
     * @param source 数据源标识
     * @return 知识块列表
     */
    List<KnowledgeChunk> findBySource(String source);
    
    /**
     * 根据标签获取知识块列表
     * 
     * @param tags 标签列表
     * @return 包含指定标签的知识块列表
     */
    List<KnowledgeChunk> findByTags(List<String> tags);
    
    /**
     * 更新知识块
     * 
     * @param knowledgeChunk 要更新的知识块
     * @return 是否更新成功
     */
    boolean update(KnowledgeChunk knowledgeChunk);
    
    /**
     * 根据ID删除知识块
     * 
     * @param id 知识块ID
     * @return 是否删除成功
     */
    boolean deleteById(String id);
    
    /**
     * 根据来源删除所有知识块
     * 
     * @param source 数据源标识
     * @return 删除的记录数
     */
    int deleteBySource(String source);
    
    /**
     * 获取向量存储统计信息
     * 
     * @return 统计信息（总数量、存储大小等）
     */
    VectorStoreStats getStats();
    
    /**
     * 检查向量存储健康状态
     * 
     * @return 是否健康
     */
    boolean isHealthy();
    
    /**
     * 向量存储统计信息内部类
     */
    class VectorStoreStats {
        private long totalCount;
        private long storageSize;
        private int dimensions;
        private String indexType;
        
        public VectorStoreStats() {}
        
        public VectorStoreStats(long totalCount, long storageSize, int dimensions, String indexType) {
            this.totalCount = totalCount;
            this.storageSize = storageSize;
            this.dimensions = dimensions;
            this.indexType = indexType;
        }
        
        // Getters and Setters
        public long getTotalCount() {
            return totalCount;
        }
        
        public void setTotalCount(long totalCount) {
            this.totalCount = totalCount;
        }
        
        public long getStorageSize() {
            return storageSize;
        }
        
        public void setStorageSize(long storageSize) {
            this.storageSize = storageSize;
        }
        
        public int getDimensions() {
            return dimensions;
        }
        
        public void setDimensions(int dimensions) {
            this.dimensions = dimensions;
        }
        
        public String getIndexType() {
            return indexType;
        }
        
        public void setIndexType(String indexType) {
            this.indexType = indexType;
        }
        
        @Override
        public String toString() {
            return "VectorStoreStats{" +
                    "totalCount=" + totalCount +
                    ", storageSize=" + storageSize +
                    ", dimensions=" + dimensions +
                    ", indexType='" + indexType + '\'' +
                    '}';
        }
    }
} 