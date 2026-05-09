package com.hmdp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 启动类
 */
@MapperScan("com.hmdp.mapper")
@SpringBootApplication
@ConfigurationPropertiesScan
public class HMDPApplication {

    public static void main(String[] args) {
        SpringApplication.run(HMDPApplication.class, args);
    }

}