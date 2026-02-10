package com.example.demosaia.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.example.demosaia.graph.state.Text2SqlState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;

/**
 * Router Node - 路由节点
 * 职责：分析用户问题，判断查询类型（simple 或 complex）
 *
 * simple：简单查询（列表查询、单表统计）
 * complex：复杂查询（多表关联、聚合统计、需要表结构信息）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RouterNode implements Function<OverAllState, Map<String, Object>> {

	private final ChatClient chatClient;

	private static final String ROUTER_PROMPT = """
			你是一个查询分类专家。分析用户问题，判断查询类型。

			【分类规则】
			simple（简单查询）：
			- 列表查询：列出所有客户、显示订单、查看商品等
			- 单表统计：客户总数、订单总金额、最贵的产品等
			- 单表筛选：ID=5的客户、2024年的订单等

			complex（复杂查询）：
			- 多表关联：每个客户的订单数、各城市客户分布等
			- 聚合统计：按城市统计客户数、按月统计订单量等
			- 需要 JOIN、GROUP BY、或多个表的查询

			【输出格式】
			只返回一个单词：simple 或 complex
			不要解释，不要其他内容。

			【用户问题】
			{question}
			""";

	@Override
	public Map<String, Object> apply(OverAllState overAllState) {
		Text2SqlState state = Text2SqlState.fromMap(overAllState.data());

		log.info("[RouterNode] 开始分析问题: {}", state.getQuestion());
		state.addLog("[RouterNode] 开始问题分类");

		try {
			String queryType = chatClient.prompt()
				.user(userSpec -> userSpec.text(ROUTER_PROMPT.replace("{question}", state.getQuestion())))
				.call()
				.content()
				.trim()
				.toLowerCase();

			if (!queryType.equals("simple") && !queryType.equals("complex")) {
				log.warn("[RouterNode] LLM 返回了无效的查询类型: {}, 默认为 complex", queryType);
				queryType = "complex";
			}

			state.setQueryType(queryType);
			state.addLog("[RouterNode] 分类结果: " + queryType);

			log.info("[RouterNode] 分类完成: {}", queryType);

			return state.toMap();

		}
		catch (Exception e) {
			log.error("[RouterNode] 分类失败", e);
			state.addLog("[RouterNode] 分类失败: " + e.getMessage());
			state.recordError("RouterNode", classifyError(e), e.getMessage(),
					"路由分类失败，已默认使用复杂查询路径。如问题持续，请稍后重试。", true);

			state.setQueryType("complex");
			return state.toMap();
		}
	}

	private String classifyError(Exception e) {
		String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
		if (msg.contains("timeout") || msg.contains("connect")) return "NETWORK_ERROR";
		return "LLM_ERROR";
	}

}
