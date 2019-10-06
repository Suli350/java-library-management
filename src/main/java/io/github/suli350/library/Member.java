package io.github.suli350.library;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Member {

    private final String id;
    private final String name;
    private final String email;
    private final LocalDate joined;
    private BigDecimal finesOwed;

    public Member(String id, String name, String email, LocalDate joined, BigDecimal finesOwed) {
        if (name == null || name.isBlank()) {
            throw new LibraryException("Member name is required");
        }
        if (email != null && !email.isBlank() && !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new LibraryException("Invalid email address: " + email);
        }
        this.id = id;
        this.name = name.trim();
        this.email = email == null ? "" : email.trim();
        this.joined = joined;
        this.finesOwed = finesOwed;
    }

    void addFine(BigDecimal amount) {
        finesOwed = finesOwed.add(amount);
    }

    BigDecimal pay(BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new LibraryException("Payment must be positive");
        }
        BigDecimal applied = amount.min(finesOwed);
        finesOwed = finesOwed.subtract(applied);
        return amount.subtract(applied); // change to give back
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public LocalDate getJoined() { return joined; }
    public BigDecimal getFinesOwed() { return finesOwed; }

    @Override
    public String toString() {
        return String.format("%-6s %-24s %-28s joined %s  fines $%s", id, name, email, joined, finesOwed);
    }
}
