# Loan POC - Quick Reference & Getting Started

## What You Have

A complete publisher-subscriber proof of concept using KurrentDB event streaming:

```
Postman → Demo Service (9992) → KurrentDB Stream → Loan-Project (9993) → Results
```

---

## Quick Start (Choose One)

### Option 1: Automated Script (Recommended)
```bash
cd /home/phu/Company/Work/devop/Devop
./poc-start.sh
```

### Option 2: Manual Setup
Follow step-by-step instructions in [MANUAL_SETUP.md](MANUAL_SETUP.md)

### Option 3: Docker Only (Infra)
```bash
cd Infra
docker compose up
```

Then start services separately in terminals.

---

## Run Services

### Terminal 1: Demo Service (Publisher)
```bash
cd App/spring-project/demo
./mvnw spring-boot:run
# Waits on port 9992
```

### Terminal 2: Loan-Project Service (Subscriber)
```bash
cd App/spring-project/loan-project
./mvnw spring-boot:run
# Waits on port 9993
```

---

## Test with Postman

### Option 1: Import Collection
1. Open Postman
2. Collections → Import
3. Select: `Loan-POC.postman_collection.json`
4. Run requests

### Option 2: Use curl

**Publish Event:**
```bash
curl -X POST http://localhost:9992/kurrentdb/auth-events \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "username": "alice",
    "creditScore": 750,
    "eventType": "LOGIN"
  }'
```

**Check Loan Status:**
```bash
curl http://localhost:9992/loan/statuses/user123
```

**Check Credit Decision:**
```bash
curl http://localhost:9993/loan/credit-decisions/user123
```

---

## What Happens Behind the Scenes

1. **Postman sends** event to Demo (port 9992)
2. **Demo publishes** to KurrentDB stream `My_Stream_Diy`
3. **KurrentDB notifies** persistent subscription `My_Group_Diy`
4. **Loan-Project receives** and processes:
   - Updates loan tracking service
   - Evaluates credit with risk bands
   - Stores in-memory snapshots
5. **Postman queries** loan-project (port 9993) for results

---

## File Guide

| File | Purpose |
|------|---------|
| [POC_GUIDE.md](POC_GUIDE.md) | Complete POC guide with all scenarios |
| [MANUAL_SETUP.md](MANUAL_SETUP.md) | Step-by-step manual setup instructions |
| [API_REFERENCE.md](API_REFERENCE.md) | Full API documentation & architecture |
| [Loan-POC.postman_collection.json](Loan-POC.postman_collection.json) | Postman collection (import in Postman) |
| [poc-start.sh](poc-start.sh) | Automated setup script |

---

## Key Endpoints

### Demo Service (Port 9992) - Publisher
```
POST   /kurrentdb/auth-events        Publish event
GET    /loan/statuses                Get all loan statuses
GET    /loan/statuses/{userId}       Get user loan status
GET    /health                       Health check
```

### Loan-Project (Port 9993) - Subscriber
```
GET    /loan/credit-decisions        Get all credit decisions
GET    /loan/credit-decisions/{userId} Get user credit decision
GET    /health                       Health check
```

---

## Test Examples

### Example 1: High Credit Score (Approved)
```bash
curl -X POST http://localhost:9992/kurrentdb/auth-events \
  -H "Content-Type: application/json" \
  -d '{
    "userId":"alice1","username":"alice","creditScore":750,"eventType":"LOGIN"
  }'

# Then check:
curl http://localhost:9993/loan/credit-decisions/alice1
# Result: riskBand=Prime, approved=true, maxApprovedAmount=30000
```

### Example 2: Low Credit Score (Declined)
```bash
curl -X POST http://localhost:9992/kurrentdb/auth-events \
  -H "Content-Type: application/json" \
  -d '{
    "userId":"charlie1","username":"charlie","creditScore":500,"eventType":"LOGIN"
  }'

# Then check:
curl http://localhost:9993/loan/credit-decisions/charlie1
# Result: riskBand=High-Risk, approved=false, maxApprovedAmount=0
```

### Example 3: Logout (Always Denied)
```bash
curl -X POST http://localhost:9992/kurrentdb/auth-events \
  -H "Content-Type: application/json" \
  -d '{
    "userId":"alice1","eventType":"LOGOUT"
  }'

# Then check:
curl http://localhost:9993/loan/credit-decisions/alice1
# Result: approved=false (even though credit was 750 before)
```

---

## Credit Risk Bands

```
Score      Band          Approved?
──────────────────────────────────
760+       Prime         ✓ Yes
700-759    Near-Prime    ✓ Yes
640-699    Fair          ✓ Yes
580-639    Subprime      ✗ No
<580       High-Risk     ✗ No

Special: LOGOUT event → Always ✗ No (regardless of score)
```

---

## Troubleshooting

### Service won't start
```bash
# Check ports are free
lsof -i :9992
lsof -i :9993
lsof -i :2113

# Check KurrentDB is running
curl http://localhost:2113/health
```

### Events not processing
```bash
# Check loan-project logs for:
# "Attached to persistent subscription My_Stream_Diy / My_Group_Diy"

# Verify event published:
curl http://localhost:9992/loan/statuses
```

### Clean slate
```bash
# Stop services (Ctrl+C in terminals)
# Stop infrastructure
cd Infra && docker compose down -v
# Restart everything
./poc-start.sh
```

---

## Architecture Overview

```
┌─────────────────────────────────────┐
│  Postman (Test Client)              │
└────────────┬────────────────────────┘
             │ POST /kurrentdb/auth-events
             ▼
┌─────────────────────────────────────┐
│  Demo Service (9992) - Publisher    │
│  - Receives events from Postman     │
│  - Publishes to KurrentDB stream    │
│  - Tracks loan status (in-memory)   │
└────────────┬────────────────────────┘
             │ AppendToStream
             ▼
┌─────────────────────────────────────┐
│  KurrentDB Stream (My_Stream_Diy)   │
│  - Persistent event store           │
│  - Event ordering guaranteed        │
└────────────┬────────────────────────┘
             │ PersistentSubscription
             ▼
┌─────────────────────────────────────┐
│  Loan-Project (9993) - Subscriber   │
│  - Subscribes to stream             │
│  - Evaluates credit decisions       │
│  - Stores results (in-memory)       │
└────────────┬────────────────────────┘
             │ Query Results
             ▼
        Postman Reads Results
```

---

## Next Steps

1. **Run the POC**: Follow Quick Start above
2. **Test scenarios**: Use Postman collection or curl examples
3. **Check logs**: Observe event flow in service terminals
4. **Review details**: Read full docs (POC_GUIDE.md, API_REFERENCE.md)
5. **Extend**: Add persistence, webhooks, more complex rules

---

## Documentation Map

- **Getting Started**: This file (README_QUICKSTART.md)
- **Manual Setup**: [MANUAL_SETUP.md](MANUAL_SETUP.md)
- **Full POC Guide**: [POC_GUIDE.md](POC_GUIDE.md)
- **API Reference**: [API_REFERENCE.md](API_REFERENCE.md)
- **Postman Collection**: [Loan-POC.postman_collection.json](Loan-POC.postman_collection.json)

---

## Support

Check service logs:
```bash
# Demo service logs show event publishing
# Loan-project service logs show event processing & credit decisions
```

View KurrentDB Admin UI:
```
http://localhost:2113
```

Verify connectivity:
```bash
# KurrentDB
curl http://localhost:2113/health

# Demo
curl http://localhost:9992/health

# Loan-Project
curl http://localhost:9993/health
```

