package com.example.demosaia;

import com.alibaba.cloud.ai.graph.StateGraph;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 验证 Spring AI Alibaba Graph 依赖是否正确引入
 */
public class GraphDependencyTest {

    @Test
    public void testGraphClassExists() {
        // 验证 StateGraph 类可以被加载
        Class<?> clazz = StateGraph.class;
        assertNotNull(clazz, "StateGraph class should be available");
        System.out.println("✅ Spring AI Alibaba Graph dependency loaded successfully!");
    }
}
