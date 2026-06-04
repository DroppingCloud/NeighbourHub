package com.community.platform.service;

import com.community.platform.entity.ProxyRelation;
import com.community.platform.entity.ResidentProfile;
import com.community.platform.entity.User;

public interface ProxyPermissionService {
    /**
     * 校验当前用户是否有权代理目标居民执行某项操作
     * @param currentUserId 当前登录用户ID
     * @param targetProfileId 目标居民档案ID（可为null，表示本人操作）
     * @param actionType 操作类型，如 "apply", "booking", "query"
     * @return 目标居民对应的用户ID（若本人操作，返回 currentUserId）
     * @throws BusinessException 如果无权代理
     */
    Long validateAndGetTargetUserId(Long currentUserId, Long targetProfileId, String actionType);

    /**
     * 校验当前用户是否有权代理目标用户执行某项操作（通过目标用户ID校验）
     * @param currentUserId 当前登录用户ID
     * @param targetUserId 目标用户ID（被代理人）
     * @param actionType 操作类型，如 "apply", "booking", "query", "notice"
     * @throws BusinessException 如果无权代理
     */
    void validateProxyPermission(Long currentUserId, Long targetUserId, String actionType);

    /**
     * 检查当前用户是否为目标用户的有效代理人（不抛异常，返回布尔值）
     */
    boolean hasProxyPermission(Long currentUserId, Long targetUserId, String actionType);
}