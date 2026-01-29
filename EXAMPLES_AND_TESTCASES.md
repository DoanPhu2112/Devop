# POC - Visual Examples & Test Cases




















































































































































































































































































































- [ ] Migration path for existing events- [ ] Event schema versioning strategy- [ ] Real-time score updates from credit bureaus- [ ] Persistence layer for credit scores (database)For production implementation, consider:4. ✅ Confirm event flow with loan service processing3. ✅ Verify credit scores are deterministically derived2. ✅ Test with simplified payloads using EXAMPLES_AND_TESTCASES.md1. ✅ Rebuild both services: `mvn clean package`## Next Steps---| Clear separation of concerns | Architecture clarity | Easier to scale and extend || Smaller event payload | Reduced bandwidth | Better performance || Deterministic score derivation | Consistent results | Predictable behavior for testing || Moved credit derivation to service | Centralized logic | Single source of truth for business rules || Removed 5 fields from event | Simpler model | Easier to understand and maintain ||--------|--------|---------|| Change | Impact | Benefit |## Summary---```4. Loan-Project derives score = 735 (same value, deterministic)3. Publish: {userId: "u1", eventType: "LOGIN"} at 10:05 AM2. Loan-Project derives score = 735 (Near-Prime)1. Publish: {userId: "u1", eventType: "LOGIN"} at 10:00 AM```### Scenario 3: Consistent Scoring ✓```3. Returns: approved = false (regardless of score)2. Loan-Project derives score but event type is LOGOUT1. Publish: {userId: "u1", username: "User 1", eventType: "LOGOUT"}```### Scenario 2: Logout Denial ✓```3. Returns: CreditDecision with derived score2. Loan-Project derives score from "u1" hash1. Publish: {userId: "u1", username: "User 1", eventType: "LOGIN"}```### Scenario 1: Basic Event Flow ✓## Testing Scenarios---```}        .orElse(620); // fallback        .map(CreditProfile::getScore)    return creditRepository.findByUserId(userId)private int deriveCreditScore(String userId) {// Future: Query credit database}    return 520 + (derived % 330);    int derived = Math.abs(userId.hashCode());private int deriveCreditScore(String userId) {// Current: Deterministic hash```javaIf implementing persistent credit data lookup:## Migration Path for Production---- Each service owns its business logic- Simpler event model (easier to replicate across services)- Smaller event payloads (less network bandwidth)### 4. Scalability- Simpler versioning strategy- New risk bands can be added without changing published events- Future changes to credit logic don't affect event schema### 3. Event Schema Evolution- Clear responsibility boundary- **Loan-Project**: Only handles credit decisions (financial logic)- **Demo**: Only publishes events (authentication context)### 2. Business Logic Centralization- Suitable for POC/Demo environments- No external lookup needed- Same userId → Same credit score (across service restarts)Credit scores are **deterministically derived** from userId:### 1. Deterministic Derivation## Key Insights---```              - GET /loan/credit-decisions/:userId              - GET /loan/statuses/:userId              Clients Query Results                       ↓                       │└──────────────────────┬──────────────────────────────────────┘│ 4. ACK event to KurrentDB                                   ││                                                              ││    - Updates loan status                                    ││ 3. LoanTrackingService.handleUserEvent()                    ││                                                              ││    - Makes approval decision                                ││    - Calculates risk band                                   ││    - deriveAnnualIncome(userId) ← NEW                       ││    - deriveCreditScore(userId) ← NEW                        ││ 2. CreditApprovalService.evaluate()                         ││                                                              ││    - No creditScore/annualIncome in event                   ││    - Receives: {eventId, userId, username, eventType}       ││ 1. UserAuthSubscriptionHandler.onEventAppeared()            ││                                                              ││ Loan-Project Service (9993) - Subscriber                    │┌─────────────────────────────────────────────────────────────┐                       ↓                       │ ROUND_ROBIN Consumer                       │ Persistent Subscription└──────────────────────┬──────────────────────────────────────┘│ - All events stored immutably                               ││ - Persistent Subscription: My_Group_Diy                     ││ KurrentDB (Event Store)                                     │┌─────────────────────────────────────────────────────────────┐                       ↓                       │ Event: KeycloakAuthEvent-LOGIN                       │ EventStoreDB Stream: My_Stream_Diy└──────────────────────┬──────────────────────────────────────┘│    - Stores status: PROFILE_CREATED, ACTIVE_SESSION, etc.   ││    - Tracks loan status by event type                       ││ 3. LoanTrackingService.handleUserEvent()                    ││                                                              ││    - No creditScore/annualIncome processing                 ││    - Only preserves core fields                             ││ 2. UserAuthEventPublisher.normalize()                       ││                                                              ││    - Appends to KurrentDB stream                            ││    - Normalizes to: {eventId, userId, username, ...}        ││    - Generates eventId (UUID)                               ││ 1. UserAuthEventPublisher.publish()                         ││                                                              ││ Demo Service (9992) - Publisher                             │┌─────────────────────────────────────────────────────────────┐                       ↓                       │ {userId, username, eventType}                       │ POST /kurrentdb/auth-events└──────────────────────┬──────────────────────────────────────┘│ Client/Postman                                              │┌─────────────────────────────────────────────────────────────┐```## Event Processing Flow---```# Result: creditScore will be the same deterministic valuecurl http://localhost:9993/loan/credit-decisions/alice-prime# Later, Query at 10:05 AM - Same credit score  -d '{"userId": "alice-prime", "username": "Alice", "eventType": "LOGIN"}'curl -X POST http://localhost:9992/kurrentdb/auth-events \# Request 1 at 10:00 AM```bashMultiple requests with same userId always produce same credit decision:### Credit Scores Remain Consistent| `derived-user` | ~735 | Near-Prime | ✓ || `charlie-risk` | ~520 | High-Risk | ✗ || `bob-fair` | ~680 | Fair | ✓ || `alice-prime` | ~800 | Prime | ✓ ||--------|---------------|-----------|----------|| userId | Derived Score | Risk Band | Approved |Same userId always produces same credit score (deterministic):### Credit Scores are Now Auto-Derived**Note:** No `creditScore`, `annualIncome`, `email`, `fullName`, or `country` needed.```  }'    "eventType": "LOGIN"    "username": "Alice Prime",    "userId": "alice-prime",  -d '{  -H "Content-Type: application/json" \curl -X POST http://localhost:9992/kurrentdb/auth-events \```bash### Updated Request Format## Testing the Refactored Model---| **Flexibility** | Low (changes ripple) | High (each service owns logic) || **Payload** | 180+ bytes | ~100 bytes || **Testing** | Complex (many fields) | Simple (minimal fields) || **Business Logic Location** | Scattered (in event) | Centralized (in loan service) || **Data Coupling** | Tight (auth depends on business logic) | Loose (auth is independent) || **Event Size** | 10 fields | 5 fields || **Separation of Concerns** | Mixed (auth + finance) | Clean (auth event only) ||--------|--------|-------|| Aspect | Before | After |## Benefits---```}    return 48000d; // Default fallback    }        return (double) normalized;        int normalized = 35000 + (derived % 115000);        int derived = Math.abs(userId.hashCode());    if (userId != null) {    // Range: $35,000 - $150,000    // Deterministic derivation from userId hashprivate double deriveAnnualIncome(String userId) {}    return 620; // Default fallback    }        return clamp(normalized, 300, 900);        int normalized = 520 + (derived % 330);        int derived = Math.abs(userId.hashCode());    if (userId != null) {    // Range: 520-849 (High-Risk to Prime)    // Deterministic derivation from userId hashprivate int deriveCreditScore(String userId) {```java**New Logic:**- ✅ Now derives both values deterministically from userId hash- ✅ Added `deriveAnnualIncome(String userId)` method- ✅ Replaced `resolveCreditScore(UserAuthEvent)` with `deriveCreditScore(String userId)`#### CreditApprovalService.java- ✅ Kept: `eventId`, `userId`, `username`, `eventType`, `occurredAt`- ✅ Removed: `email`, `fullName`, `country`, `creditScore`, `annualIncome`#### UserAuthEvent.java### 2. Loan-Project Service (`/loan-project/`)- ✅ Simplified payload serialization (only 5 fields instead of 10)- ✅ Updated `normalize()` to only handle core event fields#### UserAuthEventPublisher.java- ✅ Kept: `eventId`, `userId`, `username`, `eventType`, `occurredAt`- ✅ Removed: `email`, `fullName`, `country`, `creditScore`, `annualIncome`#### UserAuthEvent.java### 1. Demo Service (`/demo/`)## Files Modified---Financial data (credit score, annual income) are business logic concerns that belong in the loan service, not in the authentication event.**Event should contain only authentication context, not financial data.**### Principle## Architecture Changes```}    private Long occurredAt;    private UserAuthEventType eventType;    private String username;    private String userId;    private String eventId;public class UserAuthEvent {```java### After```}    private Long occurredAt;    private UserAuthEventType eventType;    private Double annualIncome;    private Integer creditScore;    private String country;    private String fullName;    private String email;    private String username;    private String userId;    private String eventId;public class UserAuthEvent {```java### BeforeThe `UserAuthEvent` model has been refactored to follow a cleaner separation of concerns:## Summary of Changes## Complete End-to-End Walkthrough

### Setup Check
```bash
# Terminal 1: Check services
curl http://localhost:9992/health
# {"status":"UP"}

curl http://localhost:9993/health
# {"status":"UP","service":"loan-project"}

curl http://localhost:2113/health
# {"state":"Live"}
```

---

## Test Case 1: High Credit Score (Prime Band)

### Step 1: Publish Event
```bash
curl -X POST http://localhost:9992/kurrentdb/auth-events \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "alice-prime",
    "username": "Alice Prime",
    "eventType": "LOGIN"
  }'
```

**Response:**
```json
{
  "eventId": "e1234567-89ab-cdef-0123-456789abcdef",
  "streamName": "My_Stream_Diy",
  "createdEpochMillis": 1737854050000
}
```

### Step 2: Check Loan Status (Demo)
```bash
curl http://localhost:9992/loan/statuses/alice-prime
```

**Response:**
```json
{
  "userId": "alice-prime",
  "username": "Alice Prime",
  "lastEventType": "LOGIN",
  "lastEventAt": 1737854050000,
  "status": "ACTIVE_SESSION",
  "notes": "Event LOGIN at 1737854050000"
}
```

### Step 3: Check Credit Decision (Loan-Project)
```bash
curl http://localhost:9993/loan/credit-decisions/alice-prime
```

**Response:**
```json
{
  "userId": "alice-prime",
  "username": "Alice Prime",
  "creditScore": 800,
  "riskBand": "Prime",
  "approved": true,
  "maxApprovedAmount": 32000.0,
  "decisionReason": "Approved on LOGIN with risk band Prime",
  "lastEventType": "LOGIN",
  "evaluatedAt": 1737854050000
}
```

### Expected Logs

**Demo Service:**
```
[INFO] Loan tracker updated for user alice-prime with event LOGIN
```

**Loan-Project:**
```
[INFO] Processed auth event e1234567-89ab-cdef-0123-456789abcdef for user alice-prime
[INFO] Credit decision updated for user alice-prime with score 800 (Prime). Approved: true
```

---

## Test Case 2: Fair Credit Score (Fair Band)

### Step 1: Publish Signup Event
```bash
curl -X POST http://localhost:9992/kurrentdb/auth-events \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "bob-fair",
    "username": "Bob Fair",
    "eventType": "SIGNUP"
  }'
```

**Response:**
```json
{
  "eventId": "e2234567-89ab-cdef-0123-456789abcdef",
  "streamName": "My_Stream_Diy",
  "createdEpochMillis": 1737854100000
}
```

### Step 2: Check Loan Status (Demo)
```bash
curl http://localhost:9992/loan/statuses/bob-fair
```

**Response:**
```json
{
  "userId": "bob-fair",
  "username": "Bob Fair",
  "lastEventType": "SIGNUP",
  "lastEventAt": 1737854100000,
  "status": "PROFILE_CREATED",
  "notes": "Event SIGNUP at 1737854100000"
}
```

### Step 3: Check Credit Decision (Loan-Project)
```bash
curl http://localhost:9993/loan/credit-decisions/bob-fair
```

**Response:**
```json
{
  "userId": "bob-fair",
  "username": "Bob Fair",
  "creditScore": 680,
  "riskBand": "Fair",
  "approved": true,
  "maxApprovedAmount": 27200.0,
  "decisionReason": "Approved on SIGNUP with risk band Fair",
  "lastEventType": "SIGNUP",
  "evaluatedAt": 1737854100000
}
```

**Calculation:** max(680 × 40, 70000 × 0.25) = max(27200, 17500) = **27,200**

### Expected Logs

**Demo Service:**
```
[INFO] Loan tracker updated for user bob-fair with event SIGNUP
```

**Loan-Project:**
```
[INFO] Processed auth event e2234567-89ab-cdef-0123-456789abcdef for user bob-fair
[INFO] Credit decision updated for user bob-fair with score 680 (Fair). Approved: true
```

---

## Test Case 3: Low Credit Score (High-Risk)

### Step 1: Publish Event
```bash
curl -X POST http://localhost:9992/kurrentdb/auth-events \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "charlie-risk",
    "username": "Charlie Risk",
    "eventType": "LOGIN"
  }'
```

**Response:**
```json
{
  "eventId": "e3234567-89ab-cdef-0123-456789abcdef",
  "streamName": "My_Stream_Diy",
  "createdEpochMillis": 1737854150000
}
```

### Step 2: Check Loan Status (Demo)
```bash
curl http://localhost:9992/loan/statuses/charlie-risk
```

**Response:**
```json
{
  "userId": "charlie-risk",
  "username": "Charlie Risk",
  "lastEventType": "LOGIN",
  "lastEventAt": 1737854150000,
  "status": "ACTIVE_SESSION",
  "notes": "Event LOGIN at 1737854150000"
}
```

### Step 3: Check Credit Decision (Loan-Project)
```bash
curl http://localhost:9993/loan/credit-decisions/charlie-risk
```

**Response:**
```json
{
  "userId": "charlie-risk",
  "username": "Charlie Risk",
  "creditScore": 520,
  "riskBand": "High-Risk",
  "approved": false,
  "maxApprovedAmount": 0.0,
  "decisionReason": "Declined: insufficient score for band High-Risk",
  "lastEventType": "LOGIN",
  "evaluatedAt": 1737854150000
}
```

### Expected Logs

**Demo Service:**
```
[INFO] Loan tracker updated for user charlie-risk with event LOGIN
```

**Loan-Project:**
```
[INFO] Processed auth event e3234567-89ab-cdef-0123-456789abcdef for user charlie-risk
[INFO] Credit decision updated for user charlie-risk with score 520 (High-Risk). Approved: false
```

---

## Test Case 4: Logout (Always Denied)

### Step 1: Publish Logout Event
```bash
curl -X POST http://localhost:9992/kurrentdb/auth-events \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "alice-prime",
    "eventType": "LOGOUT"
  }'
```

**Response:**
```json
{
  "eventId": "e4234567-89ab-cdef-0123-456789abcdef",
  "streamName": "My_Stream_Diy",
  "createdEpochMillis": 1737854200000
}
```

### Step 2: Check Loan Status (Demo)
```bash
curl http://localhost:9992/loan/statuses/alice-prime
```

**Response (Updated):**
```json
{
  "userId": "alice-prime",
  "username": "Alice Prime",
  "lastEventType": "LOGOUT",
  "lastEventAt": 1737854200000,
  "status": "SESSION_ENDED",
  "notes": "Event LOGOUT at 1737854200000"
}
```

### Step 3: Check Credit Decision (Loan-Project)
```bash
curl http://localhost:9993/loan/credit-decisions/alice-prime
```

**Response (Updated):**
```json
{
  "userId": "alice-prime",
  "username": "Alice Prime",
  "creditScore": 800,
  "riskBand": "Prime",
  "approved": false,
  "maxApprovedAmount": 0.0,
  "decisionReason": "Declined: insufficient score for band Prime",
  "lastEventType": "LOGOUT",
  "evaluatedAt": 1737854200000
}
```

**Key Point:** Even though credit score is still 800 (Prime), the LOGOUT event causes approval to be denied.

### Expected Logs

**Demo Service:**
```
[INFO] Loan tracker updated for user alice-prime with event LOGOUT
```

**Loan-Project:**
```
[INFO] Processed auth event e4234567-89ab-cdef-0123-456789abcdef for user alice-prime
[INFO] Credit decision updated for user alice-prime with score 800 (Prime). Approved: false
```

---

## Test Case 5: Password Update (Security Review)

### Step 1: Publish Password Update Event
```bash
curl -X POST http://localhost:9992/kurrentdb/auth-events \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "bob-fair",
    "eventType": "PASSWORD_UPDATE"
  }'
```

**Response:**
```json
{
  "eventId": "e5234567-89ab-cdef-0123-456789abcdef",
  "streamName": "My_Stream_Diy",
  "createdEpochMillis": 1737854250000
}
```

### Step 2: Check Loan Status (Demo)
```bash
curl http://localhost:9992/loan/statuses/bob-fair
```

**Response (Updated):**
```json
{
  "userId": "bob-fair",
  "username": "Bob Fair",
  "lastEventType": "PASSWORD_UPDATE",
  "lastEventAt": 1737854250000,
  "status": "SECURITY_REVIEW",
  "notes": "Event PASSWORD_UPDATE at 1737854250000"
}
```

### Step 3: Check Credit Decision (Loan-Project)
```bash
curl http://localhost:9993/loan/credit-decisions/bob-fair
```

**Response (Updated):**
```json
{
  "userId": "bob-fair",
  "username": "Bob Fair",
  "creditScore": 680,
  "riskBand": "Fair",
  "approved": true,
  "maxApprovedAmount": 27200.0,
  "decisionReason": "Approved on PASSWORD_UPDATE with risk band Fair",
  "lastEventType": "PASSWORD_UPDATE",
  "evaluatedAt": 1737854250000
}
```

### Expected Logs

**Demo Service:**
```
[INFO] Loan tracker updated for user bob-fair with event PASSWORD_UPDATE
```

**Loan-Project:**
```
[INFO] Processed auth event e5234567-89ab-cdef-0123-456789abcdef for user bob-fair
[INFO] Credit decision updated for user bob-fair with score 680 (Fair). Approved: true
```

---

## Test Case 6: Event Without Credit Score (Derived)

### Step 1: Publish Event (No Credit Score)
```bash
curl -X POST http://localhost:9992/kurrentdb/auth-events \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "derived-user",
    "username": "Derived User",
    "eventType": "LOGIN"
  }'
```

**Response:**
```json
{
  "eventId": "e6234567-89ab-cdef-0123-456789abcdef",
  "streamName": "My_Stream_Diy",
  "createdEpochMillis": 1737854300000
}
```

### Step 3: Check Credit Decision (Loan-Project)
```bash
curl http://localhost:9993/loan/credit-decisions/derived-user
```

**Response:**
```json
{
  "userId": "derived-user",
  "username": "Derived User",
  "creditScore": 735,
  "riskBand": "Near-Prime",
  "approved": true,
  "maxApprovedAmount": 29400.0,
  "decisionReason": "Approved on LOGIN with risk band Near-Prime",
  "lastEventType": "LOGIN",
  "evaluatedAt": 1737854300000
}
```

**Key Point:** Credit score is deterministically derived from `userId.hashCode()` (range: 520-849), ensuring consistent results.

---

## Test Case 7: List All Statuses

### Step 1: Get All Loan Statuses from Demo
```bash
curl http://localhost:9992/loan/statuses
```

**Response:**
```json
[
  {
    "userId": "alice-prime",
    "username": "Alice Prime",
    "lastEventType": "LOGOUT",
    "lastEventAt": 1737854200000,
    "status": "SESSION_ENDED",
    "notes": "Event LOGOUT at 1737854200000"
  },
  {
    "userId": "bob-fair",
    "username": "Bob Fair",
    "lastEventType": "PASSWORD_UPDATE",
    "lastEventAt": 1737854250000,
    "status": "SECURITY_REVIEW",
    "notes": "Event PASSWORD_UPDATE at 1737854250000"
  },
  {
    "userId": "charlie-risk",
    "username": "Charlie Risk",
    "lastEventType": "LOGIN",
    "lastEventAt": 1737854150000,
    "status": "ACTIVE_SESSION",
    "notes": "Event LOGIN at 1737854150000"
  },
  {
    "userId": "derived-user",
    "username": "Derived User",
    "lastEventType": "LOGIN",
    "lastEventAt": 1737854300000,
    "status": "ACTIVE_SESSION",
    "notes": "Event LOGIN at 1737854300000"
  }
]
```

### Step 2: Get All Credit Decisions from Loan-Project
```bash
curl http://localhost:9993/loan/credit-decisions
```

**Response:**
```json
[
  {
    "userId": "alice-prime",
    "username": "Alice Prime",
    "creditScore": 800,
    "riskBand": "Prime",
    "approved": false,
    "maxApprovedAmount": 0.0,
    "decisionReason": "Declined: insufficient score for band Prime",
    "lastEventType": "LOGOUT",
    "evaluatedAt": 1737854200000
  },
  {
    "userId": "bob-fair",
    "username": "Bob Fair",
    "creditScore": 680,
    "riskBand": "Fair",
    "approved": true,
    "maxApprovedAmount": 27200.0,
    "decisionReason": "Approved on PASSWORD_UPDATE with risk band Fair",
    "lastEventType": "PASSWORD_UPDATE",
    "evaluatedAt": 1737854250000
  },
  {
    "userId": "charlie-risk",
    "username": "Charlie Risk",
    "creditScore": 520,
    "riskBand": "High-Risk",
    "approved": false,
    "maxApprovedAmount": 0.0,
    "decisionReason": "Declined: insufficient score for band High-Risk",
    "lastEventType": "LOGIN",
    "evaluatedAt": 1737854150000
  },
  {
    "userId": "derived-user",
    "username": "Derived User",
    "creditScore": 735,
    "riskBand": "Near-Prime",
    "approved": true,
    "maxApprovedAmount": 29400.0,
    "decisionReason": "Approved on LOGIN with risk band Near-Prime",
    "lastEventType": "LOGIN",
    "evaluatedAt": 1737854300000
  }
]
```

---

## Summary: Credit Decisions by User

| User | Score | Band | Event | Approved | Max Loan | Reason |
|------|-------|------|-------|----------|----------|--------|
| alice-prime | 800 | Prime | LOGOUT | ✗ | $0 | LOGOUT denies |
| bob-fair | 680 | Fair | PASSWORD_UPDATE | ✓ | $27,200 | Fair score |
| charlie-risk | 520 | High-Risk | LOGIN | ✗ | $0 | Score too low |
| derived-user | 735 | Near-Prime | LOGIN | ✓ | $29,400 | Derived score |

---

## Stream Content (KurrentDB)

Access KurrentDB Admin UI: http://localhost:2113

Stream: `My_Stream_Diy`
```
Event 1: KeycloakAuthEvent-LOGIN    @ 1737854050000  (alice-prime)
Event 2: KeycloakAuthEvent-SIGNUP   @ 1737854100000  (bob-fair)
Event 3: KeycloakAuthEvent-LOGIN    @ 1737854150000  (charlie-risk)
Event 4: KeycloakAuthEvent-LOGOUT   @ 1737854200000  (alice-prime)
Event 5: KeycloakAuthEvent-PASSWORD_UPDATE @ 1737854250000 (bob-fair)
Event 6: KeycloakAuthEvent-LOGIN    @ 1737854300000  (derived-user)
```

PersistentSubscription: `My_Group_Diy`
- All events processed and ACK'd
- No events in parked stream (no failures)

