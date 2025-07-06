#!/bin/bash
# tests/run-all-tests.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Configuration
export API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
export TEST_ENVIRONMENT="${TEST_ENVIRONMENT:-local}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}    Protomil API Test Suite Runner    ${NC}"
echo -e "${BLUE}======================================${NC}"
echo "Environment: $TEST_ENVIRONMENT"
echo "Base URL: $API_BASE_URL"
echo ""

# Create overall results directory
mkdir -p test-results/suite-$(date +%Y%m%d_%H%M%S)

# Run test suites
test_suites=(
    "API Registration Tests:$SCRIPT_DIR/api/test-user-registration.sh"
    "Wireframe Tests:$SCRIPT_DIR/wireframes/test-wireframes.sh"
)

overall_result=0

for suite in "${test_suites[@]}"; do
    suite_name="${suite%%:*}"
    suite_script="${suite##*:}"

    echo -e "${BLUE}Running: $suite_name${NC}"
    echo "----------------------------------------"

    if [ -f "$suite_script" ]; then
        bash "$suite_script"
        suite_result=$?

        if [ $suite_result -eq 0 ]; then
            echo -e "${GREEN}✓ $suite_name completed successfully${NC}"
        else
            echo -e "${RED}✗ $suite_name failed${NC}"
            overall_result=1
        fi
    else
        echo -e "${RED}✗ Test suite not found: $suite_script${NC}"
        overall_result=1
    fi

    echo ""
done

# Final summary
echo -e "${BLUE}======================================${NC}"
if [ $overall_result -eq 0 ]; then
    echo -e "${GREEN}🎉 All test suites passed!${NC}"
else
    echo -e "${RED}❌ Some test suites failed!${NC}"
fi
echo -e "${BLUE}======================================${NC}"

exit $overall_result