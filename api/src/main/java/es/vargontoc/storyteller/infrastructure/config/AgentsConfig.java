package es.vargontoc.storyteller.infrastructure.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import es.vargontoc.storyteller.shared.Constants;


@Configuration
public class AgentsConfig {

    @Bean(name = Constants.BeanNames.AGENT_TOPICS_MODEL)
    public String topicModel(@Value("${app.agents.topics}") String value) { return value;}

    @Bean(name = Constants.BeanNames.AGENT_DIRECTOR_MODEL)
    public String directorModel(@Value("${app.agents.director}") String value) { return value;}

    @Bean(name = Constants.BeanNames.AGENT_SCRIPTWRITER_MODEL)
    public String scriptwriterModel(@Value("${app.agents.scriptwriter}") String value) { return value;}

    @Bean(name = Constants.BeanNames.AGENT_TRANSLATOR_MODEL)
    public String translatorModel(@Value("${app.agents.translator}") String value) { return value;}



    @Bean(name = Constants.BeanNames.AGENT_TOPICS)
    public ChatClient topicAgent(OllamaChatModel ollamaModel, @Qualifier(Constants.BeanNames.AGENT_TOPICS_MODEL) String model) {
        return ChatClient.builder(ollamaModel)
            .defaultAdvisors(new SimpleLoggerAdvisor())
            .defaultOptions(ChatOptions.builder().model(model))
            .build();
    }

    @Bean(name = Constants.BeanNames.AGENT_DIRECTOR)
    public ChatClient directorAgent(OllamaChatModel ollama, @Qualifier(Constants.BeanNames.AGENT_DIRECTOR_MODEL) String model) {
        return ChatClient.builder(ollama)
            .defaultAdvisors(new SimpleLoggerAdvisor())
            .defaultOptions(ChatOptions.builder().model(model))
            .build();
    }

    @Bean(name = Constants.BeanNames.AGENT_SCRIPTWRITER)
    public ChatClient scriptwriterAgent(OllamaChatModel ollama, @Qualifier(Constants.BeanNames.AGENT_SCRIPTWRITER_MODEL) String model) {
        return ChatClient.builder(ollama)
            .defaultAdvisors(new SimpleLoggerAdvisor())
            .defaultOptions(ChatOptions.builder().model(model))
            .build();
    }

    @Bean(name = Constants.BeanNames.AGENT_TRANSLATOR)
    public ChatClient translatorAgent(OllamaChatModel ollama,
            @Qualifier(Constants.BeanNames.AGENT_TRANSLATOR_MODEL) String model,
            @Value("classpath:/prompts/translate_scene.st") Resource systemPrompt) {
        return ChatClient.builder(ollama)
            .defaultAdvisors(new SimpleLoggerAdvisor())
            .defaultOptions(ChatOptions.builder().model(model))
            .defaultSystem(systemPrompt)
            .build();
    }
}
