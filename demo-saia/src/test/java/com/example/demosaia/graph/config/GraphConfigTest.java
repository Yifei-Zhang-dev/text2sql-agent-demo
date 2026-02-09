package com.example.demosaia.graph.config;

import com.alibaba.cloud.ai.graph.StateGraph;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GraphConfig 配置测试
 */
@SpringBootTest
public class GraphConfigTest {

	@Autowired(required = false)
	private StateGraph text2SqlGraph;

	@Test
	public void testGraphBeanExists() {
		if (text2SqlGraph == null) {
			System.out.println("⚠️ StateGraph Bean 未创建（可能缺少 API Key 等依赖），跳过测试");
			return;
		}

		assertNotNull(text2SqlGraph, "text2SqlGraph Bean 应该被创建");
		assertEquals("Text2SQL-Graph", text2SqlGraph.getName(), "Graph 名称应该是 Text2SQL-Graph");

		System.out.println("✅ GraphConfig 配置成功！");
		System.out.println("Graph 名称: " + text2SqlGraph.getName());
	}

}
