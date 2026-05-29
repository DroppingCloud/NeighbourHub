package com.community.platform.dto.booking;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingAssignDTO {

    @NotNull(message = "工作人员ID不能为空")
    private Long staffUserId;
}
