package com.community.platform.service.impl;

import com.community.platform.service.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.api.url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${ai.model:deepseek-chat}")
    private String model;

    @Value("${ai.max-tokens:2000}")
    private Integer maxTokens;

    @Value("${ai.timeout:30000}")
    private Integer timeout;

    @Override
    public String ask(String prompt) {
        return chat(prompt, null);
    }
    
    @Override
    public String chat(String message, List<ChatHistory> history) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("AI API key not configured");
            return null;
        }
        
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            
            messages.add(Map.of(
                "role", "system",
                "content", buildSystemPrompt()
            ));
            
            if (history != null && !history.isEmpty()) {
                for (ChatHistory h : history) {
                    messages.add(Map.of("role", h.role(), "content", h.content()));
                }
            }
            
            messages.add(Map.of("role", "user", "content", message));
            
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", messages,
                "max_tokens", maxTokens,
                "temperature", 0.7,
                "stream", false
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getBody() != null && response.getBody().containsKey("choices")) {
                Object choicesObj = response.getBody().get("choices");
                if (choicesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) choicesObj;
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> choice = choices.get(0);
                        Object messageObj = choice.get("message");
                        if (messageObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> messageMap = (Map<String, Object>) messageObj;
                            if (messageMap.containsKey("content")) {
                                String content = (String) messageMap.get("content");
                                log.info("AI response: {}", content);
                                return content;
                            }
                        }
                    }
                }
            }
            
            log.warn("Unexpected AI response format: {}", response.getBody());
            return null;
            
        } catch (RestClientException e) {
            log.error("Failed to call AI API: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void chatStream(String message, List<ChatHistory> history, Consumer<String> onToken) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("AI API key not configured for streaming");
            onToken.accept("[DONE]");
            return;
        }

        try {
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", buildSystemPrompt()));
            if (history != null && !history.isEmpty()) {
                for (ChatHistory h : history) {
                    messages.add(Map.of("role", h.role(), "content", h.content()));
                }
            }
            messages.add(Map.of("role", "user", "content", message));

            Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", messages,
                "max_tokens", maxTokens,
                "temperature", 0.7,
                "stream", true
            );

            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            String body = objectMapper.writeValueAsString(requestBody);
            conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String dataLine = line;
                    if (dataLine.startsWith("data:")) {
                        dataLine = dataLine.substring(5).trim();
                    }
                    if ("[DONE]".equals(dataLine)) {
                        onToken.accept("[DONE]");
                        break;
                    }
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> chunk = objectMapper.readValue(dataLine, Map.class);
                        String token = null;
                        if (chunk.containsKey("choices")) {
                            Object choicesObj = chunk.get("choices");
                            if (choicesObj instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> choices = (List<Map<String, Object>>) choicesObj;
                                if (choices != null && !choices.isEmpty()) {
                                    Map<String, Object> first = choices.get(0);
                                    Object delta = first.get("delta");
                                    if (delta instanceof Map) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> deltaMap = (Map<String, Object>) delta;
                                        if (deltaMap.get("content") != null) {
                                            token = (String) deltaMap.get("content");
                                        }
                                    } else if (first.get("text") != null) {
                                        token = (String) first.get("text");
                                    } else if (first.get("message") instanceof Map) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> msg = (Map<String, Object>) first.get("message");
                                        if (msg.get("content") != null) {
                                            token = (String) msg.get("content");
                                        }
                                    }
                                }
                            }
                        } else if (chunk.get("text") != null) {
                            token = (String) chunk.get("text");
                        }

                        if (token != null) {
                            try {
                                token = token.replaceAll("(?m)^[Dd]ata:\\s*", "");
                            } catch (Exception ignored) {}
                            onToken.accept(token);
                        } else {
                            String raw = dataLine;
                            try {
                                raw = raw.replaceAll("(?m)^[Dd]ata:\\s*", "");
                            } catch (Exception ignored) {}
                            onToken.accept(raw);
                        }
                    } catch (Exception ex) {
                        onToken.accept(dataLine);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Streaming AI chat error: {}", e.getMessage(), e);
            onToken.accept("[DONE]");
        }
    }
    
    private String buildSystemPrompt() {
        return """
            你是智慧社区AI导办助手，专门为社区居民提供政务服务咨询和办事指导。
            
            你的职责：
            1. 帮助居民了解各类政务事项的办理条件、所需材料、办理流程
            2. 推荐适合的政务服务事项，如：证明开具、补贴申请、证件办理、社区服务等
            3. 解答办事过程中的常见问题
            
            常见事项分类：
            - 证明类：居住证明、亲属关系证明、无犯罪记录证明、低收入证明等
            - 补贴类：高龄津贴、低保申请、残疾人补贴、育儿补贴等
            - 证件类：居住证办理、身份证补办、社保卡办理等
            - 服务类：助餐服务、陪诊服务、上门服务等
            
            回答要求：
            1. 回答要简洁明了，使用通俗易懂的语言
            2. 如果是老年人，回答要更详细、更有耐心
            3. 提供具体的办理步骤和材料清单
            4. 如果用户的问题涉及具体事项，建议用户点击“开始办理”按钮
            5. 如果不确定答案，建议用户联系社区工作人员
            
            服务热线：12345
            社区服务中心：周一至周五 9:00-17:00
            """;
    }
}