package com.community.platform.controller;

import com.community.platform.common.Result;
import com.community.platform.dto.guide.ChatMessageDTO;
import com.community.platform.dto.guide.GuideRequestDTO;
import com.community.platform.service.GuideService;
import com.community.platform.vo.guide.GuideResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "智能导办")
@RestController
@RequestMapping("/api/guide")
@RequiredArgsConstructor
public class GuideController {

    private final GuideService guideService;

    @Operation(summary = "智能推荐事项")
    @PostMapping("/recommend")
    public Result<GuideResultVO> recommend(@RequestBody GuideRequestDTO dto) {
        return Result.success(guideService.recommend(dto));
    }

    @Operation(summary = "AI 对话咨询")
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody ChatMessageDTO dto) {
        return Result.success(guideService.chat(dto));
    }
}
