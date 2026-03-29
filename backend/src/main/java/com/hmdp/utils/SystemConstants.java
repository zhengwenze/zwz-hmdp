package com.hmdp.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SystemConstants {
    public static final String IMAGE_UPLOAD_DIR = resolveImageUploadDir();
    public static final String USER_NICK_NAME_PREFIX = "user_";
    public static final int DEFAULT_PAGE_SIZE = 5;
    public static final int MAX_PAGE_SIZE = 10;

    private static String resolveImageUploadDir() {
        String configuredDir = System.getenv("APP_UPLOAD_DIR");
        Path uploadDir = configuredDir == null || configuredDir.trim().isEmpty()
                ? Paths.get("data", "hmdp", "imgs")
                : Paths.get(configuredDir.trim());
        return uploadDir.toAbsolutePath().normalize().toString();
    }
}
