package com.community.platform.service.impl;

import com.community.platform.dto.guide.ChatMessageDTO;
import com.community.platform.dto.guide.GuideRequestDTO;
import com.community.platform.service.GuideService;
import com.community.platform.vo.guide.GuideResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuideServiceImpl implements GuideService {

    @Value("${ai.fallback-enabled:true}")
    private Boolean fallbackEnabled;

    @Override
    public GuideResultVO recommend(GuideRequestDTO dto) {
        // TODO: 调用 AI 服务进行意图识别和事项推荐
        //       AI 不可用时降级为规则匹配
        throw new UnsupportedOperationException("TODO: 实现智能导办推荐");
    }

    @Override
    public String chat(ChatMessageDTO dto) {
        // TODO: 调用 AI 服务进行多轮对话，维护 sessionId 上下文
        throw new UnsupportedOperationException("TODO: 实现 AI 对话");
    }
}
