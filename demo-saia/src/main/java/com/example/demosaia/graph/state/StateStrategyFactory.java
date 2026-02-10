package com.example.demosaia.graph.state;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateFactory;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.Map;

/**
 * 状态合并策略工厂
 * 定义每个状态字段的合并规则
 */
public class StateStrategyFactory {

    /**
     * 创建 Text2SQL Graph 的状态合并策略
     *
     * @return OverAllStateFactory 用于 StateGraph 构建
     */
    public static OverAllStateFactory createText2SqlStateFactory() {
        return () -> {
            Map<String, KeyStrategy> strategies = new HashMap<>();

            // 输入数据：替换策略
            strategies.put("question", new ReplaceStrategy());
            strategies.put("paginationParam", new ReplaceStrategy());

            // 节点输出：替换策略（每个节点只设置一次）
            strategies.put("queryType", new ReplaceStrategy());
            strategies.put("schema", new ReplaceStrategy());
            strategies.put("sql", new ReplaceStrategy());
            strategies.put("validatedSql", new ReplaceStrategy());
            strategies.put("isValid", new ReplaceStrategy());
            strategies.put("validationError", new ReplaceStrategy());
            strategies.put("scriptCode", new ReplaceStrategy());
            strategies.put("explanation", new ReplaceStrategy());
            strategies.put("componentType", new ReplaceStrategy());

            // 错误追踪：替换策略
            strategies.put("errorNode", new ReplaceStrategy());
            strategies.put("errorType", new ReplaceStrategy());
            strategies.put("errorDetail", new ReplaceStrategy());
            strategies.put("errorSuggestion", new ReplaceStrategy());
            strategies.put("errorRetryable", new ReplaceStrategy());

            // 日志：追加策略（累积所有节点的日志）
            strategies.put("executionLog", new AppendStrategy());

            OverAllState state = new OverAllState();
            state.registerKeyAndStrategy(strategies);
            return state;
        };
    }
}
