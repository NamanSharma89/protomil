package com.protomil.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistration {
    private String email;
    private String firstName;
    private String lastName;
    private String password;
    private String phoneNumber;
    private String employeeId;
    private String department;
}