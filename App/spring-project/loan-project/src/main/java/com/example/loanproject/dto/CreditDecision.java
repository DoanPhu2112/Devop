package com.example.loanproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditDecision {
    private String userId;
    private String username;
    private int creditScore;
    private String riskBand;
    private boolean approved;
    private double maxApprovedAmount;
    private String decisionReason;
    private UserAuthEventType lastEventType;
    private long evaluatedAt;
}
