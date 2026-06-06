package com.community.platform.controller;

import com.community.platform.common.Result;
import com.community.platform.dto.proxy.ProxyApplyDTO;
import com.community.platform.dto.proxy.ProxyBindDTO;
import com.community.platform.entity.ProxyRelation;
import com.community.platform.service.ProxyRelationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "家属绑定")
@RestController
@RequestMapping("/api/proxy")
@RequiredArgsConstructor
public class ProxyRelationController {

    private final ProxyRelationService proxyRelationService;

    @Operation(summary = "绑定被代理人（仅管理员可直接绑定，普通用户请使用 /apply 流程）")
    @PostMapping("/bind")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> bind(@AuthenticationPrincipal Long userId, @RequestBody ProxyBindDTO dto) {
        return Result.success(proxyRelationService.bind(userId, dto));
    }

    @Operation(summary = "家属绑定列表")
    @GetMapping("/list")
    public Result<List<ProxyRelation>> list(@AuthenticationPrincipal Long userId) {
        return Result.success(proxyRelationService.getList(userId));
    }

    @Operation(summary = "撤销绑定授权")
    @PutMapping("/{id}/revoke")
    public Result<Void> revoke(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        proxyRelationService.revoke(userId, id);
        return Result.success();
    }

    @Operation(summary = "发起绑定申请（居民或家属可调用）")
    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('RESIDENT','FAMILY')")
    public Result<Long> apply(@AuthenticationPrincipal Long userId,
                              @Valid @RequestBody ProxyApplyDTO dto) {
        return Result.success(proxyRelationService.apply(userId, dto));
    }

    @Operation(summary = "获取待确认的绑定请求（作为被代理人）")
    @GetMapping("/pending-requests")
    public Result<List<ProxyRelation>> getPendingRequests(@AuthenticationPrincipal Long userId) {
        return Result.success(proxyRelationService.getPendingRequests(userId));
    }

    @Operation(summary = "确认绑定申请")
    @PutMapping("/{id}/confirm")
    public Result<Void> confirm(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        proxyRelationService.confirm(userId, id);
        return Result.success();
    }

    @Operation(summary = "拒绝绑定申请")
    @PutMapping("/{id}/reject")
    public Result<Void> reject(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        proxyRelationService.reject(userId, id);
        return Result.success();
    }
}
