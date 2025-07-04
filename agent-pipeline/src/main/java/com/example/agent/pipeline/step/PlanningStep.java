package com.example.agent.pipeline.step;

import com.example.agent.core.dto.AgentContext;
import com.example.agent.core.dto.PlanDetail;
import com.example.agent.pipeline.template.AbstractPipelineStep;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 查询规划步骤
 * 负责分析用户查询，确定处理策略和参数
 * 
 * @author agent
 */
@Component
public class PlanningStep extends AbstractPipelineStep {
    
    private static final String STEP_NAME = "PLANNING";
    
    // 查询意图分类的正则表达式
    private static final Map<String, Pattern> INTENT_PATTERNS = new HashMap<>();
    
    static {
        // 问答类查询
        INTENT_PATTERNS.put("QA", Pattern.compile(".*[？?].*|^(什么|怎么|如何|为什么|where|what|how|why).*", Pattern.CASE_INSENSITIVE));
        // 搜索类查询
        INTENT_PATTERNS.put("SEARCH", Pattern.compile(".*搜索.*|.*查找.*|.*find.*|.*search.*", Pattern.CASE_INSENSITIVE));
        // 推理类查询
        INTENT_PATTERNS.put("REASONING", Pattern.compile(".*分析.*|.*推理.*|.*比较.*|.*analyze.*|.*compare.*", Pattern.CASE_INSENSITIVE));
        // 创作类查询
        INTENT_PATTERNS.put("CREATION", Pattern.compile(".*生成.*|.*创建.*|.*写.*|.*generate.*|.*create.*|.*write.*", Pattern.CASE_INSENSITIVE));
    }
    
    @Override
    protected boolean doExecute(AgentContext context) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 验证必要的上下文信息
            if (!validateContext(context, "query")) {
                return false;
            }
            
            // 创建计划详情
            PlanDetail planDetail = createPlanDetail(context);
            
            // 分析查询意图
            analyzeQueryIntent(context, planDetail);
            
            // 选择知识源策略
            selectKnowledgeSourceStrategies(context, planDetail);
            
            // 设置检索参数
            configureRetrievalParameters(context, planDetail);
            
            // 确定处理步骤序列
            determineProcessingSteps(context, planDetail);
            
            // 将计划详情保存到上下文
            context.addExtensionProperty("planDetail", planDetail);
            context.setQueryIntent(planDetail.getQueryIntent());
            
            // 记录执行时间
            long duration = System.currentTimeMillis() - startTime;
            recordExecutionTime(context, STEP_NAME, duration);
            
            logger.info("查询规划完成 - 查询ID: {}, 意图: {}, 策略: {}", 
                    context.getQueryId(), planDetail.getQueryIntent(), planDetail.getKnowledgeSourceStrategies());
            
            return true;
            
        } catch (Exception e) {
            logger.error("查询规划步骤执行失败", e);
            return false;
        }
    }
    
    /**
     * 创建计划详情
     */
    private PlanDetail createPlanDetail(AgentContext context) {
        String planId = context.getQueryId() + "_plan";
        PlanDetail planDetail = new PlanDetail(planId, "", "");
        
        // 初始化查询分析
        planDetail.setQueryAnalysis(analyzeQuery(context.getCurrentQuery()));
        
        return planDetail;
    }
    
    /**
     * 分析查询内容
     */
    private String analyzeQuery(String query) {
        StringBuilder analysis = new StringBuilder();
        
        // 查询长度分析
        analysis.append("查询长度: ").append(query.length()).append("字符; ");
        
        // 查询复杂度分析
        if (query.contains("和") || query.contains("或") || query.contains("以及")) {
            analysis.append("复合查询; ");
        }
        
        // 关键词提取
        String[] keywords = extractKeywords(query);
        analysis.append("关键词: ").append(Arrays.toString(keywords)).append("; ");
        
        return analysis.toString();
    }
    
    /**
     * 分析查询意图
     */
    private void analyzeQueryIntent(AgentContext context, PlanDetail planDetail) {
        String query = context.getCurrentQuery();
        String intent = "GENERAL";
        
        // 使用正则表达式匹配意图
        for (Map.Entry<String, Pattern> entry : INTENT_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(query).find()) {
                intent = entry.getKey();
                break;
            }
        }
        
        planDetail.setQueryIntent(intent);
        planDetail.setQueryType(determineQueryType(intent));
    }
    
    /**
     * 确定查询类型
     */
    private String determineQueryType(String intent) {
        switch (intent) {
            case "QA":
                return "QUESTION_ANSWERING";
            case "SEARCH":
                return "INFORMATION_RETRIEVAL";
            case "REASONING":
                return "ANALYTICAL_REASONING";
            case "CREATION":
                return "CONTENT_GENERATION";
            default:
                return "GENERAL_QUERY";
        }
    }
    
    /**
     * 选择知识源策略
     */
    private void selectKnowledgeSourceStrategies(AgentContext context, PlanDetail planDetail) {
        List<String> strategies = new ArrayList<>();
        
        // 根据查询意图选择策略
        String intent = planDetail.getQueryIntent();
        switch (intent) {
            case "QA":
                strategies.add("VECTOR_STORE");
                strategies.add("SQL_DATABASE");
                break;
            case "SEARCH":
                strategies.add("VECTOR_STORE");
                strategies.add("API_SOURCE");
                break;
            case "REASONING":
                strategies.add("VECTOR_STORE");
                strategies.add("SQL_DATABASE");
                break;
            case "CREATION":
                strategies.add("VECTOR_STORE");
                break;
            default:
                strategies.add("VECTOR_STORE");
        }
        
        // 考虑用户偏好
        if (context.getPreferredSources() != null && !context.getPreferredSources().isEmpty()) {
            strategies.retainAll(context.getPreferredSources());
        }
        
        planDetail.setKnowledgeSourceStrategies(strategies);
    }
    
    /**
     * 配置检索参数
     */
    private void configureRetrievalParameters(AgentContext context, PlanDetail planDetail) {
        Map<String, Object> params = new HashMap<>();
        
        // 基础参数
        params.put("maxResults", getConfigParameter(context, "maxResults", 10));
        params.put("similarityThreshold", getConfigParameter(context, "similarityThreshold", 0.7));
        params.put("timeout", getConfigParameter(context, "timeout", 30000));
        
        // 根据查询意图调整参数
        String intent = planDetail.getQueryIntent();
        switch (intent) {
            case "QA":
                params.put("maxResults", 5);
                params.put("similarityThreshold", 0.8);
                break;
            case "SEARCH":
                params.put("maxResults", 20);
                params.put("similarityThreshold", 0.6);
                break;
            case "REASONING":
                params.put("maxResults", 15);
                params.put("similarityThreshold", 0.7);
                break;
        }
        
        planDetail.setRetrievalParameters(params);
        
        // 更新上下文中的检索参数
        if (context.getRetrievalParams() == null) {
            context.setRetrievalParams(new HashMap<>());
        }
        context.getRetrievalParams().putAll(params);
    }
    
    /**
     * 确定处理步骤序列
     */
    private void determineProcessingSteps(AgentContext context, PlanDetail planDetail) {
        List<String> steps = new ArrayList<>();
        
        // 基础步骤序列
        steps.add("PLANNING");
        steps.add("KNOWLEDGE_RECALL");
        steps.add("PROMPT_CONSTRUCTION");
        steps.add("MODEL_INVOCATION");
        
        // 根据查询复杂度调整步骤
        String query = context.getCurrentQuery();
        if (query.length() > 100 || query.contains("和") || query.contains("或")) {
            planDetail.setEstimatedDuration(10000); // 10秒
        } else {
            planDetail.setEstimatedDuration(5000); // 5秒
        }
        
        planDetail.setExpectedSteps(steps);
    }
    
    /**
     * 提取关键词
     */
    private String[] extractKeywords(String query) {
        // 简单的关键词提取，实际应用中可以使用更复杂的NLP技术
        String[] words = query.replaceAll("[^\\w\\s]", "").split("\\s+");
        return Arrays.stream(words)
                .filter(word -> word.length() > 1)
                .distinct()
                .toArray(String[]::new);
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
        // 规划步骤通常不能跳过
        return false;
    }
    
    @Override
    public void afterExecution(AgentContext context, boolean success) {
        if (success) {
            context.setProcessingStatus("PLANNED");
        } else {
            context.setProcessingStatus("PLANNING_FAILED");
        }
    }
} 