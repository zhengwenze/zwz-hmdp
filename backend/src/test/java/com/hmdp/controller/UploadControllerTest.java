package com.hmdp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.utils.SystemConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UploadController.class)
@ContextConfiguration(classes = UploadController.class)
class UploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final List<Path> filesToCleanup = new ArrayList<>();

    @AfterEach
    void tearDown() throws Exception {
        for (Path path : filesToCleanup) {
            Files.deleteIfExists(path);
        }
        filesToCleanup.clear();
    }

    @Test
    void uploadImage_shouldAcceptMultipartFileAndReturnImagePath() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.png",
                "image/png",
                "image-bytes".getBytes());

        MvcResult result = mockMvc.perform(multipart("/upload/blog").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", startsWith("/blogs/")))
                .andReturn();

        Map<?, ?> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        String returnedPath = ((String) body.get("data")).substring(1);
        filesToCleanup.add(Path.of(SystemConstants.IMAGE_UPLOAD_DIR, returnedPath));
    }

    @Test
    void deleteBlogImg_shouldNormalizeFileNameAndReturnOk() throws Exception {
        Path imagePath = Path.of(SystemConstants.IMAGE_UPLOAD_DIR, "blogs", "test-delete.txt");
        Files.createDirectories(imagePath.getParent());
        Files.writeString(imagePath, "test");

        mockMvc.perform(get("/upload/blog/delete").param("name", "/blogs/test-delete.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(imagePath));
    }

    @Test
    void deleteBlogImg_shouldRejectDirectoryName() throws Exception {
        Path directory = Path.of(SystemConstants.IMAGE_UPLOAD_DIR, "blogs", "test-dir");
        Files.createDirectories(directory);

        mockMvc.perform(get("/upload/blog/delete").param("name", "/blogs/test-dir"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMsg").value("错误的文件名称"));

        Files.deleteIfExists(directory);
    }
}
