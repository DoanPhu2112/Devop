#!/bin/bash

# POC Quick Start Script
# Starts all services needed for the loan publisher/subscriber POC

set -e

PROJECT_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

echo "======================================================================"
echo "Loan POC - Quick Start"
echo "======================================================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Docker is running
echo -e "${BLUE}[1/4]${NC} Checking Docker status..."
if ! docker info > /dev/null 2>&1; then
    echo -e "${YELLOW}Docker is not running. Please start Docker.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Docker is running${NC}"
echo ""

# Start Infrastructure
echo -e "${BLUE}[2/4]${NC} Starting infrastructure (KurrentDB, MongoDB, Keycloak, MinIO)..."
cd "$PROJECT_ROOT/Infra"
docker compose up -d

echo -e "${GREEN}✓ Infrastructure starting...${NC}"
echo "  - Waiting for KurrentDB to be healthy..."

# Wait for KurrentDB
for i in {1..30}; do
    if curl -s http://localhost:2113/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ KurrentDB is ready${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${YELLOW}KurrentDB is taking longer to start. It should be ready shortly.${NC}"
        break
    fi
    sleep 2
done
echo ""

# Build and start Demo Service
echo -e "${BLUE}[3/4]${NC} Building and starting Demo Service (Publisher on port 9992)..."
cd "$PROJECT_ROOT/App/spring-project/demo"
if [ ! -f "target/demo-0.0.1-SNAPSHOT.jar" ]; then
    echo "  Building demo service..."
    ./mvnw clean package -q
fi
echo -e "${GREEN}✓ Demo service ready${NC}"
echo ""

# Build Loan Project
echo -e "${BLUE}[4/4]${NC} Building Loan-Project Service (Subscriber on port 9993)..."
cd "$PROJECT_ROOT/App/spring-project/loan-project"
if [ ! -f "target/loan-project-0.0.1-SNAPSHOT.jar" ]; then
    echo "  Building loan-project service..."
    ./mvnw clean package -q
fi
echo -e "${GREEN}✓ Loan-project ready${NC}"
echo ""

echo "======================================================================"
echo -e "${GREEN}POC Setup Complete!${NC}"
echo "======================================================================"
echo ""
echo "Next steps:"
echo ""
echo "1. Start Demo Service (Publisher) in a new terminal:"
echo "   cd $PROJECT_ROOT/App/spring-project/demo"
echo "   ./mvnw spring-boot:run"
echo ""
echo "2. Start Loan-Project Service (Subscriber) in another terminal:"
echo "   cd $PROJECT_ROOT/App/spring-project/loan-project"
echo "   ./mvnw spring-boot:run"
echo ""
echo "3. Use Postman to:"
echo "   - Import: Loan-POC.postman_collection.json"
echo "   - POST events to: http://localhost:9992/kurrentdb/auth-events"
echo "   - Query results: http://localhost:9993/loan/credit-decisions"
echo ""
echo "4. Read the full guide: POC_GUIDE.md"
echo ""
echo "Services Status:"
echo "  - KurrentDB:     http://localhost:2113"
echo "  - MongoDB:       localhost:27017"
echo "  - MinIO:         http://localhost:9000"
echo "  - Keycloak:      http://localhost:8080"
echo "  - Demo (pub):    http://localhost:9992"
echo "  - Loan-Project:  http://localhost:9993"
echo ""
