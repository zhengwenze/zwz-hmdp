package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagStreamMetaDTO {
    private String traceId;
    private String sessionId;
    private List<RagReferenceDTO> references;
    private List<RagCitationDTO> citations;
    private Boolean grounded;
    private Boolean cached;
}
