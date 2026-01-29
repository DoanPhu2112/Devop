# POC: Publisher (Postman) → KurrentDB Stream → Subscriber (Loan-Project)

## Overview

This POC demonstrates:
1. **Publisher**: Demo service (port 9992) - receives events from Postman and publishes to KurrentDB stream `My_Stream_Diy`
2. **Stream**: KurrentDB persistent subscription `My_Stream_Diy/My_Group_Diy`
3. **Subscriber**: Loan-project service (port 9993) - subscribes and processes events, updates loan/credit snapshots

## Architecture Diagram

```
Postman (HTTP Client)
    ↓ POST /kurrentdb/auth-events
Demo Service (Publisher) port 9992
    ↓ AppendToStream
KurrentDB Stream: My_Stream_Diy
    ↓ PersistentSubscription: My_Group_Diy
Loan-Project Service (Subscriber) port 9993
    ↓ Process & ACK/NACK
    In-Memory: LoanStatus, CreditDecision
    ↓ Query via REST API
Postman (Read Results)
```

---

## Step 1: Start Infrastructure (KurrentDB, MongoDB, Keycloak, MinIO)

### From Infra folder:
```bash
cd Infra
docker compose up
```

Wait for all services to be healthy. Expected services:
- **KurrentDB**: http://localhost:2113 (admin UI)
- **MongoDB**: localhost:27017
- **MinIO**: http://localhost:9000 (minio_server, credentials: phudvq / Ph12345678!)
- **Keycloak**: http://localhost:8080

Verify KurrentDB is running:
```bash
curl http://localhost:2113/health
```

Expected response: `{"state":"Live"}`

---

## Step 2: Build & Start Demo Service (Publisher)

### Build:
```bash
cd App/spring-project/demo
./mvnw clean package
```

### Run:
```bash
./mvnw spring-boot:run
```

Expected output:
```
... Started DemoApplication in X seconds ...
```

Verify health:
```bash
curl http://localhost:9992/health
```

---

## Step 3: Build & Start Loan-Project Service (Subscriber)

### In a new terminal:
```bash
cd App/spring-project/loan-project
./mvnw clean package
```

### Run:
```bash
./mvnw spring-boot:run
```

Expected output:
```
... Started LoanProjectApplication in X seconds ...
... Created persistent subscription My_Stream_Diy for group My_Group_Diy
... Attached to persistent subscription My_Stream_Diy / My_Group_Diy with id ...
```

Verify health:
```bash
curl http://localhost:9993/health
```

---

## Step 4: Test with Postman

### Import Collection (or create requests manually)

#### Request 1: Publish Auth Event (Login)

**Method**: `POST`
**URL**: `http://localhost:9992/kurrentdb/auth-events`
**Headers**: 
```
Content-Type: application/json
```
**Body** (JSON):
```json
{
  "userId": "alice-prime",
  "username": "Alice Prime",
  "eventType": "LOGIN",
  "occurredAt": 1737854050123
}
```

**Expected Response** (201 Created):
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "streamName": "My_Stream_Diy",
  "createdEpochMillis": 1737854050123
}
```

---

#### Request 2: Query Loan Status (Demo Service)

**Method**: `GET`
**URL**: `http://localhost:9992/loan/statuses/alice-prime`

**Expected Response** (200 OK):
```json
{
  "userId": "alice-prime",
  "username": "Alice Prime",
  "lastEventType": "LOGIN",
  "lastEventAt": 1737854050123,
  "status": "ACTIVE_SESSION",
  "notes": "Event LOGIN at 1737854050123"
}
```

> **Note**: Demo service only tracks loan status, does NOT handle credit decisions anymore.

---

#### Request 3: Query Credit Decision (Loan-Project Service)

**Method**: `GET`
**URL**: `http://localhost:9993/loan/credit-decisions/alice-prime`

**Expected Response** (200 OK):
```json
{
  "userId": "alice-prime",
  "username": "Alice Prime",
  "creditScore": 800,
  "riskBand": "Prime",
  "approved": true,
  "maxApprovedAmount": 30000.0,
  "decisionReason": "Approved on LOGIN with risk band Prime",
  "lastEventType": "LOGIN",
  "evaluatedAt": 1737854050123
}
```

---

#### Request 4: List All Loan Statuses

**Method**: `GET`
**URL**: `http://localhost:9992/loan/statuses`

**Expected Response** (200 OK):
```json
[
  {
    "userId": "alice-prime",
    "username": "Alice Prime",
    "lastEventType": "LOGIN",
    "lastEventAt": 1737854050123,
    "status": "ACTIVE_SESSION",
    "notes": "Event LOGIN at 1737854050123"
  }
]
```

---

#### Request 5: List All Credit Decisions

**Method**: `GET`
**URL**: `http://localhost:9993/loan/credit-decisions`

**Expected Response** (200 OK):
```json
[
  {
    "userId": "alice-prime",
    "username": "Alice Prime",
    "creditScore": 800,
    "riskBand": "Prime",
    "approved": true,
    "maxApprovedAmount": 37500.0,
    "decisionReason": "Approved on LOGIN with risk band Prime",
    "lastEventType": "LOGIN",
    "evaluatedAt": 1737854050123
  }
]
```

---

## Step 5: Test Multiple Scenarios

### Scenario A: Signup Event (Bob Fair)

```json
{
  "userId": "bob-fair",
  "username": "Bob Fair",
  "eventType": "SIGNUP"
}
```

Expected:
- Demo: `status: "PROFILE_CREATED"`
- Loan-Project: `creditScore: 680`, `riskBand: "Fair"`, `approved: true`, `maxApprovedAmount: 27200`

---

### Scenario B: High-Risk Profile

```json
{
  "userId": "charlie-risk",
  "username": "Charlie Risk",
  "eventType": "LOGIN"
}
```

Expected:
- Demo: `status: "ACTIVE_SESSION"`
- Loan-Project: `creditScore: 520`, `riskBand: "High-Risk"`, `approved: false`, `maxApprovedAmount: 0.0`

---

### Scenario C: Logout Event (Always Denied)

```json
{
  "userId": "alice-prime",
  "username": "Alice Prime",
  "eventType": "LOGOUT"
}
```

Expected:
- Demo: `status: "SESSION_ENDED"`
- Loan-Project: `approved: false` (LOGOUT denies approval even for Prime)

---

## Step 6: Monitor Logs & Verify Event Flow

### Demo Service Logs:
```
... Loan tracker updated for user alice-prime with event LOGIN
```

### Loan-Project Service Logs:
```
... Attached to persistent subscription My_Stream_Diy / My_Group_Diy with id ...
... Processed auth event ... for user alice-prime
... Credit decision updated for user alice-prime with score 800 (Prime). Approved: true
```

---

## Troubleshooting

### Issue: KurrentDB connection refused
**Solution**: Ensure Docker is running and KurrentDB is healthy:
```bash
curl http://localhost:2113/health
```

### Issue: Demo service startup fails with "connection refused"
**Solution**: Make sure KurrentDB is fully started before running demo:
```bash
docker compose logs kurrentdb.db
```

### Issue: Loan-project not receiving events
**Check**:
1. Is persistent subscription created? (check logs for "Created persistent subscription")
2. Are events being published? (POST to demo's endpoint and check response)
3. Is loan-project attached? (check logs for "Attached to persistent subscription")

### Issue: Credit decision showing `riskBand: "Unknown"`
**Solution**: Use one of the hardcoded profiles (`alice-prime`, `bob-fair`, `charlie-risk`, `derived-user`) or let the service fall back to the default profile by supplying any userId.

---

## API Endpoint Summary

### Demo Service (Port 9992)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/kurrentdb/auth-events` | Publish user auth event to stream |
| GET | `/loan/statuses` | List all loan statuses |
| GET | `/loan/statuses/{userId}` | Get loan status for user |
| GET | `/health` | Health check |

### Loan-Project Service (Port 9993)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/loan/credit-decisions` | List all credit decisions |
| GET | `/loan/credit-decisions/{userId}` | Get credit decision for user |
| GET | `/health` | Health check |

---

## Event Types Supported

- **SIGNUP**: User account creation → Status: `PROFILE_CREATED`
- **LOGIN**: User login → Status: `ACTIVE_SESSION`, Approval: based on score
- **LOGOUT**: User logout → Status: `SESSION_ENDED`, Approval: denied
- **PASSWORD_UPDATE**: Password changed → Status: `SECURITY_REVIEW`

---

## Credit Scoring Rules

| Score Range | Risk Band | Default Action |
|-------------|-----------|-----------------|
| 760+ | Prime | ✅ Approved (max: score × 40) |
| 700-759 | Near-Prime | ✅ Approved (max: score × 40) |
| 640-699 | Fair | ✅ Approved (max: max(score × 40, income × 0.25)) |
| 580-639 | Subprime | ❌ Declined |
| <580 | High-Risk | ❌ Declined |

> **Note**: LOGOUT events always result in denied approval regardless of score.

---

## Next Steps

1. **Extend Publisher**: Add Keycloak event hook to auto-publish auth events
2. **Add Persistence**: Store loan/credit decisions in database instead of in-memory
3. **Add Notifications**: WebSocket or SSE to push updates to clients
4. **Add Authorization**: Integrate with Keycloak for API security
5. **Add Audit Trail**: Log all decision reasoning to separate event stream

