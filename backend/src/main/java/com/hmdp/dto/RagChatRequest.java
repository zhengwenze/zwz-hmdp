package com.hmdp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RagChatRequest {
    @NotBlank(message = "sessionId 不能为空")
    private String sessionId;

    @NotBlank(message = "question 不能为空")
    private String question;
}
