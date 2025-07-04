package com.example.agent.knowledge.repository;

import com.example.agent.core.dto.KnowledgeChunk;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SQL数据库访问仓库接口
 * 
 * @author agent
 */
public interface SqlDatabaseRepository {
    
    /**
     * 执行SQL查询并返回知识块
     * 
     * @param sql SQL查询语句
     * @param parameters 查询参数
     * @return 查询结果转换的知识块列表
     */
    List<KnowledgeChunk> executeQuery(String sql, Map<String, Object> parameters);
    
    /**
     * 根据关键词搜索
     * 
     * @param keyword 搜索关键词
     * @param tableName 目标表名
     * @param searchFields 搜索字段列表
     * @param limit 结果限制数量
     * @return 匹配的知识块列表
     */
    List<KnowledgeChunk> searchByKeyword(String keyword, String tableName, List<String> searchFields, int limit);
    
    /**
     * 根据条件查询
     * 
     * @param tableName 表名
     * @param conditions 查询条件
     * @param orderBy 排序字段
     * @param limit 结果限制数量
     * @return 查询结果知识块列表
     */
    List<KnowledgeChunk> findByConditions(String tableName, Map<String, Object> conditions, String orderBy, int limit);
    
    /**
     * 分页查询
     * 
     * @param tableName 表名
     * @param conditions 查询条件
     * @param orderBy 排序字段
     * @param offset 偏移量
     * @param limit 每页数量
     * @return 分页查询结果
     */
    PagedResult<KnowledgeChunk> findByConditionsPaged(String tableName, Map<String, Object> conditions, 
                                                      String orderBy, int offset, int limit);
    
    /**
     * 根据ID查询单条记录
     * 
     * @param tableName 表名
     * @param id 主键ID
     * @return 知识块对象，如果不存在则返回空
     */
    Optional<KnowledgeChunk> findById(String tableName, Object id);
    
    /**
     * 插入新记录
     * 
     * @param tableName 表名
     * @param data 数据映射
     * @return 插入后的主键ID
     */
    Object insert(String tableName, Map<String, Object> data);
    
    /**
     * 批量插入记录
     * 
     * @param tableName 表名
     * @param dataList 数据列表
     * @return 插入的记录数
     */
    int batchInsert(String tableName, List<Map<String, Object>> dataList);
    
    /**
     * 更新记录
     * 
     * @param tableName 表名
     * @param id 主键ID
     * @param data 更新数据
     * @return 是否更新成功
     */
    boolean update(String tableName, Object id, Map<String, Object> data);
    
    /**
     * 根据条件更新
     * 
     * @param tableName 表名
     * @param conditions 更新条件
     * @param data 更新数据
     * @return 更新的记录数
     */
    int updateByConditions(String tableName, Map<String, Object> conditions, Map<String, Object> data);
    
    /**
     * 删除记录
     * 
     * @param tableName 表名
     * @param id 主键ID
     * @return 是否删除成功
     */
    boolean delete(String tableName, Object id);
    
    /**
     * 根据条件删除
     * 
     * @param tableName 表名
     * @param conditions 删除条件
     * @return 删除的记录数
     */
    int deleteByConditions(String tableName, Map<String, Object> conditions);
    
    /**
     * 获取表结构信息
     * 
     * @param tableName 表名
     * @return 表结构信息
     */
    TableSchema getTableSchema(String tableName);
    
    /**
     * 获取数据库中所有表名
     * 
     * @return 表名列表
     */
    List<String> getAllTableNames();
    
    /**
     * 执行聚合查询
     * 
     * @param tableName 表名
     * @param aggregateFunction 聚合函数（COUNT, SUM, AVG等）
     * @param field 聚合字段
     * @param conditions 查询条件
     * @return 聚合结果
     */
    Object aggregate(String tableName, String aggregateFunction, String field, Map<String, Object> conditions);
    
    /**
     * 检查数据库连接健康状态
     * 
     * @return 是否健康
     */
    boolean isHealthy();
    
    /**
     * 分页结果包装类
     */
    class PagedResult<T> {
        private List<T> content;
        private long totalElements;
        private int totalPages;
        private int currentPage;
        private int pageSize;
        
        public PagedResult() {}
        
        public PagedResult(List<T> content, long totalElements, int currentPage, int pageSize) {
            this.content = content;
            this.totalElements = totalElements;
            this.currentPage = currentPage;
            this.pageSize = pageSize;
            this.totalPages = (int) Math.ceil((double) totalElements / pageSize);
        }
        
        // Getters and Setters
        public List<T> getContent() {
            return content;
        }
        
        public void setContent(List<T> content) {
            this.content = content;
        }
        
        public long getTotalElements() {
            return totalElements;
        }
        
        public void setTotalElements(long totalElements) {
            this.totalElements = totalElements;
        }
        
        public int getTotalPages() {
            return totalPages;
        }
        
        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }
        
        public int getCurrentPage() {
            return currentPage;
        }
        
        public void setCurrentPage(int currentPage) {
            this.currentPage = currentPage;
        }
        
        public int getPageSize() {
            return pageSize;
        }
        
        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }
    }
    
    /**
     * 表结构信息类
     */
    class TableSchema {
        private String tableName;
        private List<ColumnInfo> columns;
        private List<String> primaryKeys;
        
        public TableSchema() {}
        
        public TableSchema(String tableName, List<ColumnInfo> columns, List<String> primaryKeys) {
            this.tableName = tableName;
            this.columns = columns;
            this.primaryKeys = primaryKeys;
        }
        
        // Getters and Setters
        public String getTableName() {
            return tableName;
        }
        
        public void setTableName(String tableName) {
            this.tableName = tableName;
        }
        
        public List<ColumnInfo> getColumns() {
            return columns;
        }
        
        public void setColumns(List<ColumnInfo> columns) {
            this.columns = columns;
        }
        
        public List<String> getPrimaryKeys() {
            return primaryKeys;
        }
        
        public void setPrimaryKeys(List<String> primaryKeys) {
            this.primaryKeys = primaryKeys;
        }
    }
    
    /**
     * 列信息类
     */
    class ColumnInfo {
        private String columnName;
        private String dataType;
        private boolean nullable;
        private Object defaultValue;
        private String comment;
        
        public ColumnInfo() {}
        
        public ColumnInfo(String columnName, String dataType, boolean nullable) {
            this.columnName = columnName;
            this.dataType = dataType;
            this.nullable = nullable;
        }
        
        // Getters and Setters
        public String getColumnName() {
            return columnName;
        }
        
        public void setColumnName(String columnName) {
            this.columnName = columnName;
        }
        
        public String getDataType() {
            return dataType;
        }
        
        public void setDataType(String dataType) {
            this.dataType = dataType;
        }
        
        public boolean isNullable() {
            return nullable;
        }
        
        public void setNullable(boolean nullable) {
            this.nullable = nullable;
        }
        
        public Object getDefaultValue() {
            return defaultValue;
        }
        
        public void setDefaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
        }
        
        public String getComment() {
            return comment;
        }
        
        public void setComment(String comment) {
            this.comment = comment;
        }
    }
} 