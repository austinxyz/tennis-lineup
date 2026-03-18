package com.tennis.service;

import com.tennis.model.Lineup;
import com.tennis.model.Pair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class ZhipuAiService {

    private static final int TIMEOUT_SECONDS = 3;

    @Value("${zhipu.api.key:}")
    private String apiKey;

    /**
     * Select the best lineup from candidates using Zhipu AI.
     * Returns the index of the selected lineup, or -1 if AI is unavailable/failed.
     */
    public int selectBestLineup(List<Lineup> candidates, String strategy) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("Zhipu AI API key not configured, using fallback");
            return -1;
        }
        if (candidates.isEmpty()) return -1;

        try {
            String prompt = buildPrompt(candidates, strategy);
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(
                    () -> callAiApi(prompt, candidates.size()));
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Zhipu AI call timed out after {}s, using fallback", TIMEOUT_SECONDS);
        } catch (Exception e) {
            log.warn("Zhipu AI call failed: {}, using fallback", e.getMessage());
        }
        return -1;
    }

    String buildPrompt(List<Lineup> candidates, String strategy) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位网球双打教练。请根据以下策略选出最合适的排阵编号（从1开始）。\n\n");
        sb.append("策略：").append(strategy).append("\n\n");
        sb.append("候选排阵：\n");
        for (int i = 0; i < candidates.size(); i++) {
            Lineup lineup = candidates.get(i);
            sb.append(i + 1).append(". 总UTR=").append(String.format("%.1f", lineup.getTotalUtr())).append("\n");
            for (Pair pair : lineup.getPairs()) {
                sb.append("   ").append(pair.getPosition()).append(": ")
                  .append(pair.getPlayer1Name()).append(" + ").append(pair.getPlayer2Name())
                  .append(" (组合UTR=").append(String.format("%.1f", pair.getCombinedUtr())).append(")\n");
            }
        }
        sb.append("\n请只回复一个数字（排阵编号）。");
        return sb.toString();
    }

    private int callAiApi(String prompt, int candidateCount) {
        try {
            // Dynamically load the Zhipu AI SDK to avoid compile-time dependency issues
            Class<?> clientBuilderClass = Class.forName("com.zhipu.oapi.ClientV4$Builder");
            Object builder = clientBuilderClass.getConstructor(String.class).newInstance(apiKey);
            Object client = clientBuilderClass.getMethod("build").invoke(builder);

            // Build chat messages
            Class<?> chatMessageClass = Class.forName("com.zhipu.oapi.service.v4.model.ChatMessage");
            Class<?> chatMessageRoleClass = Class.forName("com.zhipu.oapi.service.v4.model.ChatMessageRole");
            Object userRole = chatMessageRoleClass.getField("user").get(null);
            Object message = chatMessageClass.getConstructor(String.class, String.class)
                    .newInstance(userRole.toString(), prompt);

            // Build request
            Class<?> requestBuilderClass = Class.forName("com.zhipu.oapi.service.v4.model.ChatCompletionRequest$ChatCompletionRequestBuilder");
            Object requestBuilder = Class.forName("com.zhipu.oapi.service.v4.model.ChatCompletionRequest")
                    .getMethod("builder").invoke(null);
            requestBuilderClass.getMethod("model", String.class).invoke(requestBuilder, "glm-4");
            requestBuilderClass.getMethod("messages", List.class).invoke(requestBuilder, List.of(message));
            Object request = requestBuilderClass.getMethod("build").invoke(requestBuilder);

            // Invoke API
            Object response = client.getClass().getMethod("invokeModelApi", request.getClass())
                    .invoke(client, request);

            return parseLineupResponse(response, candidateCount);
        } catch (Exception e) {
            throw new RuntimeException("AI API call failed: " + e.getMessage(), e);
        }
    }

    int parseLineupResponse(Object response, int candidateCount) {
        try {
            String content = extractContent(response);
            return parseIndexFromContent(content, candidateCount);
        } catch (Exception e) {
            log.warn("Could not parse AI response");
            return -1;
        }
    }

    int parseIndexFromContent(String content, int candidateCount) {
        if (content == null) return -1;
        try {
            int index = Integer.parseInt(content.trim()) - 1;
            if (index < 0 || index >= candidateCount) return -1;
            return index;
        } catch (NumberFormatException e) {
            log.warn("Could not parse AI response as lineup index: {}", content);
            return -1;
        }
    }

    private String extractContent(Object response) {
        try {
            Object data = response.getClass().getMethod("getData").invoke(response);
            if (data == null) return null;
            Object choices = data.getClass().getMethod("getChoices").invoke(data);
            if (choices == null) return null;
            Object firstChoice = ((List<?>) choices).get(0);
            Object message = firstChoice.getClass().getMethod("getMessage").invoke(firstChoice);
            return (String) message.getClass().getMethod("getContent").invoke(message);
        } catch (Exception e) {
            return null;
        }
    }
}
