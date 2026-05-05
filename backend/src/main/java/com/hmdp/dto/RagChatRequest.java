package com.hmdp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RagChatRequest {
    private String sessionId;

    @NotBlank(message = "question 不能为空")
    private String question;
}
