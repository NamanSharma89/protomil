#!/usr/bin/env python3
import os
import sys
from pathlib import Path

def create_directory_structure():
    """Generate the directory structure for Protomil UI module"""
    
    base_dir = "protomil-ui"
    
    # Define the directory structure
    dirs = [
        # Root level
        os.path.join(base_dir),
        os.path.join(base_dir, "public"),
        os.path.join(base_dir, "src"),
        
        # Source directories
        os.path.join(base_dir, "src", "assets"),
        os.path.join(base_dir, "src", "assets", "images"),
        os.path.join(base_dir, "src", "assets", "styles"),
        os.path.join(base_dir, "src", "assets", "styles", "components"),
        os.path.join(base_dir, "src", "assets", "styles", "base"),
        
        # Components
        os.path.join(base_dir, "src", "components"),
        os.path.join(base_dir, "src", "components", "common"),
        os.path.join(base_dir, "src", "components", "layout"),
        os.path.join(base_dir, "src", "components", "features"),
        os.path.join(base_dir, "src", "components", "features", "tickets"),
        os.path.join(base_dir, "src", "components", "features", "dashboard"),
        os.path.join(base_dir, "src", "components", "features", "auth"),
        os.path.join(base_dir, "src", "components", "features", "settings"),
        
        # Hooks
        os.path.join(base_dir, "src", "hooks"),
        
        # Context/State Management
        os.path.join(base_dir, "src", "context"),
        os.path.join(base_dir, "src", "store"),
        os.path.join(base_dir, "src", "store", "slices"),
        
        # Pages
        os.path.join(base_dir, "src", "pages"),
        os.path.join(base_dir, "src", "pages", "dashboard"),
        os.path.join(base_dir, "src", "pages", "tickets"),
        os.path.join(base_dir, "src", "pages", "auth"),
        os.path.join(base_dir, "src", "pages", "settings"),
        
        # Services
        os.path.join(base_dir, "src", "services"),
        os.path.join(base_dir, "src", "services", "api"),
        
        # Utils
        os.path.join(base_dir, "src", "utils"),
        os.path.join(base_dir, "src", "utils", "helpers"),
        os.path.join(base_dir, "src", "utils", "constants"),
        os.path.join(base_dir, "src", "utils", "validators"),
        
        # Types
        os.path.join(base_dir, "src", "types"),
        
        # Tests
        os.path.join(base_dir, "src", "__tests__"),
        os.path.join(base_dir, "src", "__tests__", "components"),
        os.path.join(base_dir, "src", "__tests__", "pages"),
        os.path.join(base_dir, "src", "__tests__", "services"),
        os.path.join(base_dir, "src", "__tests__", "utils"),
        
        # Build output directory
        os.path.join(base_dir, "dist"),
    ]
    
    # Files to create
    files = [
        # Root level files
        os.path.join(base_dir, "package.json"),
        os.path.join(base_dir, "tsconfig.json"),
        os.path.join(base_dir, "vite.config.ts"),
        os.path.join(base_dir, ".eslintrc.json"),
        os.path.join(base_dir, ".prettierrc"),
        os.path.join(base_dir, ".env.example"),
        os.path.join(base_dir, ".gitignore"),
        os.path.join(base_dir, "README.md"),
        os.path.join(base_dir, "index.html"),
        
        # Public directory
        os.path.join(base_dir, "public", "favicon.ico"),
        os.path.join(base_dir, "public", "robots.txt"),
        os.path.join(base_dir, "public", "manifest.json"),
        
        # Source files
        os.path.join(base_dir, "src", "index.tsx"),
        os.path.join(base_dir, "src", "App.tsx"),
        os.path.join(base_dir, "src", "main.tsx"),
        
        # Assets
        os.path.join(base_dir, "src", "assets", "styles", "base", "reset.css"),
        os.path.join(base_dir, "src", "assets", "styles", "base", "variables.css"),
        os.path.join(base_dir, "src", "assets", "styles", "base", "typography.css"),
        os.path.join(base_dir, "src", "assets", "styles", "base", "utilities.css"),
        os.path.join(base_dir, "src", "assets", "styles", "index.css"),
        
        # Common Components
        os.path.join(base_dir, "src", "components", "common", "Button", "Button.tsx"),
        os.path.join(base_dir, "src", "components", "common", "Button", "Button.module.css"),
        os.path.join(base_dir, "src", "components", "common", "Input", "Input.tsx"),
        os.path.join(base_dir, "src", "components", "common", "Input", "Input.module.css"),
        os.path.join(base_dir, "src", "components", "common", "Table", "Table.tsx"),
        os.path.join(base_dir, "src", "components", "common", "Table", "Table.module.css"),
        os.path.join(base_dir, "src", "components", "common", "Modal", "Modal.tsx"),
        os.path.join(base_dir, "src", "components", "common", "Modal", "Modal.module.css"),
        os.path.join(base_dir, "src", "components", "common", "Dropdown", "Dropdown.tsx"),
        os.path.join(base_dir, "src", "components", "common", "Dropdown", "Dropdown.module.css"),
        os.path.join(base_dir, "src", "components", "common", "Loading", "Loading.tsx"),
        os.path.join(base_dir, "src", "components", "common", "Loading", "Loading.module.css"),
        os.path.join(base_dir, "src", "components", "common", "Card", "Card.tsx"),
        os.path.join(base_dir, "src", "components", "common", "Card", "Card.module.css"),
        os.path.join(base_dir, "src", "components", "common", "Tooltip", "Tooltip.tsx"),
        os.path.join(base_dir, "src", "components", "common", "Tooltip", "Tooltip.module.css"),
        os.path.join(base_dir, "src", "components", "common", "index.ts"),
        
        # Layout Components
        os.path.join(base_dir, "src", "components", "layout", "Header", "Header.tsx"),
        os.path.join(base_dir, "src", "components", "layout", "Header", "Header.module.css"),
        os.path.join(base_dir, "src", "components", "layout", "Sidebar", "Sidebar.tsx"),
        os.path.join(base_dir, "src", "components", "layout", "Sidebar", "Sidebar.module.css"),
        os.path.join(base_dir, "src", "components", "layout", "Footer", "Footer.tsx"),
        os.path.join(base_dir, "src", "components", "layout", "Footer", "Footer.module.css"),
        os.path.join(base_dir, "src", "components", "layout", "Layout", "Layout.tsx"),
        os.path.join(base_dir, "src", "components", "layout", "Layout", "Layout.module.css"),
        os.path.join(base_dir, "src", "components", "layout", "index.ts"),
        
        # Feature Components - Tickets
        os.path.join(base_dir, "src", "components", "features", "tickets", "TicketList", "TicketList.tsx"),
        os.path.join(base_dir, "src", "components", "features", "tickets", "TicketList", "TicketList.module.css"),
        os.path.join(base_dir, "src", "components", "features", "tickets", "TicketCard", "TicketCard.tsx"),
        os.path.join(base_dir, "src", "components", "features", "tickets", "TicketCard", "TicketCard.module.css"),
        os.path.join(base_dir, "src", "components", "features", "tickets", "TicketFilters", "TicketFilters.tsx"),
        os.path.join(base_dir, "src", "components", "features", "tickets", "TicketFilters", "TicketFilters.module.css"),
        os.path.join(base_dir, "src", "components", "features", "tickets", "TicketForm", "TicketForm.tsx"),
        os.path.join(base_dir, "src", "components", "features", "tickets", "TicketForm", "TicketForm.module.css"),
        os.path.join(base_dir, "src", "components", "features", "tickets", "TicketDetails", "TicketDetails.tsx"),
        os.path.join(base_dir, "src", "components", "features", "tickets", "TicketDetails", "TicketDetails.module.css"),
        os.path.join(base_dir, "src", "components", "features", "tickets", "index.ts"),
        
        # Feature Components - Dashboard
        os.path.join(base_dir, "src", "components", "features", "dashboard", "DashboardStats", "DashboardStats.tsx"),
        os.path.join(base_dir, "src", "components", "features", "dashboard", "DashboardStats", "DashboardStats.module.css"),
        os.path.join(base_dir, "src", "components", "features", "dashboard", "ActivityFeed", "ActivityFeed.tsx"),
        os.path.join(base_dir, "src", "components", "features", "dashboard", "ActivityFeed", "ActivityFeed.module.css"),
        os.path.join(base_dir, "src", "components", "features", "dashboard", "RecentTickets", "RecentTickets.tsx"),
        os.path.join(base_dir, "src", "components", "features", "dashboard", "RecentTickets", "RecentTickets.module.css"),
        os.path.join(base_dir, "src", "components", "features", "dashboard", "index.ts"),
        
        # Feature Components - Auth
        os.path.join(base_dir, "src", "components", "features", "auth", "LoginForm", "LoginForm.tsx"),
        os.path.join(base_dir, "src", "components", "features", "auth", "LoginForm", "LoginForm.module.css"),
        os.path.join(base_dir, "src", "components", "features", "auth", "RegisterForm", "RegisterForm.tsx"),
        os.path.join(base_dir, "src", "components", "features", "auth", "RegisterForm", "RegisterForm.module.css"),
        os.path.join(base_dir, "src", "components", "features", "auth", "ProtectedRoute", "ProtectedRoute.tsx"),
        os.path.join(base_dir, "src", "components", "features", "auth", "index.ts"),
        
        # Feature Components - Settings
        os.path.join(base_dir, "src", "components", "features", "settings", "ProfileSettings", "ProfileSettings.tsx"),
        os.path.join(base_dir, "src", "components", "features", "settings", "ProfileSettings", "ProfileSettings.module.css"),
        os.path.join(base_dir, "src", "components", "features", "settings", "NotificationSettings", "NotificationSettings.tsx"),
        os.path.join(base_dir, "src", "components", "features", "settings", "NotificationSettings", "NotificationSettings.module.css"),
        os.path.join(base_dir, "src", "components", "features", "settings", "SystemSettings", "SystemSettings.tsx"),
        os.path.join(base_dir, "src", "components", "features", "settings", "SystemSettings", "SystemSettings.module.css"),
        os.path.join(base_dir, "src", "components", "features", "settings", "index.ts"),
        
        # Hooks
        os.path.join(base_dir, "src", "hooks", "useAuth.ts"),
        os.path.join(base_dir, "src", "hooks", "useTickets.ts"),
        os.path.join(base_dir, "src", "hooks", "useTheme.ts"),
        os.path.join(base_dir, "src", "hooks", "useLocalStorage.ts"),
        os.path.join(base_dir, "src", "hooks", "useMediaQuery.ts"),
        os.path.join(base_dir, "src", "hooks", "useDebounce.ts"),
        os.path.join(base_dir, "src", "hooks", "index.ts"),
        
        # Context
        os.path.join(base_dir, "src", "context", "AuthContext.tsx"),
        os.path.join(base_dir, "src", "context", "ThemeContext.tsx"),
        os.path.join(base_dir, "src", "context", "index.ts"),
        
        # Store
        os.path.join(base_dir, "src", "store", "index.ts"),
        os.path.join(base_dir, "src", "store", "slices", "authSlice.ts"),
        os.path.join(base_dir, "src", "store", "slices", "ticketSlice.ts"),
        os.path.join(base_dir, "src", "store", "slices", "notificationSlice.ts"),
        
        # Pages
        os.path.join(base_dir, "src", "pages", "dashboard", "DashboardPage.tsx"),
        os.path.join(base_dir, "src", "pages", "tickets", "TicketListPage.tsx"),
        os.path.join(base_dir, "src", "pages", "tickets", "TicketDetailPage.tsx"),
        os.path.join(base_dir, "src", "pages", "tickets", "CreateTicketPage.tsx"),
        os.path.join(base_dir, "src", "pages", "auth", "LoginPage.tsx"),
        os.path.join(base_dir, "src", "pages", "auth", "RegisterPage.tsx"),
        os.path.join(base_dir, "src", "pages", "auth", "ForgotPasswordPage.tsx"),
        os.path.join(base_dir, "src", "pages", "settings", "SettingsPage.tsx"),
        os.path.join(base_dir, "src", "pages", "NotFoundPage.tsx"),
        os.path.join(base_dir, "src", "pages", "index.ts"),
        
        # Services
        os.path.join(base_dir, "src", "services", "api", "authApi.ts"),
        os.path.join(base_dir, "src", "services", "api", "ticketApi.ts"),
        os.path.join(base_dir, "src", "services", "api", "userApi.ts"),
        os.path.join(base_dir, "src", "services", "api", "index.ts"),
        os.path.join(base_dir, "src", "services", "apiClient.ts"),
        os.path.join(base_dir, "src", "services", "websocket.ts"),
        os.path.join(base_dir, "src", "services", "index.ts"),
        
        # Utils
        os.path.join(base_dir, "src", "utils", "helpers", "dateHelper.ts"),
        os.path.join(base_dir, "src", "utils", "helpers", "formatHelper.ts"),
        os.path.join(base_dir, "src", "utils", "helpers", "errorHelper.ts"),
        os.path.join(base_dir, "src", "utils", "helpers", "storageHelper.ts"),
        os.path.join(base_dir, "src", "utils", "helpers", "index.ts"),
        os.path.join(base_dir, "src", "utils", "constants", "apiEndpoints.ts"),
        os.path.join(base_dir, "src", "utils", "constants", "routeConstants.ts"),
        os.path.join(base_dir, "src", "utils", "constants", "appConstants.ts"),
        os.path.join(base_dir, "src", "utils", "constants", "index.ts"),
        os.path.join(base_dir, "src", "utils", "validators", "ticketValidators.ts"),
        os.path.join(base_dir, "src", "utils", "validators", "userValidators.ts"),
        os.path.join(base_dir, "src", "utils", "validators", "commonValidators.ts"),
        os.path.join(base_dir, "src", "utils", "validators", "index.ts"),
        os.path.join(base_dir, "src", "utils", "index.ts"),
        
        # Types
        os.path.join(base_dir, "src", "types", "ticket.ts"),
        os.path.join(base_dir, "src", "types", "user.ts"),
        os.path.join(base_dir, "src", "types", "auth.ts"),
        os.path.join(base_dir, "src", "types", "api.ts"),
        os.path.join(base_dir, "src", "types", "common.ts"),
        os.path.join(base_dir, "src", "types", "index.ts"),
        
        # Tests
        os.path.join(base_dir, "src", "__tests__", "components", "Button.test.tsx"),
        os.path.join(base_dir, "src", "__tests__", "components", "TicketCard.test.tsx"),
        os.path.join(base_dir, "src", "__tests__", "pages", "DashboardPage.test.tsx"),
        os.path.join(base_dir, "src", "__tests__", "services", "apiClient.test.ts"),
        os.path.join(base_dir, "src", "__tests__", "utils", "dateHelper.test.ts"),
        os.path.join(base_dir, "src", "__tests__", "setup.ts"),
        os.path.join(base_dir, "jest.config.js"),
        
        # Routing
        os.path.join(base_dir, "src", "router", "index.tsx"),
        os.path.join(base_dir, "src", "router", "routes.tsx"),
    ]
    
    # Create directories
    for directory in dirs:
        try:
            os.makedirs(directory, exist_ok=True)
            print(f"Created directory: {directory}")
        except Exception as e:
            print(f"Error creating directory {directory}: {e}")
    
    # Create empty files
    for file_path in files:
        try:
            Path(file_path).touch()
            print(f"Created file: {file_path}")
        except Exception as e:
            print(f"Error creating file {file_path}: {e}")
    
    print("\n✅ Directory structure created successfully!")
    print(f"\nNext steps:")
    print(f"1. Navigate to the {base_dir} directory")
    print(f"2. Initialize Git repository: git init")
    print(f"3. Install dependencies: npm install")
    print(f"4. Start development: npm run dev")

if __name__ == "__main__":
    create_directory_structure()