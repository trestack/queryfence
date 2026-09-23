package com.acme.shop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.acme.shop")
public class ShopApplication {

  public static void main(String[] args) {
    SpringApplication.run(ShopApplication.class, args);
  }
}
