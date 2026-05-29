package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.common.BusinessException;
import com.community.platform.common.ResultCode;
import com.community.platform.entity.ServiceItem;
import com.community.platform.entity.ServiceMaterialTemplate;
import com.community.platform.mapper.ServiceItemMapper;
import com.community.platform.mapper.ServiceMaterialTemplateMapper;
import com.community.platform.service.ServiceItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceItemServiceImpl implements ServiceItemService {

    private final ServiceItemMapper serviceItemMapper;
    private final ServiceMaterialTemplateMapper materialTemplateMapper;

    @Override
    public Page<ServiceItem> getList(String category, String status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<ServiceItem> wrapper = new LambdaQueryWrapper<ServiceItem>()
                .eq(StringUtils.hasText(category), ServiceItem::getCategory, category)
                .eq(StringUtils.hasText(status), ServiceItem::getStatus, status)
                .orderByDesc(ServiceItem::getCreateTime);
        return serviceItemMapper.selectPage(new Page<>(page(pageNum), size(pageSize)), wrapper);
    }

    @Override
    public ServiceItem getDetail(Long itemId) {
        ServiceItem item = serviceItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ResultCode.SERVICE_ITEM_NOT_EXISTS);
        }
        return item;
    }

    @Override
    public Long create(ServiceItem serviceItem) {
        if (!StringUtils.hasText(serviceItem.getStatus())) {
            serviceItem.setStatus("online");
        }
        serviceItemMapper.insert(serviceItem);
        return serviceItem.getItemId();
    }

    @Override
    public void update(ServiceItem serviceItem) {
        getDetail(serviceItem.getItemId());
        serviceItemMapper.updateById(serviceItem);
    }

    @Override
    @Transactional
    public void delete(Long itemId) {
        getDetail(itemId);
        serviceItemMapper.deleteById(itemId);
    }

    @Override
    public List<ServiceMaterialTemplate> getMaterials(Long itemId) {
        getDetail(itemId);
        return materialTemplateMapper.selectList(new LambdaQueryWrapper<ServiceMaterialTemplate>()
                .eq(ServiceMaterialTemplate::getItemId, itemId)
                .orderByAsc(ServiceMaterialTemplate::getSortOrder)
                .orderByAsc(ServiceMaterialTemplate::getTemplateId));
    }

    @Override
    public Long createMaterialTemplate(ServiceMaterialTemplate template) {
        getDetail(template.getItemId());
        if (template.getIsRequired() == null) {
            template.setIsRequired(1);
        }
        if (template.getSortOrder() == null) {
            template.setSortOrder(0);
        }
        materialTemplateMapper.insert(template);
        return template.getTemplateId();
    }

    @Override
    public void updateMaterialTemplate(ServiceMaterialTemplate template) {
        ServiceMaterialTemplate existing = materialTemplateMapper.selectById(template.getTemplateId());
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "材料模板不存在");
        }
        if (template.getItemId() != null) {
            getDetail(template.getItemId());
        }
        materialTemplateMapper.updateById(template);
    }

    @Override
    public void deleteMaterialTemplate(Long templateId) {
        if (materialTemplateMapper.selectById(templateId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "材料模板不存在");
        }
        materialTemplateMapper.deleteById(templateId);
    }

    private long page(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private long size(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}
