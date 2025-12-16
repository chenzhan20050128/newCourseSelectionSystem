package org.example.newcourseselectionsystem.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TypeCreditDeficitDTO {
    private String type;
    private int requiredCredits;
    private int earnedCredits;
    private int remainingCredits;
}

