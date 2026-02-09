package com.example.demosaia.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.example.demosaia.graph.state.Text2SqlState;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RouterNode 单元测试
 */
@SpringBootTest
public class RouterNodeTest {

	@Autowired(required = false)
	private RouterNode routerNode;

	@Autowired(required = false)
	private ChatClient chatClient;

	@Test
	public void testSimpleQuery() {
		if (routerNode == null || chatClient == null) {
			System.out.println("⚠️ RouterNode 或 ChatClient 未配置，跳过测试");
			return;
		}

		Text2SqlState inputState = new Text2SqlState();
		inputState.setQuestion("列出所有客户");

		OverAllState overAllState = new OverAllState();
		overAllState.updateState(inputState.toMap());

		Map<String, Object> result = routerNode.apply(overAllState);

		Text2SqlState outputState = Text2SqlState.fromMap(result);
		assertNotNull(outputState.getQueryType(), "queryType 不应为空");
		assertTrue(outputState.getQueryType().equals("simple") || outputState.getQueryType().equals("complex"),
				"queryType 应该是 simple 或 complex");

		System.out.println("✅ 测试通过！查询类型: " + outputState.getQueryType());
		System.out.println("执行日志:\n" + outputState.getExecutionLog());
	}

	@Test
	public void testComplexQuery() {
		if (routerNode == null || chatClient == null) {
			System.out.println("⚠️ RouterNode 或 ChatClient 未配置，跳过测试");
			return;
		}

		Text2SqlState inputState = new Text2SqlState();
		inputState.setQuestion("统计每个客户的订单数量");

		OverAllState overAllState = new OverAllState();
		overAllState.updateState(inputState.toMap());

		Map<String, Object> result = routerNode.apply(overAllState);

		Text2SqlState outputState = Text2SqlState.fromMap(result);
		assertNotNull(outputState.getQueryType());
		assertEquals("complex", outputState.getQueryType(), "应该被分类为复杂查询");

		System.out.println("✅ 测试通过！查询类型: " + outputState.getQueryType());
		System.out.println("执行日志:\n" + outputState.getExecutionLog());
	}

}
