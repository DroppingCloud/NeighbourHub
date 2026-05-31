package com.community.platform.service;

import com.community.platform.dto.guide.ChatMessageDTO;
import com.community.platform.dto.guide.GuideRequestDTO;
import com.community.platform.vo.guide.GuideResultVO;

public interface GuideService {
    GuideResultVO recommend(GuideRequestDTO dto);
    String chat(ChatMessageDTO dto);
    
    /**
     * 流式对话，将 AI 返回的分片通过回调传出
     */
    void chatStream(ChatMessageDTO dto, java.util.function.Consumer<String> onToken);
}
