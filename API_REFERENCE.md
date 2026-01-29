# Loan POC - API Reference & Architecture

## System Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                          POSTMAN Client                      │
│         (Manual HTTP POST/GET requests for testing)          │
└────────────────────────┬─────────────────────────────────────┘
                         │
          POST /kurrentdb/auth-events
          GET  /loan/statuses/{userId}
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│          Demo Service (Publisher) - Port 9992                │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  AuthEventsController                               │   │
│  │  - POST /kurrentdb/auth-events                      │   │
│  │  - UserAuthEventPublisher.publish(event)            │   │
│  └─────────────────────────────────────────────────────┘   │
│                         │                                   │
│                         │ AppendToStream                    │
│                         ▼                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  KurrentDb Config                                   │   │
│  │  - EventStoreDBClient (basic append)                │   │
│  │  - EventStoreDBClientSettings                       │   │
│  │  - EventStoreDBPersistentSubscriptionsClient        │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  LoanTrackingService (in-memory)                    │   │
│  │  - handleUserEvent(event)                           │   │
│  │  - getAllStatuses()                                 │   │
│  │  - getStatus(userId)                                │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  LoanProcessingController                           │   │
│  │  - GET /loan/statuses                               │   │
│  │  - GET /loan/statuses/{userId}                      │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                              │
└──────────────────────────────────────────────────────────────┘
                         │
           AppendToStream to My_Stream_Diy
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│      KurrentDB Event Store                                   │
│                                                              │
│  Stream: My_Stream_Diy                                       │
│  ├─ KeycloakAuthEvent-LOGIN    (user123, ts1)              │
│  ├─ KeycloakAuthEvent-SIGNUP   (user456, ts2)              │
│  ├─ KeycloakAuthEvent-LOGIN    (user789, ts3)              │
│  ├─ KeycloakAuthEvent-LOGOUT   (user123, ts4)              │
│  └─ ...                                                      │
│                                                              │
│  PersistentSubscription: My_Group_Diy                        │
│  └─ Competing Consumers (can scale horizontally)            │
│                                                              │
└──────────────────────────────────────────────────────────────┘
                         │
         Subscribe to My_Stream_Diy/My_Group_Diy
         ACK/NACK handling with Park on failure
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│      Loan-Project Service (Subscriber) - Port 9993           │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  UserAuthSubscriptionHandler                        │   │
│  │  - @PostConstruct: ensurePersistentSubscription()   │   │
│  │  - subscribe()                                      │   │
│  │  - handleEvent(subscription, event, retryCount)     │   │
│  │  - ACK on success, NACK (Park) on failure           │   │
│  └─────────────────────────────────────────────────────┘   │
│                         │                                   │
│         ┌───────────────┼───────────────┐                  │
│         │               │               │                  │
│         ▼               ▼               ▼                  │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐      │
│  │ LoanTracking │ │   Credit     │ │   Parsing    │      │
│  │   Service    │ │  Approval    │ │   Logic      │      │
│  │              │ │   Service    │ │              │      │
│  │ In-Memory:   │ │              │ │ Infer type   │      │
│  │ User → Loan  │ │ In-Memory:   │ │ from event   │      │
│  │ Status       │ │ User → Credit│ │ type name    │      │
│  │              │ │ Decision     │ │              │      │
│  └──────────────┘ └──────────────┘ └──────────────┘      │
│         │               │                                   │
│         └───────────────┼───────────────┘                  │
│                         │                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  LoanProcessingController                           │   │
│  │  - GET /loan/credit-decisions                       │   │
│  │  - GET /loan/credit-decisions/{userId}              │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                              │
└──────────────────────────────────────────────────────────────┘
                         │
          GET /loan/credit-decisions/{userId}
          GET /loan/credit-decisions
                         │
                         ▼
                   POSTMAN Client
           (View credit decisions & loan status)
```

---

## Event Flow Sequence

```
1. POSTMAN
   └─→ POST /kurrentdb/auth-events
       {userId, username, creditScore, eventType, ...}

2. DEMO SERVICE (UserAuthEventPublisher)
   ├─→ Normalize event data
   ├─→ Generate UUID eventId
   └─→ AppendToStream(My_Stream_Diy, KeycloakAuthEvent-{TYPE})

3. KURRENTDB
   ├─→ Persist event to My_Stream_Diy
   └─→ Notify subscribers (My_Group_Diy)

4. LOAN-PROJECT (UserAuthSubscriptionHandler)
   ├─→ Receive event from persistent subscription
   ├─→ Parse UserAuthEvent JSON
   ├─→ LoanTrackingService.handleUserEvent()
   │   └─→ Update in-memory loan status
   ├─→ CreditApprovalService.evaluate()
   │   └─→ Update in-memory credit decision
   └─→ subscription.ack(event) or nack(event)

5. POSTMAN
   ├─→ GET /loan/statuses/{userId}          (Demo)
   │   └─→ Returns: LoanStatus
   └─→ GET /loan/credit-decisions/{userId}  (Loan-Project)
       └─→ Returns: CreditDecision
```

---

## API Endpoints

### Demo Service (Port 9992) - Publisher

#### Health Check
```
GET /health
Content-Type: application/json

Response (200):
{
  "status": "UP"
}
```

#### Publish Auth Event
```
POST /kurrentdb/auth-events
Content-Type: application/json

Request Body:
{
  "userId": "user123",                    // Required: unique identifier
  "username": "alice",                    // Optional: display name
  "email": "alice@example.com",          // Optional: email address
  "fullName": "Alice Johnson",           // Optional: full name
  "country": "US",                       // Optional: country code
  "creditScore": 750,                    // Optional: credit score (300-900)
  "annualIncome": 120000,                // Optional: annual income
  "eventType": "LOGIN",                  // Required: SIGNUP|LOGIN|LOGOUT|PASSWORD_UPDATE
  "occurredAt": 1737854050123            // Optional: epoch millis (defaults to now)
}

Response (201 Created):
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "streamName": "My_Stream_Diy",
  "createdEpochMillis": 1737854050123
}

Error (400):
{
  "timestamp": "...",
  "status": 400,
  "error": "Bad Request",
  "message": "..."
}
```

#### Get All Loan Statuses
```
GET /loan/statuses
Content-Type: application/json

Response (200):
[
  {
    "userId": "user123",
    "username": "alice",
    "lastEventType": "LOGIN",
    "lastEventAt": 1737854050123,
    "status": "ACTIVE_SESSION",
    "notes": "Event LOGIN at 1737854050123"
  },
  {
    "userId": "user456",
    "username": "bob",
    "lastEventType": "SIGNUP",
    "lastEventAt": 1737854100000,
    "status": "PROFILE_CREATED",
    "notes": "Event SIGNUP at 1737854100000, onboarding from UK"
  }
]
```

#### Get Loan Status by User
```
GET /loan/statuses/{userId}
Content-Type: application/json

Path Parameters:
- userId: string (unique user identifier)

Response (200):
{
  "userId": "user123",
  "username": "alice",
  "lastEventType": "LOGIN",
  "lastEventAt": 1737854050123,
  "status": "ACTIVE_SESSION",
  "notes": "Event LOGIN at 1737854050123"
}

Response (404 Not Found):
{
  "timestamp": "...",
  "status": 404,
  "error": "Not Found"
}
```

---

### Loan-Project Service (Port 9993) - Subscriber

#### Health Check
```
GET /health
Content-Type: application/json

Response (200):
{
  "status": "UP",
  "service": "loan-project"
}
```

#### Get All Credit Decisions
```
GET /loan/credit-decisions
Content-Type: application/json

Response (200):
[
  {
    "userId": "user123",
    "username": "alice",
    "creditScore": 750,
    "riskBand": "Prime",
    "approved": true,
    "maxApprovedAmount": 30000.0,
    "decisionReason": "Approved on LOGIN with risk band Prime",
    "lastEventType": "LOGIN",
    "evaluatedAt": 1737854050123
  },
  {
    "userId": "user456",
    "username": "bob",
    "creditScore": 620,
    "riskBand": "Fair",
    "approved": true,
    "maxApprovedAmount": 12500.0,
    "decisionReason": "Approved on SIGNUP with risk band Fair",
    "lastEventType": "SIGNUP",
    "evaluatedAt": 1737854100000
  },
  {
    "userId": "user789",
    "username": "charlie",
    "creditScore": 500,
    "riskBand": "High-Risk",
    "approved": false,
    "maxApprovedAmount": 0.0,
    "decisionReason": "Declined: insufficient score for band High-Risk",
    "lastEventType": "LOGIN",
    "evaluatedAt": 1737854200000
  }
]
```

#### Get Credit Decision by User
```
GET /loan/credit-decisions/{userId}
Content-Type: application/json

Path Parameters:
- userId: string (unique user identifier)

Response (200):
{
  "userId": "user123",
  "username": "alice",
  "creditScore": 750,
  "riskBand": "Prime",
  "approved": true,
  "maxApprovedAmount": 30000.0,
  "decisionReason": "Approved on LOGIN with risk band Prime",
  "lastEventType": "LOGIN",
  "evaluatedAt": 1737854050123
}

Response (404 Not Found):
{
  "timestamp": "...",
  "status": 404,
  "error": "Not Found"
}
```

---

## Data Models

### UserAuthEvent (Input)
```typescript
{
  eventId?: string                    // Generated if omitted
  userId: string                      // Required
  username?: string
  email?: string
  fullName?: string
  country?: string
  creditScore?: number                // 300-900, derived if omitted
  annualIncome?: number
  eventType: "SIGNUP"|"LOGIN"|"LOGOUT"|"PASSWORD_UPDATE"
  occurredAt?: number                 // epoch millis, defaults to now()
}
```

### LoanStatus (Output from Demo)
```typescript
{
  userId: string
  username?: string
  lastEventType: "SIGNUP"|"LOGIN"|"LOGOUT"|"PASSWORD_UPDATE"
  lastEventAt: number                 // epoch millis
  status: string                      // PROFILE_CREATED|ACTIVE_SESSION|SESSION_ENDED|SECURITY_REVIEW|UNKNOWN
  notes: string                       // Human-readable status notes
}
```

### CreditDecision (Output from Loan-Project)
```typescript
{
  userId: string
  username?: string
  creditScore: number                 // 300-900
  riskBand: string                    // Prime|Near-Prime|Fair|Subprime|High-Risk
  approved: boolean
  maxApprovedAmount: number           // 0 if not approved
  decisionReason: string              // Explanation of decision
  lastEventType: "SIGNUP"|"LOGIN"|"LOGOUT"|"PASSWORD_UPDATE"
  evaluatedAt: number                 // epoch millis
}
```

---

## Event Types & Mapping

| Event Type | LoanStatus | Credit Approved | Notes |
|------------|-----------|-----------------|-------|
| SIGNUP | PROFILE_CREATED | ✓ (if score ≥ 640) | New account created |
| LOGIN | ACTIVE_SESSION | ✓ (if score ≥ 640) | User session active |
| LOGOUT | SESSION_ENDED | ✗ Always denied | Session ended |
| PASSWORD_UPDATE | SECURITY_REVIEW | ✓ (if score ≥ 640) | Password changed, security check |

---

## Credit Scoring Logic

### Risk Bands
```
Score Range  | Risk Band     | Approval | Max Loan Amount
─────────────┼───────────────┼──────────┼─────────────────────
760+         | Prime         | ✓        | score × 40
700-759      | Near-Prime    | ✓        | score × 40
640-699      | Fair          | ✓        | max(score × 40, income × 0.25)
580-639      | Subprime      | ✗        | $0
<580         | High-Risk     | ✗        | $0
```

### Special Cases
- **LOGOUT Event**: Always results in `approved: false` regardless of score
- **No Credit Score Provided**: Deterministically derived from `userId.hashCode()`
  - Ensures consistent results across service restarts
  - Range: 520-849 (always Fair or better)

### Example Calculations
```
User: alice, creditScore=750
├─ riskBand: "Prime" (760+)
├─ approved: true
├─ maxApprovedAmount: 750 × 40 = 30,000
└─ decisionReason: "Approved on LOGIN with risk band Prime"

User: bob, creditScore=620, annualIncome=50000
├─ riskBand: "Fair" (640-699)
├─ approved: true
├─ maxApprovedAmount: max(620 × 40, 50000 × 0.25) = max(24800, 12500) = 24,800
└─ decisionReason: "Approved on SIGNUP with risk band Fair"

User: charlie, creditScore=500, eventType="LOGOUT"
├─ riskBand: "High-Risk" (<580)
├─ approved: false
├─ maxApprovedAmount: 0
└─ decisionReason: "Declined: insufficient score for band High-Risk"
```

---

## Error Handling

### Event Processing Errors
- **Invalid JSON**: Returns 400 Bad Request
- **Missing userId**: Returns 400 Bad Request
- **KurrentDB Connection Failed**: Returns 503 Service Unavailable

### Subscription Errors
- **Failed to Parse Event**: Event is NACKed to parked stream
- **Service Exception**: Event is NACKed to parked stream
- **Already Exists Subscription**: Service reuses existing subscription (normal)

### API Errors
- **User Not Found**: Returns 404 Not Found
- **Invalid Path Parameter**: Returns 400 Bad Request

---

## Performance Characteristics

### Latency
- **Event Publish**: ~50-100ms (KurrentDB append)
- **Event Subscribe**: ~100-200ms (stream delivery + processing)
- **Query Results**: <5ms (in-memory lookup)

### Throughput
- **Single Service**: ~1,000 events/second
- **Loan-Project Subscription**: Scales horizontally with competing consumers

### Storage
- **In-Memory Loan Status**: O(n) where n = unique users
- **In-Memory Credit Decisions**: O(n) where n = unique users
- **KurrentDB Stream**: Persisted (unlimited)

---

## Testing Scenarios

See [POC_GUIDE.md](POC_GUIDE.md) for detailed test scenarios including:
- Login with high credit score (approval)
- Signup with fair credit score (approval)
- Login with low credit score (denial)
- Logout event (denial regardless of score)
- Password update event

