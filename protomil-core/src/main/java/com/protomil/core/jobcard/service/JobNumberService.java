package com.protomil.core.jobcard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobNumberService {

    private final AtomicLong counter = new AtomicLong(1);
    private static final String JOB_NUMBER_FORMAT = "JC-%s-%04d";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy");

    public String generateJobNumber(String category) {
        String year = LocalDate.now().format(DATE_FORMAT);
        long sequence = counter.getAndIncrement();

        String jobNumber = String.format(JOB_NUMBER_FORMAT, year, sequence);

        log.debug("Generated job number: {} for category: {}", jobNumber, category);

        return jobNumber;
    }

    public String generateJobNumberWithCategory(String category) {
        String year = LocalDate.now().format(DATE_FORMAT);
        long sequence = counter.getAndIncrement();
        String categoryPrefix = category != null ? category.substring(0, Math.min(3, category.length())).toUpperCase() : "GEN";

        String jobNumber = String.format("%s-JC-%s-%04d", categoryPrefix, year, sequence);

        log.debug("Generated job number with category: {} for category: {}", jobNumber, category);

        return jobNumber;
    }
}