package com.community.platform.dto.guide;

import lombok.Data;

/**
 * AI 对话消息 DTO
 */
@Data
public class ChatMessageDTO {

    /** 用户输入的消息内容 */
    private String message;

    /** 会话 ID（用于多轮对话上下文） */
    private String sessionId;
}
