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
            if (!actions.contains(actionType)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "您没有被授权执行此操作");
            }
        }

        return targetUserId;
    }
}