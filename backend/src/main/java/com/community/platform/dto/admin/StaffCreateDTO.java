package com.community.platform.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class StaffCreateDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;

    private String email;

    private Long communityId;

    /** application: 事项办理工作人员; booking: 服务预约工作人员 */
    @NotBlank(message = "工作人员类型不能为空")
    private String staffType;
}
