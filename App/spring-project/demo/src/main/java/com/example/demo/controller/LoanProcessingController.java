package com.example.demo.controller;

import com.example.demo.dto.LoanStatus;
import com.example.demo.service.LoanTrackingService;
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
}
