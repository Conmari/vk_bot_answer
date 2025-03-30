package scari.corp.vk_bot_answer.service;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import scari.corp.vk_bot_answer.config.VkConfig;
import scari.corp.vk_bot_answer.vkapi.VkApiClient;

@Service
public class BotService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotService.class);
    private final ExecutorService messageHandlingExecutor = Executors.newFixedThreadPool(10); 


    private final VkConfig vkConfig;
    private final VkApiClient vkApiClient;

    public BotService(VkConfig vkConfig, VkApiClient vkApiClient) {
        this.vkConfig = vkConfig;
        this.vkApiClient = vkApiClient;
    }

    @PostConstruct
    public void startPolling() {
        try {
            JsonNode longPollServerResponse = vkApiClient.getLongPollServer(vkConfig.getGroupId(), vkConfig.getToken());
            if (longPollServerResponse != null && longPollServerResponse.has("response")) {
                vkConfig.setLongPollServer(longPollServerResponse.get("response").get("server").asText());
                vkConfig.setLongPollKey(longPollServerResponse.get("response").get("key").asText());
                vkConfig.setLongPollTs(longPollServerResponse.get("response").get("ts").asText());

                LOGGER.info("Бот запустился");
                pollForNewMessages();
            } else {
                LOGGER.error("Ошибка инициализации Long Poll Server. {}", longPollServerResponse); //
            }
        } catch (Exception e) {
            LOGGER.error("Ошибка в классе startPolling: ", e);
        }
    }

    private void pollForNewMessages() {
        while (true) {
            try {
                JsonNode eventsResponse = vkApiClient.getLongPollEvents(vkConfig.getLongPollServer(), vkConfig.getLongPollKey(), vkConfig.getLongPollTs());

                if (eventsResponse != null && eventsResponse.has("updates")) {
                    JsonNode updates = eventsResponse.get("updates");
                    
                    if (updates.isArray()) {
                        for (JsonNode update : updates) {
                            if (update.has("type") && update.get("type").asText().equals("message_new")) {
                            messageHandlingExecutor.submit(() -> handleNewMessage(update));
                            // handleNewMessage(update);
                            }
                        }
                        vkConfig.setLongPollTs(eventsResponse.get("ts").asText());
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Ошибка в классе pollForNewMessages: ", e);
            }
        }
    
    }

    private void handleNewMessage(JsonNode update) {
        try {
            JsonNode message = update.get("object").get("message");
            int userId = message.get("from_id").asInt();
            String textAnswer = message.get("text").asText();

            String quotedText = textAnswer; 
            vkApiClient.sendMessage(userId, quotedText, vkConfig.getToken());
        } catch (Exception e) {
            LOGGER.error("Ошибка в классе handleNewMessage: ", e);
        }
    }

    @PreDestroy
    public void shutdownExecutor() {
        LOGGER.info("завершение потоков в shutdownExecutor");
        messageHandlingExecutor.shutdown();
        try {
            if (!messageHandlingExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                LOGGER.warn("Время завершения превысило 60 сек.");
                messageHandlingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOGGER.error("shutdownExecutor возникла ошибка", e);
            messageHandlingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.info("shutdownExecutor завершился успешно.");
    }

}
