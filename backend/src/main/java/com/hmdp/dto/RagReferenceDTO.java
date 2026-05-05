package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagReferenceDTO {
    private String source;
    private String content;
    private Double score;
    private String chunkId;
    private Integer chunkIndex;
    private String section;
    private Integer pageNo;
}
