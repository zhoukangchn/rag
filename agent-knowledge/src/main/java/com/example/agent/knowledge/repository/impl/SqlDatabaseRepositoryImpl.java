package com.example.agent.knowledge.repository.impl;

import com.example.agent.core.dto.KnowledgeChunk;
import com.example.agent.knowledge.repository.SqlDatabaseRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SQL Database Repository Implementation
 * Provides comprehensive SQL database operations for knowledge chunks
 */
@Repository
public class SqlDatabaseRepositoryImpl implements SqlDatabaseRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public SqlDatabaseRepositoryImpl(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public List<KnowledgeChunk> executeQuery(String sql, Map<String, Object> parameters) {
        try {
            // Convert named parameters to positional parameters
            List<Object> paramList = new ArrayList<>();
            String processedSql = sql;
            
            if (parameters != null) {
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                    String paramName = ":" + entry.getKey();
                    if (processedSql.contains(paramName)) {
                        processedSql = processedSql.replace(paramName, "?");
                        paramList.add(entry.getValue());
                    }
                }
            }
            
            return jdbcTemplate.query(processedSql, new KnowledgeChunkRowMapper(), paramList.toArray());
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to execute SQL query: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<KnowledgeChunk> searchByKeyword(String keyword, String tableName, List<String> searchFields, int limit) {
        if (searchFields == null || searchFields.isEmpty()) {
            searchFields = Arrays.asList("content", "summary", "tags");
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ").append(tableName).append(" WHERE ");
        
        List<String> conditions = new ArrayList<>();
        for (String field : searchFields) {
            conditions.add("UPPER(" + field + ") LIKE UPPER(?)");
        }
        sql.append(String.join(" OR ", conditions));
        sql.append(" LIMIT ?");
        
        String searchPattern = "%" + keyword + "%";
        Object[] params = new Object[searchFields.size() + 1];
        Arrays.fill(params, 0, searchFields.size(), searchPattern);
        params[searchFields.size()] = limit;
        
        return jdbcTemplate.query(sql.toString(), new KnowledgeChunkRowMapper(), params);
    }
    
    @Override
    public List<KnowledgeChunk> findByConditions(String tableName, Map<String, Object> conditions, String orderBy, int limit) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ").append(tableName);
        
        List<Object> params = new ArrayList<>();
        
        if (conditions != null && !conditions.isEmpty()) {
            sql.append(" WHERE ");
            List<String> whereClauses = new ArrayList<>();
            
            for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                String column = entry.getKey();
                Object value = entry.getValue();
                
                if (value instanceof String && ((String) value).contains("%")) {
                    whereClauses.add(column + " LIKE ?");
                } else {
                    whereClauses.add(column + " = ?");
                }
                params.add(value);
            }
            sql.append(String.join(" AND ", whereClauses));
        }
        
        if (orderBy != null && !orderBy.trim().isEmpty()) {
            sql.append(" ORDER BY ").append(orderBy);
        }
        
        sql.append(" LIMIT ?");
        params.add(limit);
        
        return jdbcTemplate.query(sql.toString(), new KnowledgeChunkRowMapper(), params.toArray());
    }
    
    @Override
    public SqlDatabaseRepository.PagedResult<KnowledgeChunk> findByConditionsPaged(String tableName, Map<String, Object> conditions, String orderBy, int offset, int limit) {
        // Count total records
        StringBuilder countSql = new StringBuilder();
        countSql.append("SELECT COUNT(*) FROM ").append(tableName);
        
        List<Object> countParams = new ArrayList<>();
        
        if (conditions != null && !conditions.isEmpty()) {
            countSql.append(" WHERE ");
            List<String> whereClauses = new ArrayList<>();
            
            for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                String column = entry.getKey();
                Object value = entry.getValue();
                
                if (value instanceof String && ((String) value).contains("%")) {
                    whereClauses.add(column + " LIKE ?");
                } else {
                    whereClauses.add(column + " = ?");
                }
                countParams.add(value);
            }
            countSql.append(String.join(" AND ", whereClauses));
        }
        
        long totalElements = jdbcTemplate.queryForObject(countSql.toString(), Long.class, countParams.toArray());
        
        // Fetch paginated data
        StringBuilder dataSql = new StringBuilder();
        dataSql.append("SELECT * FROM ").append(tableName);
        
        List<Object> dataParams = new ArrayList<>(countParams);
        
        if (conditions != null && !conditions.isEmpty()) {
            dataSql.append(" WHERE ");
            List<String> whereClauses = new ArrayList<>();
            
            for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                String column = entry.getKey();
                Object value = entry.getValue();
                
                if (value instanceof String && ((String) value).contains("%")) {
                    whereClauses.add(column + " LIKE ?");
                } else {
                    whereClauses.add(column + " = ?");
                }
            }
            dataSql.append(String.join(" AND ", whereClauses));
        }
        
        if (orderBy != null && !orderBy.trim().isEmpty()) {
            dataSql.append(" ORDER BY ").append(orderBy);
        }
        
        dataSql.append(" LIMIT ? OFFSET ?");
        dataParams.add(limit);
        dataParams.add(offset);
        
        List<KnowledgeChunk> content = jdbcTemplate.query(dataSql.toString(), new KnowledgeChunkRowMapper(), dataParams.toArray());
        
        int currentPage = offset / limit;
        return new SqlDatabaseRepository.PagedResult<>(content, totalElements, currentPage, limit);
    }
    
    @Override
    public Optional<KnowledgeChunk> findById(String tableName, Object id) {
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        
        try {
            KnowledgeChunk chunk = jdbcTemplate.queryForObject(sql, new KnowledgeChunkRowMapper(), id);
            return Optional.ofNullable(chunk);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    @Override
    @Transactional
    public Object insert(String tableName, Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(tableName).append(" (");
        
        List<String> columns = new ArrayList<>(data.keySet());
        sql.append(String.join(", ", columns));
        sql.append(") VALUES (");
        sql.append(String.join(", ", Collections.nCopies(columns.size(), "?")));
        sql.append(")");
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql.toString(), new String[]{"id"});
            for (int i = 0; i < columns.size(); i++) {
                ps.setObject(i + 1, data.get(columns.get(i)));
            }
            return ps;
        }, keyHolder);
        
        return keyHolder.getKey();
    }
    
    @Override
    @Transactional
    public int batchInsert(String tableName, List<Map<String, Object>> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }
        
        Map<String, Object> firstRow = dataList.get(0);
        List<String> columns = new ArrayList<>(firstRow.keySet());
        
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(tableName).append(" (");
        sql.append(String.join(", ", columns));
        sql.append(") VALUES (");
        sql.append(String.join(", ", Collections.nCopies(columns.size(), "?")));
        sql.append(")");
        
        return jdbcTemplate.batchUpdate(sql.toString(), dataList, dataList.size(), 
            (ps, data) -> {
                for (int i = 0; i < columns.size(); i++) {
                    ps.setObject(i + 1, data.get(columns.get(i)));
                }
            }).length;
    }
    
    @Override
    @Transactional
    public boolean update(String tableName, Object id, Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(tableName).append(" SET ");
        
        List<String> setClauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            setClauses.add(entry.getKey() + " = ?");
            params.add(entry.getValue());
        }
        
        sql.append(String.join(", ", setClauses));
        sql.append(" WHERE id = ?");
        params.add(id);
        
        int updated = jdbcTemplate.update(sql.toString(), params.toArray());
        return updated > 0;
    }
    
    @Override
    @Transactional
    public int updateByConditions(String tableName, Map<String, Object> conditions, Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return 0;
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(tableName).append(" SET ");
        
        List<String> setClauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            setClauses.add(entry.getKey() + " = ?");
            params.add(entry.getValue());
        }
        
        sql.append(String.join(", ", setClauses));
        
        if (conditions != null && !conditions.isEmpty()) {
            sql.append(" WHERE ");
            List<String> whereClauses = new ArrayList<>();
            
            for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                whereClauses.add(entry.getKey() + " = ?");
                params.add(entry.getValue());
            }
            
            sql.append(String.join(" AND ", whereClauses));
        }
        
        return jdbcTemplate.update(sql.toString(), params.toArray());
    }
    
    @Override
    @Transactional
    public boolean delete(String tableName, Object id) {
        String sql = "DELETE FROM " + tableName + " WHERE id = ?";
        int deleted = jdbcTemplate.update(sql, id);
        return deleted > 0;
    }
    
    @Override
    @Transactional
    public int deleteByConditions(String tableName, Map<String, Object> conditions) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(tableName);
        
        List<Object> params = new ArrayList<>();
        
        if (conditions != null && !conditions.isEmpty()) {
            sql.append(" WHERE ");
            List<String> whereClauses = new ArrayList<>();
            
            for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                whereClauses.add(entry.getKey() + " = ?");
                params.add(entry.getValue());
            }
            
            sql.append(String.join(" AND ", whereClauses));
        }
        
        return jdbcTemplate.update(sql.toString(), params.toArray());
    }
    
    @Override
    public SqlDatabaseRepository.TableSchema getTableSchema(String tableName) {
        String sql = """
            SELECT 
                c.column_name,
                c.data_type,
                c.is_nullable,
                c.column_default
            FROM information_schema.columns c
            WHERE c.table_name = ?
            ORDER BY c.ordinal_position
        """;
        
        List<SqlDatabaseRepository.ColumnInfo> columns = jdbcTemplate.query(sql, (rs, rowNum) -> {
            SqlDatabaseRepository.ColumnInfo column = new SqlDatabaseRepository.ColumnInfo();
            column.setColumnName(rs.getString("column_name"));
            column.setDataType(rs.getString("data_type"));
            column.setNullable("YES".equals(rs.getString("is_nullable")));
            column.setDefaultValue(rs.getString("column_default"));
            return column;
        }, tableName);
        
        // Get primary keys
        String pkSql = """
            SELECT kcu.column_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
            WHERE tc.table_name = ? AND tc.constraint_type = 'PRIMARY KEY'
        """;
        
        List<String> primaryKeys = jdbcTemplate.queryForList(pkSql, String.class, tableName);
        
        return new SqlDatabaseRepository.TableSchema(tableName, columns, primaryKeys);
    }
    
    @Override
    public List<String> getAllTableNames() {
        String sql = """
            SELECT table_name 
            FROM information_schema.tables 
            WHERE table_schema = 'public' 
            ORDER BY table_name
        """;
        
        return jdbcTemplate.queryForList(sql, String.class);
    }
    
    @Override
    public Object aggregate(String tableName, String aggregateFunction, String field, Map<String, Object> conditions) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(aggregateFunction.toUpperCase()).append("(").append(field).append(") FROM ").append(tableName);
        
        List<Object> params = new ArrayList<>();
        
        if (conditions != null && !conditions.isEmpty()) {
            sql.append(" WHERE ");
            List<String> whereClauses = new ArrayList<>();
            
            for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                whereClauses.add(entry.getKey() + " = ?");
                params.add(entry.getValue());
            }
            
            sql.append(String.join(" AND ", whereClauses));
        }
        
        try {
            return jdbcTemplate.queryForObject(sql.toString(), Object.class, params.toArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute aggregate query: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isHealthy() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Row mapper for KnowledgeChunk objects
     */
    private static class KnowledgeChunkRowMapper implements RowMapper<KnowledgeChunk> {
        @Override
        public KnowledgeChunk mapRow(ResultSet rs, int rowNum) throws SQLException {
            KnowledgeChunk chunk = new KnowledgeChunk();
            
            try {
                chunk.setId(String.valueOf(rs.getLong("id")));
            } catch (SQLException e) {
                // ID column might not exist in all tables
            }
            
            try {
                chunk.setContent(rs.getString("content"));
            } catch (SQLException e) {
                // Handle missing columns gracefully
            }
            
            try {
                chunk.setSource(rs.getString("source"));
            } catch (SQLException e) {
                // Handle missing columns gracefully
            }
            
            try {
                chunk.setSourceType(rs.getString("source_type"));
            } catch (SQLException e) {
                // Handle missing columns gracefully
            }
            
            try {
                String metadataStr = rs.getString("metadata");
                if (metadataStr != null) {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("raw", metadataStr);
                    chunk.setMetadata(metadata);
                }
            } catch (SQLException e) {
                // Handle missing columns gracefully
            }
            
            try {
                chunk.setSimilarity(rs.getDouble("similarity"));
            } catch (SQLException e) {
                // Handle missing columns gracefully
            }
            
            try {
                chunk.setSummary(rs.getString("summary"));
            } catch (SQLException e) {
                // Handle missing columns gracefully
            }
            
            try {
                String tagsStr = rs.getString("tags");
                if (tagsStr != null && !tagsStr.trim().isEmpty()) {
                    chunk.setTags(Arrays.asList(tagsStr.split(",")));
                }
            } catch (SQLException e) {
                // Handle missing columns gracefully
            }
            
            try {
                chunk.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
            } catch (SQLException e) {
                // Handle missing columns gracefully
            }
            
            try {
                chunk.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
            } catch (SQLException e) {
                // Handle missing columns gracefully
            }
            
            return chunk;
        }
    }
} 