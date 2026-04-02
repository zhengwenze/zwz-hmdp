package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagIngestJobDTO {
    private Long id;
    private String status;
    private Integer totalFiles;
    private Integer successFiles;
    private Integer failedFiles;
    private LocalDateTime startedTime;
    private LocalDateTime finishedTime;
    private String errorMsg;
}
