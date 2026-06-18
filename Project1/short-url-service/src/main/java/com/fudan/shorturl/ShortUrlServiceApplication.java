package com.fudan.shorturl;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.fudan.shorturl.mapper")
public class ShortUrlServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShortUrlServiceApplication.class, args);
	}

}
