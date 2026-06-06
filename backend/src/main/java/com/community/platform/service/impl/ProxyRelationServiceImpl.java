package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.platform.common.BusinessException;
import com.community.platform.common.ResultCode;
import com.community.platform.dto.proxy.ProxyApplyDTO;
import com.community.platform.dto.proxy.ProxyBindDTO;
import com.community.platform.entity.ProxyRelation;
import com.community.platform.entity.ResidentProfile;
import com.community.platform.entity.User;
import com.community.platform.mapper.ProxyRelationMapper;
import com.community.platform.mapper.ResidentProfileMapper;
import com.community.platform.mapper.UserMapper;
import com.community.platform.service.NoticeService;
import com.community.platform.service.ProxyRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProxyRelationServiceImpl implements ProxyRelationService {

    private static final String DEFAULT_AUTHORIZED_ACTIONS = "apply,booking,query,notice";

    private final ProxyRelationMapper proxyRelationMapper;
    private final ResidentProfileMapper residentProfileMapper;
    private final UserMapper userMapper;
    private final NoticeService noticeService;

    @Override
    @Transactional
    public Long apply(Long proxyUserId, ProxyApplyDTO dto) {
        ResidentProfile profile = residentProfileMapper.selectOne(new LambdaQueryWrapper<ResidentProfile>()
                .eq(ResidentProfile::getRealName, dto.getRealName())
                .eq(ResidentProfile::getIdCard, dto.getIdCard()));
        if (profile == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "未找到匹配的居民信息");
        }

        Long targetUserId = profile.getUserId();
        if (targetUserId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "居民档案尚未绑定登录账号");
        }
        if (targetUserId.equals(proxyUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能绑定自己");
        }

        long existCount = proxyRelationMapper.selectCount(new LambdaQueryWrapper<ProxyRelation>()
                .eq(ProxyRelation::getProxyUserId, proxyUserId)
                .eq(ProxyRelation::getTargetUserId, targetUserId)
                .in(ProxyRelation::getStatus, "active", "pending"));
        if (existCount > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已存在绑定关系或待确认的申请");
        }

        ProxyRelation relation = new ProxyRelation();
        relation.setProxyUserId(proxyUserId);
        relation.setTargetUserId(targetUserId);
        relation.setTargetProfileId(profile.getProfileId());
        relation.setRelation(dto.getRelation());
        relation.setAuthorizedActions(normalizeAuthorizedActions(dto.getAuthorizedActions()));
        relation.setStatus("pending");
        proxyRelationMapper.insert(relation);

        noticeService.sendNotice(targetUserId, "新的家属绑定申请",
                "用户 " + getUsername(proxyUserId) + " 申请成为您的家属代办人，请前往【家属绑定】页面处理。",
                "proxy", "proxy", relation.getId());
        return relation.getId();
    }

    @Override
    public List<ProxyRelation> getPendingRequests(Long targetUserId) {
        List<ProxyRelation> relations = proxyRelationMapper.selectList(new LambdaQueryWrapper<ProxyRelation>()
                .eq(ProxyRelation::getTargetUserId, targetUserId)
                .eq(ProxyRelation::getStatus, "pending")
                .orderByDesc(ProxyRelation::getCreateTime));
        return enrichProxyRelationMetadata(relations);
    }

    @Override
    @Transactional
    public void confirm(Long targetUserId, Long relationId) {
        ProxyRelation relation = proxyRelationMapper.selectById(relationId);
        if (relation == null || !targetUserId.equals(relation.getTargetUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "申请不存在");
        }
        if (!"pending".equals(relation.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该申请已被处理");
        }
        relation.setStatus("active");
        proxyRelationMapper.updateById(relation);

        noticeService.sendNotice(relation.getProxyUserId(), "家属绑定申请已通过",
                "用户 " + getUsername(targetUserId) + " 已同意您的家属代办申请。",
                "proxy", "proxy", relationId);
    }

    @Override
    @Transactional
    public void reject(Long targetUserId, Long relationId) {
        ProxyRelation relation = proxyRelationMapper.selectById(relationId);
        if (relation == null || !targetUserId.equals(relation.getTargetUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "申请不存在");
        }
        if (!"pending".equals(relation.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该申请已被处理");
        }
        relation.setStatus("rejected");
        proxyRelationMapper.updateById(relation);

        noticeService.sendNotice(relation.getProxyUserId(), "家属绑定申请被拒绝",
                "用户 " + getUsername(targetUserId) + " 拒绝了您的家属代办申请。",
                "proxy", "proxy", relationId);
    }

    @Override
    @Transactional
    public Long bind(Long proxyUserId, ProxyBindDTO dto) {
        if (dto.getTargetUserId() == null && dto.getTargetProfileId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "被代理用户或档案不能为空");
        }
        ProxyRelation relation = new ProxyRelation();
        relation.setProxyUserId(proxyUserId);
        relation.setTargetUserId(dto.getTargetUserId());
        relation.setTargetProfileId(dto.getTargetProfileId());
        relation.setRelation(dto.getRelation());
        relation.setAuthorizedActions(normalizeAuthorizedActions(dto.getAuthorizedActions()));
        relation.setStatus("active");
        proxyRelationMapper.insert(relation);

        if (relation.getTargetUserId() != null) {
            noticeService.sendNotice(relation.getTargetUserId(), "家属代办关系已建立",
                    "用户 " + getUsername(proxyUserId) + " 已成为您的家属代办人。",
                    "proxy", "proxy", relation.getId());
        }
        noticeService.sendNotice(proxyUserId, "家属代办关系已建立",
                "您已成功建立家属代办关系。",
                "proxy", "proxy", relation.getId());
        return relation.getId();
    }

    @Override
    public List<ProxyRelation> getList(Long userId) {
        List<ProxyRelation> relations = proxyRelationMapper.selectList(new LambdaQueryWrapper<ProxyRelation>()
                .and(w -> w.eq(ProxyRelation::getProxyUserId, userId)
                        .or()
                        .eq(ProxyRelation::getTargetUserId, userId))
                .orderByDesc(ProxyRelation::getCreateTime));
        return enrichProxyRelationMetadata(relations);
    }

    @Override
    @Transactional
    public void revoke(Long userId, Long id) {
        ProxyRelation relation = proxyRelationMapper.selectById(id);
        if (relation == null || (!userId.equals(relation.getProxyUserId()) && !userId.equals(relation.getTargetUserId()))) {
            throw new BusinessException(ResultCode.NOT_FOUND, "授权关系不存在");
        }
        relation.setStatus("revoked");
        proxyRelationMapper.updateById(relation);

        if (relation.getProxyUserId() != null && !relation.getProxyUserId().equals(userId)) {
            noticeService.sendNotice(relation.getProxyUserId(), "家属代办关系已解除",
                    "一条家属代办授权关系已被解除。",
                    "proxy", "proxy", id);
        }
        if (relation.getTargetUserId() != null && !relation.getTargetUserId().equals(userId)) {
            noticeService.sendNotice(relation.getTargetUserId(), "家属代办关系已解除",
                    "一条家属代办授权关系已被解除。",
                    "proxy", "proxy", id);
        }
    }

    private String normalizeAuthorizedActions(String actions) {
        return StringUtils.hasText(actions) ? actions : DEFAULT_AUTHORIZED_ACTIONS;
    }

    private String getUsername(Long userId) {
        User user = userMapper.selectById(userId);
        return user == null ? "用户" : user.getUsername();
    }

    private List<ProxyRelation> enrichProxyRelationMetadata(List<ProxyRelation> relations) {
        if (relations == null || relations.isEmpty()) {
            return relations;
        }
        Set<Long> profileIds = relations.stream()
                .map(ProxyRelation::getTargetProfileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> targetUserIds = relations.stream()
                .map(ProxyRelation::getTargetUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> proxyUserIds = relations.stream()
                .map(ProxyRelation::getProxyUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, ResidentProfile> profilesById = profileIds.isEmpty() ? Map.of() : residentProfileMapper.selectList(new LambdaQueryWrapper<ResidentProfile>()
                .in(ResidentProfile::getProfileId, profileIds))
                .stream()
                .collect(Collectors.toMap(ResidentProfile::getProfileId, profile -> profile, (left, right) -> left));
        Map<Long, ResidentProfile> profilesByUserId = targetUserIds.isEmpty() ? Map.of() : residentProfileMapper.selectList(new LambdaQueryWrapper<ResidentProfile>()
                .in(ResidentProfile::getUserId, targetUserIds))
                .stream()
                .collect(Collectors.toMap(ResidentProfile::getUserId, profile -> profile, (left, right) -> left));
        Map<Long, String> proxyUserNames = proxyUserIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(proxyUserIds)
                .stream()
                .collect(Collectors.toMap(User::getUserId, User::getUsername, (left, right) -> left));

        relations.forEach(relation -> {
            if (relation.getProxyUserId() != null) {
                relation.setProxyUserName(proxyUserNames.get(relation.getProxyUserId()));
            }
            ResidentProfile profile = relation.getTargetProfileId() == null
                    ? null
                    : profilesById.get(relation.getTargetProfileId());
            if (profile == null && relation.getTargetUserId() != null) {
                profile = profilesByUserId.get(relation.getTargetUserId());
            }
            if (profile != null) {
                relation.setTargetProfileName(profile.getRealName());
            }
        });
        return relations;
    }
}
