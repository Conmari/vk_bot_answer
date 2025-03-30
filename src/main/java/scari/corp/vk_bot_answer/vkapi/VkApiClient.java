package scari.corp.vk_bot_answer.vkapi;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class VkApiClient {

     private static final Logger LOGGER = LoggerFactory.getLogger(VkApiClient.class);

    // Константы для API-методов
    private static final String VK_API_BASE_URL = "https://api.vk.com/method/";
    private static final String MESSAGES_SEND_METHOD = "messages.send";
    private static final String GROUPS_GET_LONG_POLL_SERVER_METHOD = "groups.getLongPollServer";
    private static final String VK_API_VERSION = "5.131";

    // Константы для Long Poll
    private static final String LONG_POLL_ACT = "a_check";
    private static final String LONG_POLL_KEY_PARAM = "key";
    private static final String LONG_POLL_TS_PARAM = "ts";
    private static final String LONG_POLL_WAIT_PARAM = "wait";
    private static final int LONG_POLL_WAIT_TIME = 25;   


    private static final String UTF_8 = StandardCharsets.UTF_8.toString();
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendMessage(int userId, String message, String accessToken) {
        String url = buildSendMessageUrl(userId, message, accessToken);
        
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {            
                // LOGGER.info("Пользователю {}. направлен ответ: {}", userId, response.body());            
            } else {
                LOGGER.error("Не удалось направить сообщение пользователю {}. Статус: {}, Ответ: {}", userId, response.statusCode(), response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildSendMessageUrl(int userId, String message, String accessToken) {
        try {
            String encodedMessage = URLEncoder.encode(message, UTF_8);
            return String.format("%s%s?user_id=%d&message=%s&random_id=%d&access_token=%s&v=%s",
                    VK_API_BASE_URL, MESSAGES_SEND_METHOD, userId, encodedMessage, System.currentTimeMillis(), accessToken, VK_API_VERSION);
        } catch (Exception e) {
            LOGGER.error("Ошибка в buildSendMessageUrl: {}", e.getMessage(), e);
            return null; 
        }
    }

    public JsonNode getLongPollServer(int groupId, String accessToken) {
        String url = buildGetLongPollServerUrl(groupId, accessToken);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                LOGGER.debug("getLongPollServer: {}", jsonResponse); 
                return jsonResponse;
            } else {
                LOGGER.error("Неудалось получить данные в get getLongPollServer. Статус : {}, Ответ : {}", response.statusCode(), response.body());
                return null;
            }
        } catch (IOException e) {
            LOGGER.error("Ошибка получения из  getLongPollServer:: {}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            LOGGER.error("Ошибка в getLongPollServer: {}", e.getMessage(), e);
            return null;
        }
    }

    private String buildGetLongPollServerUrl(int groupId, String accessToken) {
        return String.format("%s%s?group_id=%d&access_token=%s&v=%s",
                VK_API_BASE_URL, GROUPS_GET_LONG_POLL_SERVER_METHOD, groupId, accessToken, VK_API_VERSION);
    }

    public JsonNode getLongPollEvents(String server, String key, String ts) {
        String url = String.format("%s?act=%s&%s=%s&%s=%s&%s=%d",
                server, LONG_POLL_ACT, LONG_POLL_KEY_PARAM, key, LONG_POLL_TS_PARAM, ts, LONG_POLL_WAIT_PARAM, LONG_POLL_WAIT_TIME);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                // LOGGER.debug("getLongPollEvents : {}", jsonResponse); 
                return jsonResponse;
            } else {
                LOGGER.error("Неудалось получить данные в get getLongPollEvents. Статус : {}, Ответ : {}", response.statusCode(), response.body());
                return null;
            }
        } catch (IOException e) {
            LOGGER.error("Ошибка получения из  getLongPollEvents: {}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            LOGGER.error("Ошибка в getLongPollEvents: {}", e.getMessage(), e);
            return null;
        }
    }
}
