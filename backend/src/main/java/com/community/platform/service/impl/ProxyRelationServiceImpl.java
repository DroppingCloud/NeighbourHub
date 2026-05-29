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

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProxyRelationServiceImpl implements ProxyRelationService {

    private final ProxyRelationMapper proxyRelationMapper;

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
