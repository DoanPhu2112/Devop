package com.example.loanproject.service;

import com.example.loanproject.dto.CreditDecision;
import com.example.loanproject.dto.UserAuthEvent;
import com.example.loanproject.dto.UserAuthEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
public class CreditApprovalService {
    private static final int DEFAULT_CREDIT_SCORE = 620;
    private static final double DEFAULT_ANNUAL_INCOME = 48_000d;

    // Hardcoded user profiles for POC/demo purposes
    private static final Map<String, HardcodedProfile> USER_PROFILES = Map.of(
            "alice-prime", new HardcodedProfile(800, 150_000d),
            "bob-fair", new HardcodedProfile(680, 70_000d),
            "charlie-risk", new HardcodedProfile(520, 40_000d),
            "derived-user", new HardcodedProfile(735, 90_000d)
    );

    private final ConcurrentMap<String, CreditDecision> decisions = new ConcurrentHashMap<>();

    public CreditDecision evaluate(UserAuthEvent event) {
        if (event == null || event.getUserId() == null) {
            log.warn("Skip credit evaluation because event or user id is null");
            return null;
        }

        UserAuthEventType eventType = event.safeEventType();
        long evaluatedAt = event.getOccurredAt() != null ? event.getOccurredAt() : Instant.now().toEpochMilli();
        int creditScore = creditScoreFor(event.getUserId());
        String riskBand = riskBand(creditScore);
        boolean approved = creditScore >= 640 && eventType != UserAuthEventType.LOGOUT;
        double annualIncome = annualIncomeFor(event.getUserId());
        double maxApprovedAmount = approved ? Math.max(creditScore * 40d, annualIncome * 0.25d) : 0d;

        CreditDecision decision = CreditDecision.builder()
                .userId(event.getUserId())
                .username(event.getUsername())
                .creditScore(creditScore)
                .riskBand(riskBand)
                .approved(approved)
                .maxApprovedAmount(maxApprovedAmount)
                .decisionReason(buildReason(riskBand, approved, eventType))
                .lastEventType(eventType)
                .evaluatedAt(evaluatedAt)
                .build();

        decisions.put(event.getUserId(), decision);

        log.info("Credit decision updated for user {} with score {} ({}). Approved: {}", 
                event.getUserId(), creditScore, riskBand, approved);
        return decision;
    }

    public Collection<CreditDecision> getAllDecisions() {
        return decisions.values();
    }

    public Optional<CreditDecision> getDecision(String userId) {
        return Optional.ofNullable(decisions.get(userId));
    }

    private int creditScoreFor(String userId) {
        if (userId == null) {
            return DEFAULT_CREDIT_SCORE;
        }
        HardcodedProfile profile = USER_PROFILES.get(userId);
        return profile != null ? clamp(profile.creditScore(), 300, 900) : DEFAULT_CREDIT_SCORE;
    }

    private double annualIncomeFor(String userId) {
        if (userId == null) {
            return DEFAULT_ANNUAL_INCOME;
        }
        HardcodedProfile profile = USER_PROFILES.get(userId);
        return profile != null ? profile.annualIncome() : DEFAULT_ANNUAL_INCOME;
    }

    private String riskBand(int score) {
        if (score >= 760) return "Prime";
        if (score >= 700) return "Near-Prime";
        if (score >= 640) return "Fair";
        if (score >= 580) return "Subprime";
        return "High-Risk";
    }

    private String buildReason(String band, boolean approved, UserAuthEventType type) {
        if (!approved) {
            return "Declined: insufficient score for band " + band;
        }
        return "Approved on " + type.name() + " with risk band " + band;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record HardcodedProfile(int creditScore, double annualIncome) { }
}
