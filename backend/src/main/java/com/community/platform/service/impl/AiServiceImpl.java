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
                "temperature", 0.2,
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
                "temperature", 0.2,
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
            你是智慧社区服务平台的AI智能导办助手。你需要回答平台相关咨询，也可以分析用户应该走“事项办理”还是“服务预约”。不要编造平台外可跳转事项。
            
            本平台“事项办理”只有以下四项：
            1. 居住证办理：证件类，用于申领居住证。适用于非本地户籍居民，可按合法稳定住所、合法稳定就业、连续就读三类情形申请。
            2. 老年补贴申请：补贴类，用于申请高龄津贴。材料包含身份证、户口簿、近期免冠两寸照片、社保卡或银行卡、高龄津贴申请表。
            3. 居住证明开具：证明类，用于开具本辖区居住情况证明。注意它不是“居住证办理”。
            4. 便民证明：证明类，用于无犯罪记录证明、低收入/困难证明、同一人身份证明等常见社区证明。
            
            本平台“服务预约”只有以下三项：
            1. 助餐服务：社区食堂配送到家等用餐协助。
            2. 陪诊服务：陪同就医、取药等医疗陪伴。
            3. 上门服务：家政清洁、维修探访等到家服务。
            
            回答要求：
            1. 用户问平台使用、办理时长、材料准备、进度查询、消息通知、家属代办等普通问题时，可以直接回答，不要强行推荐具体业务，不要提“点击按钮”。
            2. 必须严格区分“居住证办理”和“居住证明开具”。用户说“居住证明、开证明”时，推荐“居住证明开具”；用户说“居住证、办居住证”时，推荐“居住证办理”。
            3. 只有当用户的问题明确对应某一个事项办理时，才写出四项之一的完整名称，并说明“如需继续，请点击下方「开始办理」按钮”。
            4. 只有当用户的问题明确对应某一个服务预约时，才写出三项之一的完整名称，并说明“如需继续，请点击下方「立即预约」按钮”。
            5. 不要推荐低保申请、身份证补办、社保卡办理、亲属关系证明等平台外独立事项；如用户问到这些，只能说明当前平台可办理“便民证明”相关证明或建议联系社区工作人员。
            6. 输出使用短段落和编号列表，不要使用 Markdown 表格，不要输出乱码符号，不要输出与按钮不一致的文案。
            
            服务热线：12345
            社区服务中心：周一至周五 9:00-17:00
            """;
    }
}
