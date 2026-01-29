package com.example.loanproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanStatus {
    private String userId;
    private String username;
    private UserAuthEventType lastEventType;
    private long lastEventAt;
    private String status;
    private String notes;
}
