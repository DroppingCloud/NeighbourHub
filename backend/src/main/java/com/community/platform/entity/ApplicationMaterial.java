package com.community.platform.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Uploaded material metadata for an application.
 */
@Data
@TableName("application_material")
public class ApplicationMaterial {

    @TableId(type = IdType.AUTO)
    private Long materialId;

    private Long applicationId;

    private Long templateId;

    private String materialName;

    private String fileName;

    private String filePath;

    private Long fileSize;

    private String fileType;

    private String ocrText;

    /** pending/passed/failed */
    private String precheckStatus;

    private String precheckRemark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime uploadTime;
}
