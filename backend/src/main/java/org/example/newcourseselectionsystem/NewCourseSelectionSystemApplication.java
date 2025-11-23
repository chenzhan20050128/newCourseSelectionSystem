package org.example.newcourseselectionsystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.example.newcourseselectionsystem.infrastructure.mapper")
public class NewCourseSelectionSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewCourseSelectionSystemApplication.class, args);
    }

}
