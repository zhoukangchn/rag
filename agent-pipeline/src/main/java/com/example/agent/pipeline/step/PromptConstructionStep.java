package com.example.agent.pipeline.step;

import com.example.agent.core.dto.AgentContext;
import com.example.agent.core.dto.KnowledgeChunk;
import com.example.agent.core.dto.PlanDetail;
import com.example.agent.pipeline.template.AbstractPipelineStep;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Prompt构建步骤
 * 负责将检索到的知识块组装成适合LLM的提示文本
 * 
 * @author agent
 */
@Component
public class PromptConstructionStep extends AbstractPipelineStep {
    
    private static final String STEP_NAME = "PROMPT_CONSTRUCTION";
    
    // 默认提示模板
    private static final String DEFAULT_SYSTEM_PROMPT = 
        "你是一个专业的知识助手。请基于以下知识信息回答用户问题。\n" +
        "要求：\n" +
        "1. 回答要准确、简洁、有帮助\n" +
        "2. 如果知识信息不足以回答问题，请说明\n" +
        "3. 引用相关知识时请保持客观\n" +
        "4. 避免生成误导性信息\n\n";
    
    private static final String KNOWLEDGE_SECTION_HEADER = "===== 相关知识信息 =====\n";
    private static final String QUESTION_SECTION_HEADER = "\n===== 用户问题 =====\n";
    private static final String ANSWER_SECTION_HEADER = "\n===== 请回答 =====\n";
    
    @Override
    protected boolean doExecute(AgentContext context) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 验证必要的上下文信息
            if (!validateContext(context, "query")) {
                return false;
            }
            
            // 获取知识块
            List<KnowledgeChunk> knowledgeChunks = getKnowledgeChunks(context);
            
            // 获取计划详情
            PlanDetail planDetail = getPlanDetail(context);
            
            // 构建系统提示
            String systemPrompt = buildSystemPrompt(context, planDetail);
            
            // 构建用户提示
            String userPrompt = buildUserPrompt(context, knowledgeChunks);
            
            // 保存构建的提示到上下文
            context.addExtensionProperty("systemPrompt", systemPrompt);
            context.addExtensionProperty("userPrompt", userPrompt);
            context.addExtensionProperty("promptLength", systemPrompt.length() + userPrompt.length());
            
            // 记录执行时间
            long duration = System.currentTimeMillis() - startTime;
            recordExecutionTime(context, STEP_NAME, duration);
            
            logger.info("Prompt构建完成 - 查询ID: {}, 提示长度: {}, 知识块数: {}", 
                    context.getQueryId(), systemPrompt.length() + userPrompt.length(), 
                    knowledgeChunks.size());
            
            return true;
            
        } catch (Exception e) {
            logger.error("Prompt构建步骤执行失败", e);
            return false;
        }
    }
    
    /**
     * 获取知识块
     */
    @SuppressWarnings("unchecked")
    private List<KnowledgeChunk> getKnowledgeChunks(AgentContext context) {
        Object chunksObj = context.getExtensionProperty("knowledgeChunks");
        if (chunksObj instanceof List) {
            return (List<KnowledgeChunk>) chunksObj;
        }
        return Collections.emptyList();
    }
    
    /**
     * 获取计划详情
     */
    private PlanDetail getPlanDetail(AgentContext context) {
        Object planDetailObj = context.getExtensionProperty("planDetail");
        if (planDetailObj instanceof PlanDetail) {
            return (PlanDetail) planDetailObj;
        }
        return null;
    }
    
    /**
     * 构建系统提示
     */
    private String buildSystemPrompt(AgentContext context, PlanDetail planDetail) {
        StringBuilder systemPrompt = new StringBuilder();
        
        // 基础系统提示
        systemPrompt.append(DEFAULT_SYSTEM_PROMPT);
        
        // 根据查询意图定制提示
        if (planDetail != null && planDetail.getQueryIntent() != null) {
            String intentSpecificPrompt = getIntentSpecificPrompt(planDetail.getQueryIntent());
            if (intentSpecificPrompt != null) {
                systemPrompt.append(intentSpecificPrompt).append("\n\n");
            }
        }
        
        // 添加用户偏好
        if (context.getUserPreferences() != null) {
            String preferencesPrompt = buildPreferencesPrompt(context.getUserPreferences());
            if (!preferencesPrompt.isEmpty()) {
                systemPrompt.append(preferencesPrompt).append("\n\n");
            }
        }
        
        return systemPrompt.toString();
    }
    
    /**
     * 根据查询意图获取特定提示
     */
    private String getIntentSpecificPrompt(String queryIntent) {
        switch (queryIntent) {
            case "QA":
                return "专注于提供准确、直接的问答，优先回答用户的具体问题。";
            case "SEARCH":
                return "提供全面的搜索结果总结，包括相关信息的多个方面。";
            case "REASONING":
                return "进行逻辑推理和分析，提供有理有据的结论。";
            case "CREATION":
                return "基于知识信息进行创意生成，保持原创性和实用性。";
            default:
                return null;
        }
    }
    
    /**
     * 构建用户偏好提示
     */
    private String buildPreferencesPrompt(Map<String, Object> userPreferences) {
        StringBuilder prefPrompt = new StringBuilder();
        
        // 语言偏好
        Object languageObj = userPreferences.get("language");
        if (languageObj != null) {
            prefPrompt.append("用户语言偏好：").append(languageObj).append("\n");
        }
        
        // 详细程度偏好
        Object detailLevelObj = userPreferences.get("detailLevel");
        if (detailLevelObj != null) {
            prefPrompt.append("回答详细程度：").append(detailLevelObj).append("\n");
        }
        
        // 输出格式偏好
        Object formatObj = userPreferences.get("outputFormat");
        if (formatObj != null) {
            prefPrompt.append("输出格式偏好：").append(formatObj).append("\n");
        }
        
        return prefPrompt.toString();
    }
    
    /**
     * 构建用户提示
     */
    private String buildUserPrompt(AgentContext context, List<KnowledgeChunk> knowledgeChunks) {
        StringBuilder userPrompt = new StringBuilder();
        
        // 添加知识信息部分
        if (!knowledgeChunks.isEmpty()) {
            userPrompt.append(KNOWLEDGE_SECTION_HEADER);
            userPrompt.append(buildKnowledgeSection(knowledgeChunks));
        }
        
        // 添加对话历史
        if (context.getConversationHistory() != null && !context.getConversationHistory().isEmpty()) {
            userPrompt.append("\n===== 对话历史 =====\n");
            userPrompt.append(buildConversationHistory(context.getConversationHistory()));
        }
        
        // 添加用户问题
        userPrompt.append(QUESTION_SECTION_HEADER);
        userPrompt.append(context.getCurrentQuery());
        
        // 添加回答指引
        userPrompt.append(ANSWER_SECTION_HEADER);
        userPrompt.append("请基于以上知识信息回答用户问题：");
        
        return userPrompt.toString();
    }
    
    /**
     * 构建知识信息部分
     */
    private String buildKnowledgeSection(List<KnowledgeChunk> knowledgeChunks) {
        StringBuilder knowledgeSection = new StringBuilder();
        
        // 对知识块进行分组和排序
        List<KnowledgeChunk> sortedChunks = knowledgeChunks.stream()
                .sorted((c1, c2) -> Double.compare(c2.getScore(), c1.getScore()))
                .collect(Collectors.toList());
        
        // 按来源类型分组
        Map<String, List<KnowledgeChunk>> groupedBySource = sortedChunks.stream()
                .collect(Collectors.groupingBy(
                    chunk -> chunk.getSourceType() != null ? chunk.getSourceType() : "UNKNOWN"
                ));
        
        int chunkIndex = 1;
        for (Map.Entry<String, List<KnowledgeChunk>> entry : groupedBySource.entrySet()) {
            String sourceType = entry.getKey();
            List<KnowledgeChunk> chunks = entry.getValue();
            
            knowledgeSection.append("## ").append(formatSourceType(sourceType)).append("\n");
            
            for (KnowledgeChunk chunk : chunks) {
                knowledgeSection.append("[").append(chunkIndex++).append("] ");
                
                // 添加摘要（如果有）
                if (chunk.getSummary() != null && !chunk.getSummary().isEmpty()) {
                    knowledgeSection.append("摘要：").append(chunk.getSummary()).append("\n");
                }
                
                // 添加内容
                String content = chunk.getContent();
                if (content != null) {
                    // 限制内容长度
                    if (content.length() > 500) {
                        content = content.substring(0, 500) + "...";
                    }
                    knowledgeSection.append("内容：").append(content).append("\n");
                }
                
                // 添加来源信息
                if (chunk.getSource() != null) {
                    knowledgeSection.append("来源：").append(chunk.getSource()).append("\n");
                }
                
                // 添加相关性得分
                knowledgeSection.append("相关性：").append(String.format("%.2f", chunk.getScore())).append("\n");
                
                knowledgeSection.append("\n");
            }
        }
        
        return knowledgeSection.toString();
    }
    
    /**
     * 格式化来源类型
     */
    private String formatSourceType(String sourceType) {
        switch (sourceType) {
            case "VECTOR_STORE":
                return "向量数据库";
            case "SQL_DATABASE":
                return "关系数据库";
            case "API_SOURCE":
                return "外部API";
            default:
                return sourceType;
        }
    }
    
    /**
     * 构建对话历史
     */
    private String buildConversationHistory(List<String> conversationHistory) {
        StringBuilder historySection = new StringBuilder();
        
        // 只保留最近的几轮对话
        int maxHistoryItems = 5;
        int startIndex = Math.max(0, conversationHistory.size() - maxHistoryItems);
        
        for (int i = startIndex; i < conversationHistory.size(); i++) {
            historySection.append(conversationHistory.get(i)).append("\n");
        }
        
        return historySection.toString();
    }
    
    @Override
    public String getStepName() {
        return STEP_NAME;
    }
    
    @Override
    public boolean checkPreconditions(AgentContext context) {
        return context.getCurrentQuery() != null && 
               !context.getCurrentQuery().trim().isEmpty();
    }
    
    @Override
    public boolean canSkip(AgentContext context) {
        // 检查是否已经有构建好的提示
        Object systemPrompt = context.getExtensionProperty("systemPrompt");
        Object userPrompt = context.getExtensionProperty("userPrompt");
        return systemPrompt != null && userPrompt != null;
    }
    
    @Override
    public void afterExecution(AgentContext context, boolean success) {
        if (success) {
            context.setProcessingStatus("PROMPT_CONSTRUCTED");
        } else {
            context.setProcessingStatus("PROMPT_CONSTRUCTION_FAILED");
        }
    }
} 