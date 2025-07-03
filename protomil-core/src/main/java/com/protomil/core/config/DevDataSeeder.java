package com.protomil.core.config;

import com.protomil.core.shared.domain.enums.RoleStatus;
import com.protomil.core.shared.domain.enums.UserStatus;
import com.protomil.core.shared.domain.enums.UserRoleStatus;
import com.protomil.core.user.domain.Role;
import com.protomil.core.user.domain.User;
import com.protomil.core.user.domain.UserRole;
import com.protomil.core.user.repository.RoleRepository;
import com.protomil.core.user.repository.UserRepository;
import com.protomil.core.user.repository.UserRoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@Profile("dev")
@Slf4j
public class DevDataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public DevDataSeeder(RoleRepository roleRepository,
                         UserRepository userRepository,
                         UserRoleRepository userRoleRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting development data seeding...");

        // Create default roles if they don't exist
        createDefaultRoles();

        // Create default users if they don't exist
        createDefaultUsers();

        log.info("Development data seeding completed!");
    }

    private void createDefaultRoles() {
        List<String> defaultRoles = List.of(
                "SUPER_ADMIN", "ADMIN", "SUPERVISOR", "TECHNICIAN", "VIEWER"
        );

        for (String roleName : defaultRoles) {
            if (!roleRepository.existsByName(roleName)) {
                Role role = Role.builder()
                        .name(roleName)
                        .description("Default " + roleName + " role for development")
                        .status(RoleStatus.ACTIVE)
                        .build();
                roleRepository.save(role);
                log.info("Created default role: {}", roleName);
            }
        }
    }

    private void createDefaultUsers() {
        // Create Super Admin user
        createDevUser("admin@protomil.com", "Admin", "User", "SUPER_ADMIN", "EMP001");

        // Create regular users for testing
        createDevUser("supervisor@protomil.com", "John", "Supervisor", "SUPERVISOR", "EMP002");
        createDevUser("tech1@protomil.com", "Jane", "Technician", "TECHNICIAN", "EMP003");
        createDevUser("viewer@protomil.com", "Bob", "Viewer", "VIEWER", "EMP004");
    }

    private void createDevUser(String email, String firstName, String lastName,
                               String roleName, String employeeId) {
        if (!userRepository.existsByEmail(email)) {
            // Create user
            User user = User.builder()
                    .cognitoUserSub("dev-mock-" + UUID.randomUUID().toString())
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .employeeId(employeeId)
                    .department("Development")
                    .phoneNumber("+919876543210")
                    .status(UserStatus.ACTIVE)
                    .build();

            User savedUser = userRepository.save(user);

            // Assign role
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

            UserRole userRole = UserRole.builder()
                    .user(savedUser)
                    .role(role)
                    .assignedBy(savedUser.getId()) // Self-assigned for dev
                    .assignedAt(LocalDateTime.now())
                    .status(UserRoleStatus.ACTIVE)
                    .build();

            userRoleRepository.save(userRole);

            log.info("Created dev user: {} with role: {}", email, roleName);
        }
    }
}