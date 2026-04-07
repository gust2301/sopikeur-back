package sn.sopikeur.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentNumberService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public String nextInvoiceNumber(int year) {
        return format("INV", year, nextValue("INVOICE", year));
    }

    @Transactional
    public String nextReceiptNumber(int year) {
        return format("RCPT", year, nextValue("RECEIPT", year));
    }

    private int nextValue(String name, int year) {
        jdbcTemplate.update(
            "INSERT IGNORE INTO sequences(name, year, value) VALUES (?, ?, 0)",
            name,
            year
        );

        Integer current = jdbcTemplate.queryForObject(
            "SELECT value FROM sequences WHERE name = ? AND year = ? FOR UPDATE",
            Integer.class,
            name,
            year
        );

        int next = (current != null ? current : 0) + 1;
        jdbcTemplate.update(
            "UPDATE sequences SET value = ? WHERE name = ? AND year = ?",
            next,
            name,
            year
        );
        return next;
    }

    private String format(String prefix, int year, int value) {
        return "%s-%d-%06d".formatted(prefix, year, value);
    }
}
