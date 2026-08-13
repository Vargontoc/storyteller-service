package es.vargontoc.storyteller.application;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class AgentsConfig {

    @Bean("topics-agent")
    public ChatClient topicAgent(OllamaChatModel ollamaModel, ToolCallback[] tools) {


        return ChatClient.builder(ollamaModel)
            .defaultAdvisors(new SimpleLoggerAdvisor())
            .defaultOptions(ChatOptions.builder().model("storyteller-topics"))
            .build();
    }
}
