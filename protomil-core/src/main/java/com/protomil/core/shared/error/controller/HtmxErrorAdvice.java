package com.protomil.core.shared.error.controller;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
@Order(1) // Higher priority than GlobalExceptionHandler
@Slf4j
public class HtmxErrorAdvice {

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.OK)
    @HxRequest
    public String handleResourceNotFound(NoResourceFoundException ex,
                                         HttpServletRequest request,
                                         Model model) {
        // Don't log favicon errors as they're expected
        if (!request.getRequestURI().contains("favicon.ico")) {
            log.warn("Resource not found in HTMX request: {}", request.getRequestURI());
        }

        model.addAttribute("errorMessage", "Resource not found");
        return "fragments/simple-error :: not-found";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    @HxRequest
    public String handleGenericError(Exception ex,
                                     HttpServletRequest request,
                                     Model model) {
        // Don't log favicon errors
        if (!request.getRequestURI().contains("favicon.ico")) {
            log.error("Unexpected error in HTMX request - Path: {}", request.getRequestURI(), ex);
        }

        model.addAttribute("errorMessage", "An unexpected error occurred. Please try again.");
        return "fragments/simple-error :: generic";
    }
}