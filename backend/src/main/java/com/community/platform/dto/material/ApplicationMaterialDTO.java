package com.community.platform.dto.material;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApplicationMaterialDTO {

    private Long templateId;

    private String materialName;

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotBlank(message = "文件路径不能为空")
    private String filePath;

    private Long fileSize;

    private String fileType;
}
