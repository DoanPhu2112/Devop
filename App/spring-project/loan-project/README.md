# Loan Project

Loan tracking and credit approval service that subscribes to KurrentDB persistent subscription.

## Architecture

This service is a **subscriber** in the KurrentDB pub-sub flow:

- **Stream**: `My_Stream_Diy`
- **Group**: `My_Group_Diy`
- **Port**: 9993

## Components

### Services

- **LoanTrackingService**: Tracks user login/logout/signup states and maintains loan status
- **CreditApprovalService**: Evaluates credit decisions based on credit scores and event types
- **UserAuthSubscriptionHandler**: Subscribes to persistent subscription and fans out events

### DTOs

- **UserAuthEvent**: Event from auth stream (userId, username, creditScore, eventType, etc.)
- **LoanStatus**: Current loan tracking state per user
- **CreditDecision**: Credit approval decision with risk band and max approved amount

### Endpoints

#### Health

```
GET /health
```

#### Loan Statuses

```
GET /loan/statuses                  - List all loan statuses
GET /loan/statuses/{userId}         - Get loan status for specific user
```

#### Credit Decisions

```
GET /loan/credit-decisions          - List all credit decisions
GET /loan/credit-decisions/{userId} - Get credit decision for specific user
```

## Configuration

Edit `src/main/resources/application.properties`:

```properties
server.port=9993
kurrentdb.connection-string=esdb://127.0.0.1:2113?tls=false
kurrentdb.stream-name=My_Stream_Diy
kurrentdb.group-name=My_Group_Diy
```

## Running

```bash
# Build
./mvnw clean package

# Run
./mvnw spring-boot:run
```

## Credit Scoring Logic

- Credit score range: 300-900
- Risk bands:
  - Prime: 760+
  - Near-Prime: 700-759
  - Fair: 640-699
  - Subprime: 580-639
  - High-Risk: <580
- Approval threshold: 640+ (not on LOGOUT event)
- Max approved amount: max(creditScore _ 40, annualIncome _ 0.25)

## Event Processing

1. Subscribe to `My_Stream_Diy/My_Group_Diy` on startup
2. Parse each `UserAuthEvent` from stream
3. Update loan status tracking
4. Evaluate credit decision
5. ACK successful processing or NACK to parked stream on failure
