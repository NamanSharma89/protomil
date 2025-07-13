#!/bin/bash

# Activate User and Make Admin Script (Docker PostgreSQL)
echo "=== Protomil User Activation and Admin Assignment Script (Docker) ==="

# Configuration
export AWS_PROFILE=protomil-dev
export AWS_REGION=ap-south-1

# Docker PostgreSQL configuration
DOCKER_CONTAINER_NAME="postgres"  # Adjust if your container has a different name
DB_NAME="protomil_db"
DB_USER="protomil_user"
DB_PASSWORD="protomil_secure_pass_2024"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to execute SQL via Docker
execute_sql() {
    local sql_command="$1"
    local description="$2"

    echo -e "${BLUE}$description...${NC}"

    # Execute SQL command via Docker
    docker exec -i "$DOCKER_CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" -c "$sql_command"

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ $description completed successfully${NC}"
        return 0
    else
        echo -e "${RED}❌ $description failed${NC}"
        return 1
    fi
}

# Function to execute SQL and get result
execute_sql_result() {
    local sql_command="$1"
    docker exec -i "$DOCKER_CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" -t -c "$sql_command" | tr -d ' ' | head -1
}

# Function to check Docker container
check_docker_container() {
    echo -e "${BLUE}Checking Docker container...${NC}"

    # Check if Docker is running
    if ! docker info >/dev/null 2>&1; then
        echo -e "${RED}❌ Docker is not running. Please start Docker first.${NC}"
        exit 1
    fi

    # Check if PostgreSQL container exists and is running
    if ! docker ps | grep -q "$DOCKER_CONTAINER_NAME"; then
        echo -e "${YELLOW}⚠️  Container '$DOCKER_CONTAINER_NAME' is not running.${NC}"
        echo -e "${BLUE}Available containers:${NC}"
        docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
        echo ""
        read -p "Enter the correct PostgreSQL container name: " new_container_name
        if [ -n "$new_container_name" ]; then
            DOCKER_CONTAINER_NAME="$new_container_name"
        else
            echo -e "${RED}❌ Container name required${NC}"
            exit 1
        fi
    fi

    echo -e "${GREEN}✅ Docker container '$DOCKER_CONTAINER_NAME' is running${NC}"
}

# Function to test database connection
test_db_connection() {
    echo -e "${BLUE}Testing database connection...${NC}"

    docker exec -i "$DOCKER_CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1;" >/dev/null 2>&1

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Database connection successful${NC}"
        return 0
    else
        echo -e "${RED}❌ Cannot connect to database${NC}"
        echo -e "${YELLOW}Trying to create database if it doesn't exist...${NC}"

        # Try to create database
        docker exec -i "$DOCKER_CONTAINER_NAME" psql -U "$DB_USER" -d "postgres" -c "CREATE DATABASE $DB_NAME;" 2>/dev/null

        # Test again
        docker exec -i "$DOCKER_CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1;" >/dev/null 2>&1

        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✅ Database connection successful after creation${NC}"
            return 0
        else
            echo -e "${RED}❌ Still cannot connect to database${NC}"
            echo -e "${YELLOW}Please check your database configuration:${NC}"
            echo "Container: $DOCKER_CONTAINER_NAME"
            echo "Database: $DB_NAME"
            echo "User: $DB_USER"
            exit 1
        fi
    fi
}

# Function to check if user exists
check_user_exists() {
    local email="$1"

    local user_count=$(execute_sql_result "SELECT COUNT(*) FROM users WHERE email = '$email';")

    if [ "$user_count" = "1" ]; then
        return 0
    else
        return 1
    fi
}

# Function to get or create admin role
ensure_admin_role() {
    echo -e "${BLUE}Ensuring SUPER_ADMIN role exists...${NC}"

    # Check if SUPER_ADMIN role exists
    local role_count=$(execute_sql_result "SELECT COUNT(*) FROM roles WHERE name = 'SUPER_ADMIN';")

    if [ "$role_count" = "0" ]; then
        echo -e "${YELLOW}Creating SUPER_ADMIN role...${NC}"
        execute_sql "
            INSERT INTO roles (id, name, description, status, created_at, updated_at)
            VALUES (
                gen_random_uuid(),
                'SUPER_ADMIN',
                'Super Administrator with full system access',
                'ACTIVE',
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            );
        " "Create SUPER_ADMIN role"
    else
        echo -e "${GREEN}✅ SUPER_ADMIN role already exists${NC}"
    fi

    # Get the role ID
    ADMIN_ROLE_ID=$(execute_sql_result "SELECT id FROM roles WHERE name = 'SUPER_ADMIN';")
    echo -e "${BLUE}SUPER_ADMIN Role ID: $ADMIN_ROLE_ID${NC}"
}

# Function to activate user and assign admin role
activate_and_assign_admin() {
    local email="$1"

    echo -e "${BLUE}=== Activating User and Assigning Admin Role ===${NC}"
    echo -e "${BLUE}Email: $email${NC}"

    # Check if user exists
    if ! check_user_exists "$email"; then
        echo -e "${RED}❌ User not found: $email${NC}"
        echo -e "${YELLOW}Please register the user first, then run this script${NC}"
        return 1
    fi

    # Get user ID
    USER_ID=$(execute_sql_result "SELECT id FROM users WHERE email = '$email';")
    echo -e "${BLUE}User ID: $USER_ID${NC}"

    # Show current user status
    echo -e "${BLUE}Current user information:${NC}"
    execute_sql "
        SELECT
            email,
            first_name,
            last_name,
            status,
            created_at,
            updated_at
        FROM users
        WHERE email = '$email';
    " "Display current user info"

    # Activate user
    execute_sql "
        UPDATE users
        SET
            status = 'ACTIVE',
            updated_at = CURRENT_TIMESTAMP
        WHERE email = '$email';
    " "Activate user"

    # Ensure admin role exists
    ensure_admin_role

    # Check if user already has admin role
    local existing_role_count=$(execute_sql_result "
        SELECT COUNT(*)
        FROM user_roles ur
        JOIN roles r ON ur.role_id = r.id
        WHERE ur.user_id = '$USER_ID'
        AND r.name = 'SUPER_ADMIN'
        AND ur.status = 'ACTIVE';
    ")

    if [ "$existing_role_count" = "0" ]; then
        # Assign admin role
        execute_sql "
            INSERT INTO user_roles (id, user_id, role_id, assigned_by, assigned_at, status, created_at, updated_at)
            VALUES (
                gen_random_uuid(),
                '$USER_ID',
                '$ADMIN_ROLE_ID',
                '$USER_ID',
                CURRENT_TIMESTAMP,
                'ACTIVE',
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            );
        " "Assign SUPER_ADMIN role"
    else
        echo -e "${GREEN}✅ User already has SUPER_ADMIN role${NC}"
    fi

    # Display final user information
    echo -e "${BLUE}Final user information:${NC}"
    execute_sql "
        SELECT
            u.email,
            u.first_name,
            u.last_name,
            u.status as user_status,
            COALESCE(
                string_agg(r.name, ', ' ORDER BY r.name),
                'No roles assigned'
            ) as roles
        FROM users u
        LEFT JOIN user_roles ur ON u.id = ur.user_id AND ur.status = 'ACTIVE'
        LEFT JOIN roles r ON ur.role_id = r.id AND r.status = 'ACTIVE'
        WHERE u.email = '$email'
        GROUP BY u.id, u.email, u.first_name, u.last_name, u.status;
    " "Display final user information"

    # Sync with Cognito if enabled
    if [ -n "$AWS_COGNITO_DEV_USER_POOL_ID" ] && [ -n "$AWS_COGNITO_DEV_CLIENT_ID" ]; then
        echo -e "${BLUE}Syncing with Cognito...${NC}"
        sync_with_cognito "$email"
    else
        echo -e "${YELLOW}⚠️  Cognito environment variables not set, skipping Cognito sync${NC}"
    fi

    echo -e "${GREEN}🎉 User activation and admin assignment completed successfully!${NC}"
}

# Function to sync with Cognito
sync_with_cognito() {
    local email="$1"

    echo -e "${BLUE}Syncing user status with Cognito...${NC}"

    # Update user attributes in Cognito
    aws cognito-idp admin-update-user-attributes \
        --user-pool-id="$AWS_COGNITO_DEV_USER_POOL_ID" \
        --username="$email" \
        --user-attributes Name=custom:approval_status,Value=ACTIVE Name=custom:user_roles,Value=SUPER_ADMIN \
        --profile="$AWS_PROFILE" \
        --region="$AWS_REGION" 2>/dev/null

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Cognito attributes updated${NC}"
    else
        echo -e "${YELLOW}⚠️  Could not update Cognito attributes (user might not exist in Cognito)${NC}"
    fi

    # Enable user in Cognito
    aws cognito-idp admin-enable-user \
        --user-pool-id="$AWS_COGNITO_DEV_USER_POOL_ID" \
        --username="$email" \
        --profile="$AWS_PROFILE" \
        --region="$AWS_REGION" 2>/dev/null

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ User enabled in Cognito${NC}"
    else
        echo -e "${YELLOW}⚠️  Could not enable user in Cognito${NC}"
    fi
}

# Function to list all users
list_users() {
    echo -e "${BLUE}=== All Users in System ===${NC}"
    execute_sql "
        SELECT
            u.email,
            u.first_name,
            u.last_name,
            u.status as user_status,
            COALESCE(
                string_agg(r.name, ', ' ORDER BY r.name),
                'No roles assigned'
            ) as roles,
            u.created_at
        FROM users u
        LEFT JOIN user_roles ur ON u.id = ur.user_id AND ur.status = 'ACTIVE'
        LEFT JOIN roles r ON ur.role_id = r.id AND r.status = 'ACTIVE'
        GROUP BY u.id, u.email, u.first_name, u.last_name, u.status, u.created_at
        ORDER BY u.created_at DESC;
    " "List all users"
}

# Function to show Docker containers
show_containers() {
    echo -e "${BLUE}=== Available Docker Containers ===${NC}"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}\t{{.Image}}"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [OPTIONS] [EMAIL]"
    echo ""
    echo "Options:"
    echo "  -h, --help          Show this help message"
    echo "  -l, --list          List all users in the system"
    echo "  -c, --containers    Show Docker containers"
    echo "  -a, --activate EMAIL    Activate user and assign admin role"
    echo ""
    echo "Examples:"
    echo "  $0 --activate dev.namansharma89@gmail.com"
    echo "  $0 -a user@example.com"
    echo "  $0 --list"
    echo "  $0 --containers"
    echo ""
    echo "Environment Variables Required for Cognito sync:"
    echo "  AWS_COGNITO_DEV_USER_POOL_ID"
    echo "  AWS_COGNITO_DEV_CLIENT_ID"
    echo "  AWS_PROFILE (default: protomil-dev)"
    echo "  AWS_REGION (default: ap-south-1)"
    echo ""
    echo "Docker Configuration:"
    echo "  Container Name: $DOCKER_CONTAINER_NAME"
    echo "  Database: $DB_NAME"
    echo "  User: $DB_USER"
}

# Main script logic
main() {
    case "$1" in
        -h|--help)
            show_usage
            exit 0
            ;;
        -c|--containers)
            show_containers
            exit 0
            ;;
        -l|--list)
            check_docker_container
            test_db_connection
            list_users
            exit 0
            ;;
        -a|--activate)
            if [ -z "$2" ]; then
                echo -e "${RED}❌ Email address required${NC}"
                show_usage
                exit 1
            fi
            check_docker_container
            test_db_connection
            activate_and_assign_admin "$2"
            exit 0
            ;;
        "")
            # Interactive mode
            check_docker_container
            test_db_connection
            echo -e "${BLUE}=== Interactive Mode ===${NC}"
            echo -e "${BLUE}Available options:${NC}"
            echo "1. Activate user and assign admin role"
            echo "2. List all users"
            echo "3. Show Docker containers"
            echo "4. Exit"
            echo ""
            read -p "Choose an option (1-4): " choice

            case $choice in
                1)
                    read -p "Enter email address: " email
                    if [ -n "$email" ]; then
                        activate_and_assign_admin "$email"
                    else
                        echo -e "${RED}❌ Email address required${NC}"
                        exit 1
                    fi
                    ;;
                2)
                    list_users
                    ;;
                3)
                    show_containers
                    ;;
                4)
                    echo "Goodbye!"
                    exit 0
                    ;;
                *)
                    echo -e "${RED}❌ Invalid option${NC}"
                    exit 1
                    ;;
            esac
            ;;
        *)
            # Assume first argument is email
            check_docker_container
            test_db_connection
            activate_and_assign_admin "$1"
            ;;
    esac
}

# Initialize
echo -e "${BLUE}Docker PostgreSQL detected${NC}"
main "$@"