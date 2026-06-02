package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.platform.common.BusinessException;
import com.community.platform.common.ResultCode;
import com.community.platform.dto.proxy.ProxyBindDTO;
import com.community.platform.entity.ProxyRelation;
import com.community.platform.mapper.ProxyRelationMapper;
import com.community.platform.service.ProxyRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.community.platform.dto.proxy.ProxyApplyDTO;
import com.community.platform.mapper.ResidentProfileMapper;
import com.community.platform.mapper.UserMapper;
import com.community.platform.service.NoticeService;
import com.community.platform.entity.ResidentProfile;
import com.community.platform.entity.User;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProxyRelationServiceImpl implements ProxyRelationService {

    private final ProxyRelationMapper proxyRelationMapper;
    private final ResidentProfileMapper residentProfileMapper;
    private final UserMapper userMapper;
    private final NoticeService noticeService;

    @Override
    public Long apply(Long proxyUserId, ProxyApplyDTO dto) {
        // 1. 根据姓名、身份证号、手机号查询居民档案
        LambdaQueryWrapper<ResidentProfile> wrapper = new LambdaQueryWrapper<ResidentProfile>()
                .eq(ResidentProfile::getRealName, dto.getRealName())
                .eq(ResidentProfile::getIdCard, dto.getIdCard());
        ResidentProfile profile = residentProfileMapper.selectOne(wrapper);
        if (profile == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "未找到匹配的居民信息");
        }
        // 2. 不能绑定自己
        Long targetUserId = profile.getUserId();
        if (targetUserId.equals(proxyUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能绑定自己");
        }
        // 3. 检查是否已有有效或待确认的绑定关系
        long existCount = proxyRelationMapper.selectCount(new LambdaQueryWrapper<ProxyRelation>()
                .eq(ProxyRelation::getProxyUserId, proxyUserId)
                .eq(ProxyRelation::getTargetUserId, targetUserId)
                .in(ProxyRelation::getStatus, "active", "pending"));
        if (existCount > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已存在绑定关系或待确认的申请");
        }
        // 4. 创建绑定记录
        ProxyRelation relation = new ProxyRelation();
        relation.setProxyUserId(proxyUserId);
        relation.setTargetUserId(targetUserId);
        relation.setTargetProfileId(profile.getProfileId());
        relation.setRelation(dto.getRelation());
        String actions = StringUtils.hasText(dto.getAuthorizedActions()) ? dto.getAuthorizedActions() : "application,booking,notice";
        relation.setAuthorizedActions(actions);
        relation.setStatus("pending");
        proxyRelationMapper.insert(relation);

        // 5. 发送通知给被代理人
        noticeService.sendNotice(targetUserId, "新的家属绑定申请",
                "用户 " + getUsername(proxyUserId) + " 申请成为您的家属代办人，请前往【家属绑定】页面处理。",
                "proxy", "proxy", relation.getId());
        return relation.getId();
    }

    @Override
    public List<ProxyRelation> getPendingRequests(Long targetUserId) {
        return proxyRelationMapper.selectList(new LambdaQueryWrapper<ProxyRelation>()
                .eq(ProxyRelation::getTargetUserId, targetUserId)
                .eq(ProxyRelation::getStatus, "pending")
                .orderByDesc(ProxyRelation::getCreateTime));
    }

    @Override
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
        // 通知家属
        noticeService.sendNotice(relation.getProxyUserId(), "家属绑定申请已通过",
                "用户 " + getUsername(targetUserId) + " 已同意您的家属代办申请。",
                "proxy", "proxy", relationId);
    }

    @Override
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
        // 通知家属
        noticeService.sendNotice(relation.getProxyUserId(), "家属绑定申请被拒绝",
                "用户 " + getUsername(targetUserId) + " 拒绝了您的家属代办申请。",
                "proxy", "proxy", relationId);
    }

    private String getUsername(Long userId) {
        User user = userMapper.selectById(userId);
        return user == null ? "用户" : user.getUsername();
    }

    @Override
    public Long bind(Long proxyUserId, ProxyBindDTO dto) {
        if (dto.getTargetUserId() == null && dto.getTargetProfileId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "被代理用户或档案不能为空");
        }
        ProxyRelation relation = new ProxyRelation();
        relation.setProxyUserId(proxyUserId);
        relation.setTargetUserId(dto.getTargetUserId());
        relation.setTargetProfileId(dto.getTargetProfileId());
        relation.setRelation(dto.getRelation());
        relation.setAuthorizedActions(StringUtils.hasText(dto.getAuthorizedActions()) ? dto.getAuthorizedActions() : "application,booking,notice");
        relation.setStatus("active");
        proxyRelationMapper.insert(relation);
        return relation.getId();
    }

    @Override
    public List<ProxyRelation> getList(Long proxyUserId) {
        return proxyRelationMapper.selectList(new LambdaQueryWrapper<ProxyRelation>()
                .eq(ProxyRelation::getProxyUserId, proxyUserId)
                .orderByDesc(ProxyRelation::getCreateTime));
    }

    @Override
    @Transactional
    public void revoke(Long proxyUserId, Long id) {
        ProxyRelation relation = proxyRelationMapper.selectById(id);
        if (relation == null || !proxyUserId.equals(relation.getProxyUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "授权关系不存在");
        }
        relation.setStatus("revoked");
        proxyRelationMapper.updateById(relation);
    }
}
