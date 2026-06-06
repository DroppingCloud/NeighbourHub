package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.platform.common.BusinessException;
import com.community.platform.common.ResultCode;
import com.community.platform.entity.ProxyRelation;
import com.community.platform.entity.ResidentProfile;
import com.community.platform.entity.User;
import com.community.platform.mapper.ProxyRelationMapper;
import com.community.platform.mapper.ResidentProfileMapper;
import com.community.platform.mapper.UserMapper;
import com.community.platform.service.ProxyPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProxyPermissionServiceImpl implements ProxyPermissionService {

    private final ProxyRelationMapper proxyRelationMapper;
    private final ResidentProfileMapper residentProfileMapper;
    private final UserMapper userMapper;

    @Override
    public Long validateAndGetTargetUserId(Long currentUserId, Long targetProfileId, String actionType) {
        // 如果 targetProfileId 为空，表示本人操作
        if (targetProfileId == null) {
            return currentUserId;
        }

        // 查询目标居民档案
        ResidentProfile targetProfile = residentProfileMapper.selectById(targetProfileId);
        if (targetProfile == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "目标居民不存在");
        }
        Long targetUserId = targetProfile.getUserId();
        if (targetUserId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "目标居民未关联用户账户");
        }

        // 查询绑定关系
        ProxyRelation relation = proxyRelationMapper.selectOne(new LambdaQueryWrapper<ProxyRelation>()
                .eq(ProxyRelation::getProxyUserId, currentUserId)
                .eq(ProxyRelation::getTargetProfileId, targetProfileId)
                .eq(ProxyRelation::getStatus, "active"));
        if (relation == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "您无权为当前居民代办业务");
        }

        // 校验授权范围
        String authorizedActions = relation.getAuthorizedActions();
        if (StringUtils.hasText(authorizedActions)) {
            Set<String> actions = Arrays.stream(authorizedActions.split(","))
                    .map(String::trim)
                    .collect(Collectors.toSet());
            if (!isActionAuthorized(actions, actionType)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "您没有被授权执行此操作");
            }
        }

        return targetUserId;
    }

    @Override
    public void validateProxyPermission(Long currentUserId, Long targetUserId, String actionType) {
        if (currentUserId.equals(targetUserId)) {
            return; // 本人操作无需校验
        }

        ProxyRelation relation = proxyRelationMapper.selectOne(new LambdaQueryWrapper<ProxyRelation>()
                .eq(ProxyRelation::getProxyUserId, currentUserId)
                .eq(ProxyRelation::getTargetUserId, targetUserId)
                .eq(ProxyRelation::getStatus, "active"));
        if (relation == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "您无权为该用户代办业务");
        }

        String authorizedActions = relation.getAuthorizedActions();
        if (StringUtils.hasText(authorizedActions)) {
            Set<String> actions = Arrays.stream(authorizedActions.split(","))
                    .map(String::trim)
                    .collect(Collectors.toSet());
            if (!isActionAuthorized(actions, actionType)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "您没有被授权执行此操作");
            }
        }
    }

    @Override
    public boolean hasProxyPermission(Long currentUserId, Long targetUserId, String actionType) {
        if (currentUserId.equals(targetUserId)) {
            return true;
        }

        ProxyRelation relation = proxyRelationMapper.selectOne(new LambdaQueryWrapper<ProxyRelation>()
                .eq(ProxyRelation::getProxyUserId, currentUserId)
                .eq(ProxyRelation::getTargetUserId, targetUserId)
                .eq(ProxyRelation::getStatus, "active"));
        if (relation == null) {
            return false;
        }

        String authorizedActions = relation.getAuthorizedActions();
        if (!StringUtils.hasText(authorizedActions)) {
            return true;
        }
        Set<String> actions = Arrays.stream(authorizedActions.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
        return isActionAuthorized(actions, actionType);
    }

    private boolean isActionAuthorized(Set<String> actions, String actionType) {
        if (actions.contains(actionType)) {
            return true;
        }
        // 兼容早期数据中使用 application 表示“事项申请+查询”的授权写法。
        if ("application".equals(actionType)) {
            return actions.contains("application") || actions.contains("apply") || actions.contains("query");
        }
        return ("apply".equals(actionType) || "query".equals(actionType)) && actions.contains("application");
    }
}
