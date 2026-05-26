package com.community.platform.service;

import com.community.platform.dto.guide.ChatMessageDTO;
import com.community.platform.dto.guide.GuideRequestDTO;
import com.community.platform.vo.guide.GuideResultVO;

public interface GuideService {
    GuideResultVO recommend(GuideRequestDTO dto);
    String chat(ChatMessageDTO dto);
}
