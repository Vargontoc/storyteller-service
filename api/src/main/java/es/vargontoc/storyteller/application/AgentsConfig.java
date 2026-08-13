package es.vargontoc.storyteller.application;

import java.util.Arrays;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import es.vargontoc.storyteller.infrastructure.tools.TopicTools;

@Configuration
public class AgentsConfig {

    @Bean
    public ToolCallback[] topicToolCallbacks(TopicTools topicTools) {
        return ToolCallbacks.from(topicTools);
    }

    @Bean("topics-agent")
    public ChatClient topicAgent(OllamaChatModel ollamaModel, ToolCallback[] tools) {
        List<ToolCallback> filtered = Arrays.stream(tools)
            .filter(t -> t.getToolDefinition().name().equals("create_topic") ||
                t.getToolDefinition().name().equals("get_topics"))
            .toList();

        return ChatClient.builder(ollamaModel)
            .defaultAdvisors(new SimpleLoggerAdvisor())
            .defaultOptions(ChatOptions.builder().model("storyteller-topics"))
            .defaultTools(filtered.toArray()).build();
    }
}
