package com.example.agent.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring Boot Knowledge Agent 应用启动类
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.example.agent.core",
    "com.example.agent.knowledge", 
    "com.example.agent.pipeline",
    "com.example.agent.mcp",
    "com.example.agent.app"
})
@EnableAsync
@EnableTransactionManagement
public class AgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgentApplication.class, args);
	}

}
