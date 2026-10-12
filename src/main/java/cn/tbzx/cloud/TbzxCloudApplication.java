package cn.tbzx.cloud;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@MapperScan("cn.tbzx.cloud.modules.**.mapper")
public class TbzxCloudApplication {

    public static void main(String[] args) {
        SpringApplication.run(TbzxCloudApplication.class, args);
    }
}
