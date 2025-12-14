package org.example.newcourseselectionsystem.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class GraduationStatusResponse {
    private Long studentId;
    private List<TypeCreditDeficitDTO> deficits;
    private int totalRequired;
    private int totalEarned;
}

