package com.community.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Material requirement template for a service item.
 */
@Data
@TableName("service_material_template")
public class ServiceMaterialTemplate {

    @TableId(type = IdType.AUTO)
    private Long templateId;

    private Long itemId;

    private String materialName;

    private String materialType;

    private String description;

    private String sampleUrl;

    private Integer isRequired;

    private Integer sortOrder;
}
