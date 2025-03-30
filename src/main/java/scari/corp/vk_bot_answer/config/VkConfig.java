package scari.corp.vk_bot_answer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@Data
public class VkConfig {

    @Value("${vk.token}")
    private String token;

    @Value("${vk.group-id}")
    private int groupId;

    @Value("${long-poll.server}")
    private String longPollServer;

    @Value("${long-poll.key}")
    private String longPollKey;

    @Value("${long-poll.ts}")
    private String longPollTs;
}

