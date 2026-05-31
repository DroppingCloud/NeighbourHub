package com.community.platform.service;

import java.util.List;

public interface AiService {
    /**
     * 发送提示词到AI API并返回文本响应
     */
    String ask(String prompt);
    
    /**
     * 带上下文的对话
     * @param message 当前消息
     * @param history 历史对话记录
     */
    String chat(String message, List<ChatHistory> history);
    
    /**
     * 对话历史记录
     */
    record ChatHistory(String role, String content) {}

    /**
     * 流式对话：逐步接收 AI 返回的增量文本，通过回调传出每个分片
     * @param message 当前消息
     * @param history 历史对话记录
     * @param onToken 每次接收到文本分片时的回调（传入分片文本），在流结束时会回调一次 "[DONE]"
     */
    void chatStream(String message, List<ChatHistory> history, java.util.function.Consumer<String> onToken);
}