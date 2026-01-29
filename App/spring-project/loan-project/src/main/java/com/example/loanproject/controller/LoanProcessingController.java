package com.example.loanproject.controller;

import com.example.loanproject.dto.CreditDecision;
import com.example.loanproject.dto.LoanStatus;
import com.example.loanproject.service.CreditApprovalService;
import com.example.loanproject.service.LoanTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequiredArgsConstructor
@RequestMapping("/loan")
public class LoanProcessingController {
    private final LoanTrackingService loanTrackingService;
    private final CreditApprovalService creditApprovalService;

    @GetMapping(path = "/statuses", produces = "application/json")
    public ResponseEntity<Collection<LoanStatus>> listLoanStatuses() {
        return ResponseEntity.ok(loanTrackingService.getAllStatuses());
    }

    @GetMapping(path = "/statuses/{userId}", produces = "application/json")
    public ResponseEntity<LoanStatus> getLoanStatus(@PathVariable String userId) {
        return loanTrackingService.getStatus(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(path = "/credit-decisions", produces = "application/json")
    public ResponseEntity<Collection<CreditDecision>> listDecisions() {
        return ResponseEntity.ok(creditApprovalService.getAllDecisions());
    }

    @GetMapping(path = "/credit-decisions/{userId}", produces = "application/json")
    public ResponseEntity<CreditDecision> getDecision(@PathVariable String userId) {
        return creditApprovalService.getDecision(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
