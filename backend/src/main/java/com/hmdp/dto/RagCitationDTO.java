package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagCitationDTO {
    private String chunkId;
    private String fileName;
    private String section;
    private Integer pageNo;
    private String snippet;
}
