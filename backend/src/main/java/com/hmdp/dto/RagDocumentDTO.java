package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagDocumentDTO {
    private Long id;
    private String fileName;
    private String filePath;
    private String status;
    private Integer chunkCount;
    private String errorMsg;
    private LocalDateTime updateTime;
}
