# POC Setup - Manual Steps

If you prefer to set up manually instead of using the automated script, follow these steps:

## Prerequisites
- Docker & Docker Compose installed
- Java 21 installed
- Maven 3.8+
- Postman or curl

---

## Phase 1: Infrastructure Setup (KurrentDB, MongoDB, etc.)

### Step 1.1: Start Docker Services
```bash
cd Infra
docker compose up
```

Wait for output showing all services are healthy. In another terminal, verify:
```bash
# Check KurrentDB
curl http://localhost:2113/health
# Should return: {"state":"Live"}

# Check MongoDB
nc -zv localhost 27017
# Should return: Connection successful

# Check MinIO
curl http://localhost:9000/health/live
# Should return healthy response
```

### Step 1.2: Verify All Services Running
```bash
docker compose ps
```

Expected output:
```
NAME              STATUS
mongo_server      Up
minio_server      Up
kurrentdb.db      Up
keycloak_web      Up
keycloak_db       Up
```

---

## Phase 2: Build Services

### Step 2.1: Build Demo Service (Publisher)
```bash
cd App/spring-project/demo
./mvnw clean package -DskipTests
```

Expected: `BUILD SUCCESS`

### Step 2.2: Build Loan-Project Service (Subscriber)
```bash
cd App/spring-project/loan-project
./mvnw clean package -DskipTests
```

Expected: `BUILD SUCCESS`

---

## Phase 3: Run Services

### Step 3.1: Start Demo Service (Publisher)
Open **Terminal 1**:
```bash
cd App/spring-project/demo
./mvnw spring-boot:run
```

Wait for:
```
Started DemoApplication in X.XXX seconds
```

Verify:
```bash
# In Terminal 2
curl http://localhost:9992/health
# Should return: {"status":"UP"}
```

### Step 3.2: Start Loan-Project Service (Subscriber)
Open **Terminal 2**:
```bash
cd App/spring-project/loan-project
./mvnw spring-boot:run
```

Wait for logs showing:
```
Started LoanProjectApplication in X.XXX seconds
... Created persistent subscription My_Stream_Diy for group My_Group_Diy
... Attached to persistent subscription My_Stream_Diy / My_Group_Diy with id ...
```

Verify:
```bash
# In Terminal 3
curl http://localhost:9993/health
# Should return: {"status":"UP","service":"loan-project"}
```

---

## Phase 4: Test with Postman

### Option A: Import Collection
1. Open Postman
2. Click "Collections" → "Import"
3. Select file: `Loan-POC.postman_collection.json`
4. Run requests in order

### Option B: Manual Requests

#### 4.1: Publish Login Event
```bash
curl -X POST http://localhost:9992/kurrentdb/auth-events \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "alice-prime",
    "username": "Alice Prime",
    "eventType": "LOGIN",
    "occurredAt": 1737854050123
  }'
```

Expected response:
```json
{
  "eventId": "...",
  "streamName": "My_Stream_Diy",
  "createdEpochMillis": 1737854050123
}
```

#### 4.2: Check Loan Status (Demo Service)
```bash
curl http://localhost:9992/loan/statuses/alice-prime
```

Expected:
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

#### 4.3: Check Credit Decision (Loan-Project Service)
```bash
curl http://localhost:9993/loan/credit-decisions/alice-prime
```

Expected:
```json
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
```

---

## Phase 5: Test More Scenarios

### Scenario 1: Signup with Fair Profile
```bash
curl -X POST http://localhost:9992/kurrentdb/auth-events \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "bob-fair",
    "username": "Bob Fair",
    "eventType": "SIGNUP"
  }'

# Check result
curl http://localhost:9993/loan/credit-decisions/bob-fair
```

Expected: `creditScore: 680`, `riskBand: "Fair"`, `approved: true`, `maxApprovedAmount: 27200`

### Scenario 2: Login with High-Risk Profile
```bash
curl -X POST http://localhost:9992/kurrentdb/auth-events \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "charlie-risk",
    "username": "Charlie Risk",
    "eventType": "LOGIN"
  }'

# Check result
curl http://localhost:9993/loan/credit-decisions/charlie-risk
```

Expected: `creditScore: 520`, `riskBand: "High-Risk"`, `approved: false`, `decisionReason: "Declined: insufficient score for band High-Risk"`

### Scenario 3: Logout (Always Denied)
```bash
curl -X POST http://localhost:9992/kurrentdb/auth-events \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "alice-prime",
    "username": "Alice Prime",
    "eventType": "LOGOUT"
  }'

# Check result - should show approved: false even though profile is Prime
curl http://localhost:9993/loan/credit-decisions/alice-prime
```

Expected: `approved: false` (LOGOUT denies approval)

---

## Phase 6: Monitoring & Logs

### Watch Demo Service Logs
```bash
# In the demo terminal, observe:
# [INFO] Loan tracker updated for user alice-prime with event LOGIN
# [INFO] KeycloakAuthEvent-LOGIN appended to stream
```

### Watch Loan-Project Logs
```bash
# In the loan-project terminal, observe:
# [INFO] Processed auth event ... for user alice-prime
# [INFO] Credit decision updated for user alice-prime with score 800 (Prime). Approved: true
```

### Check Stream in KurrentDB Admin UI
1. Open http://localhost:2113
2. Login with default credentials (if required)
3. Navigate to stream browser
4. Find stream: `My_Stream_Diy`
5. Verify events are being appended

---

## Phase 7: Stopping Services

### Stop Loan-Project
```bash
# Press Ctrl+C in Terminal 2
```

### Stop Demo Service
```bash
# Press Ctrl+C in Terminal 1
```

### Stop Infrastructure
```bash
cd Infra
docker compose down
```

To completely clean up:
```bash
docker compose down -v  # Also removes volumes
```

---

## Troubleshooting

### Demo Service Fails to Connect to KurrentDB
**Problem**: `java.net.ConnectException: Connection refused`
**Solution**:
```bash
# Verify KurrentDB is running
curl http://localhost:2113/health

# If not running, start infrastructure
cd Infra
docker compose up
```

### Loan-Project Won't Subscribe
**Problem**: "Persistent subscription already exists" error
**Solution**: This is expected behavior. The service catches this error and reuses existing subscription.
Check logs for: `Persistent subscription ... already exists`

### Events Not Processing
**Problem**: Published events appear but loan-project doesn't update
**Solution**:
1. Check loan-project is running: `curl http://localhost:9993/health`
2. Check logs for subscription attachment: `Attached to persistent subscription`
3. Verify event is valid JSON with required fields

### Wrong Risk Band
**Problem**: Risk band shows as "Unknown"
**Solution**: Ensure `creditScore` is provided in event. If omitted, service derives from userId hash.

---

## Quick Command Reference

```bash
# Health checks
curl http://localhost:9992/health            # Demo
curl http://localhost:9993/health            # Loan-Project
curl http://localhost:2113/health            # KurrentDB

# Publish event
curl -X POST http://localhost:9992/kurrentdb/auth-events \
  -H "Content-Type: application/json" \
  -d '{"userId":"X","username":"Y","eventType":"LOGIN",...}'

# Query loan status
curl http://localhost:9992/loan/statuses/{userId}
curl http://localhost:9992/loan/statuses              # All

# Query credit decision
curl http://localhost:9993/loan/credit-decisions/{userId}
curl http://localhost:9993/loan/credit-decisions      # All

# View infrastructure
docker compose -f Infra/docker-compose.yaml ps
docker compose -f Infra/docker-compose.yaml logs

# Stop everything
docker compose -f Infra/docker-compose.yaml down
```

