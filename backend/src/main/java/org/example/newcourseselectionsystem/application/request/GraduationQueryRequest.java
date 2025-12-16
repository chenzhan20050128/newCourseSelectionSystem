package org.example.newcourseselectionsystem.application.request;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class GraduationQueryRequest {
    @NotNull
    private Long studentId;
}

