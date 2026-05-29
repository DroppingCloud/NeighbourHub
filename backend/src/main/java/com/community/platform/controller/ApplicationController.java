package com.community.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.common.Result;
import com.community.platform.dto.application.ApplicationQueryDTO;
import com.community.platform.dto.application.ApplicationSubmitDTO;
import com.community.platform.dto.material.ApplicationMaterialDTO;
import com.community.platform.dto.material.MaterialPrecheckDTO;
import com.community.platform.entity.ApplicationMaterial;
import com.community.platform.service.ApplicationMaterialService;
import com.community.platform.service.ApplicationService;
import com.community.platform.vo.application.ApplicationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "事项申请")
@RestController
@RequestMapping("/api/application")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationMaterialService applicationMaterialService;

    @Operation(summary = "提交申请")
    @PostMapping("/submit")
    public Result<Long> submit(@AuthenticationPrincipal Long userId,
                               @Valid @RequestBody ApplicationSubmitDTO dto) {
        return Result.success(applicationService.submit(userId, dto));
    }

    @Operation(summary = "申请列表")
    @GetMapping("/list")
    public Result<Page<ApplicationVO>> list(@AuthenticationPrincipal Long userId,
                                            ApplicationQueryDTO query) {
        return Result.success(applicationService.getList(userId, query));
    }

    @Operation(summary = "申请详情")
    @GetMapping("/{id}")
    public Result<ApplicationVO> detail(@AuthenticationPrincipal Long userId,
                                        @PathVariable Long id) {
        return Result.success(applicationService.getDetail(userId, id));
    }

    @Operation(summary = "补件重新提交")
    @PutMapping("/{id}/resubmit")
    public Result<Void> resubmit(@AuthenticationPrincipal Long userId,
                                 @PathVariable Long id,
                                 @Valid @RequestBody ApplicationSubmitDTO dto) {
        applicationService.resubmit(userId, id, dto);
        return Result.success();
    }

    @Operation(summary = "上传申请材料")
    @PostMapping("/{id}/materials")
    public Result<Long> uploadMaterial(@AuthenticationPrincipal Long userId,
                                       @PathVariable Long id,
                                       @Valid @RequestBody ApplicationMaterialDTO dto) {
        return Result.success(applicationMaterialService.upload(userId, id, dto));
    }

    @Operation(summary = "申请材料列表")
    @GetMapping("/{id}/materials")
    public Result<List<ApplicationMaterial>> listMaterials(@AuthenticationPrincipal Long userId,
                                                           @PathVariable Long id) {
        return Result.success(applicationMaterialService.getList(userId, id));
    }

    @Operation(summary = "材料预审")
    @PutMapping("/material/{id}/precheck")
    public Result<Void> precheckMaterial(@PathVariable Long id,
                                         @Valid @RequestBody MaterialPrecheckDTO dto) {
        applicationMaterialService.precheck(id, dto);
        return Result.success();
    }
}
