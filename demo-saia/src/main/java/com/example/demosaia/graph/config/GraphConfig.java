package com.example.demosaia.graph.config;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.example.demosaia.graph.nodes.*;
import com.example.demosaia.graph.state.StateStrategyFactory;
import com.example.demosaia.graph.state.Text2SqlState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Graph 配置类
 * 定义 Text2SQL Graph 的节点连接关系和执行流程
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class GraphConfig {

	private final RouterNode routerNode;

	private final SchemaRetrievalNode schemaRetrievalNode;

	private final SimpleSqlGeneratorNode simpleSqlGeneratorNode;

	private final ComplexSqlGeneratorNode complexSqlGeneratorNode;

	private final SqlValidatorNode sqlValidatorNode;

	private final RendererNode rendererNode;

	/**
	 * 创建 Text2SQL StateGraph Bean
	 */
	@Bean(name = "text2SqlGraph")
	public StateGraph text2SqlGraph() throws Exception {
		log.info("初始化 Text2SQL StateGraph...");

		StateGraph graph = new StateGraph("Text2SQL-Graph", StateStrategyFactory.createText2SqlStateFactory());

		// === 添加节点 ===
		graph.addNode("router", AsyncNodeAction.node_async(routerNode::apply));
		graph.addNode("schemaRetrieval", AsyncNodeAction.node_async(schemaRetrievalNode::apply));
		graph.addNode("simpleSqlGen", AsyncNodeAction.node_async(simpleSqlGeneratorNode::apply));
		graph.addNode("complexSqlGen", AsyncNodeAction.node_async(complexSqlGeneratorNode::apply));
		graph.addNode("validator", AsyncNodeAction.node_async(sqlValidatorNode::apply));
		graph.addNode("renderer", AsyncNodeAction.node_async(rendererNode::apply));

		// === 定义边 ===

		// START -> router
		graph.addEdge(StateGraph.START, "router");

		// 条件路由：router -> simple/complex 路径
		graph.addConditionalEdges("router", AsyncEdgeAction.edge_async(this::routeByQueryType),
				createRouterEdges());

		// simple 路径：simpleSqlGen -> validator
		graph.addEdge("simpleSqlGen", "validator");

		// complex 路径：schemaRetrieval -> complexSqlGen -> validator
		graph.addEdge("schemaRetrieval", "complexSqlGen");
		graph.addEdge("complexSqlGen", "validator");

		// 汇合：validator -> renderer -> END
		graph.addEdge("validator", "renderer");
		graph.addEdge("renderer", StateGraph.END);

		log.info("Text2SQL StateGraph 初始化完成");
		return graph;
	}

	/**
	 * 路由分发：根据 queryType 决定走 simple 还是 complex 路径
	 */
	private String routeByQueryType(OverAllState state) {
		Text2SqlState text2SqlState = Text2SqlState.fromMap(state.data());
		String queryType = text2SqlState.getQueryType();

		log.info("[RouterDispatcher] 查询类型: {}, 路由到: {}", queryType,
				"simple".equals(queryType) ? "simpleSqlGen" : "schemaRetrieval");

		return queryType != null ? queryType : "complex";
	}

	/**
	 * 路由边映射：queryType 值 -> 目标节点名
	 */
	private Map<String, String> createRouterEdges() {
		Map<String, String> edges = new HashMap<>();
		edges.put("simple", "simpleSqlGen");
		edges.put("complex", "schemaRetrieval");
		return edges;
	}

}
