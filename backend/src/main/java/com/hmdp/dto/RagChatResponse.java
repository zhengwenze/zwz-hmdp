package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagChatResponse {
    private String answer;
    private List<RagCitationDTO> citations;
    private Boolean grounded;
    private String traceId;
}
