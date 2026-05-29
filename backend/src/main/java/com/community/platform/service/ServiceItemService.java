package com.community.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.entity.ServiceItem;
import com.community.platform.entity.ServiceMaterialTemplate;

import java.util.List;

public interface ServiceItemService {
    Page<ServiceItem> getList(String category, String status, Integer pageNum, Integer pageSize);
    ServiceItem getDetail(Long itemId);
    Long create(ServiceItem serviceItem);
    void update(ServiceItem serviceItem);
    void delete(Long itemId);
    List<ServiceMaterialTemplate> getMaterials(Long itemId);
    Long createMaterialTemplate(ServiceMaterialTemplate template);
    void updateMaterialTemplate(ServiceMaterialTemplate template);
    void deleteMaterialTemplate(Long templateId);
}
