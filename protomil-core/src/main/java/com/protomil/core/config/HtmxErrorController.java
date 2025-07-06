package com.protomil.core.config;

import com.protomil.core.shared.dto.ErrorResponse;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDateTime;

@ControllerAdvice
@Slf4j
public class HtmxErrorController {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK) // Return 200 so HTMX will swap content
    @HxRequest
    public String handleHtmxError(Exception ex, HttpServletRequest request, Model model) {
        log.error("HTMX request error on path: {}", request.getRequestURI(), ex);

        model.addAttribute("errorMessage", "An unexpected error occurred. Please try again.");
        model.addAttribute("errorDetails", ex.getMessage());
        model.addAttribute("timestamp", LocalDateTime.now());

        return "fragments/error :: htmx-error";
    }
}