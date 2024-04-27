package com.easypan;/*
 * ClassName:EasyPanApplication
 * Package:com.easypan
 * Description:
 * @Author ly
 * @Create 2024/4/27 13:55
 * @Version 1.0
 */

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.swing.*;

@EnableAsync //开启异步线程
@SpringBootApplication(scanBasePackages = {"com.easypan"})
@MapperScan(basePackages = {"com.easypan.mappers"})
@EnableTransactionManagement //开启事务支持
@EnableScheduling
public class EasyPanApplication {
    public static void main(String[] args) {
        SpringApplication.run(EasyPanApplication.class,args);
    }
}
