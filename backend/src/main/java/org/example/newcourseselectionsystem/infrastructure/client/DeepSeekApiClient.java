package org.example.newcourseselectionsystem.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek API 客户端
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeepSeekApiClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${deepseek.api.key:}")
    private String apiKey;

    private static final String BASE_URL = "https://api.deepseek.com/chat/completions";

    /**
     * 调用 DeepSeek API 获取流式推荐结果
     *
     * @param messages 消息列表
     * @return AI 回复内容流
     */
    public Flux<String> chatStream(List<Message> messages) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", messages);
            requestBody.put("stream", true);

            WebClient webClient = webClientBuilder
                    .baseUrl(BASE_URL)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();

            return webClient.post()
                    .uri(BASE_URL)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(DataBuffer.class)
                    .timeout(Duration.ofMinutes(5))
                    .map(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        return new String(bytes, StandardCharsets.UTF_8);
                    })
                    .flatMap(this::parseSSELines)
                    .filter(data -> data != null && !data.trim().isEmpty())
                    .filter(data -> !data.equals("data: [DONE]"))
                    .map(this::parseStreamData)
                    .filter(content -> content != null && !content.isEmpty())
                    .doOnError(error -> log.error("调用 DeepSeek API 失败", error))
                    .onErrorResume(error -> {
                        log.error("DeepSeek API 流式调用失败", error);
                        return Flux.just("错误: " + error.getMessage());
                    });
        } catch (Exception e) {
            log.error("调用 DeepSeek API 失败", e);
            return Flux.just("错误: " + e.getMessage());
        }
    }

    /**
     * 解析 SSE 格式的多行数据
     */
    private Flux<String> parseSSELines(String data) {
        String[] lines = data.split("\n");
        return Flux.fromArray(lines)
                .filter(line -> line.startsWith("data: "));
    }

    /**
     * 解析流式数据
     */
    private String parseStreamData(String data) {
        try {
            // SSE 格式: "data: {...}"
            if (data.startsWith("data: ")) {
                String jsonStr = data.substring(6).trim();
                if (jsonStr.equals("[DONE]")) {
                    return null;
                }
                JsonNode jsonNode = objectMapper.readTree(jsonStr);
                JsonNode choices = jsonNode.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode choice = choices.get(0);
                    JsonNode delta = choice.get("delta");
                    if (delta != null) {
                        JsonNode content = delta.get("content");
                        if (content != null && content.isTextual()) {
                            return content.asText();
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("解析流式数据失败: {}", data, e);
            return null;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    public static class DeepSeekResponse {
        private List<Choice> choices;
    }

    @Data
    public static class Choice {
        private Message message;
    }
}
