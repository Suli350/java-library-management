package io.github.suli350.library;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Loan {

    private final long id;
    private final String isbn;
    private final String memberId;
    private final LocalDate borrowed;
    private LocalDate due;
    private LocalDate returned;
    private int renewals;

    public Loan(long id, String isbn, String memberId, LocalDate borrowed, LocalDate due,
                LocalDate returned, int renewals) {
        this.id = id;
        this.isbn = isbn;
        this.memberId = memberId;
        this.borrowed = borrowed;
        this.due = due;
        this.returned = returned;
        this.renewals = renewals;
    }

    public boolean isActive() {
        return returned == null;
    }

    public boolean isOverdue(LocalDate today) {
        return isActive() && today.isAfter(due);
    }

    public long daysLate(LocalDate on) {
        return Math.max(0, ChronoUnit.DAYS.between(due, on));
    }

    public BigDecimal fineOn(LocalDate on) {
        BigDecimal fine = Library.FINE_PER_DAY.multiply(BigDecimal.valueOf(daysLate(on)));
        return fine.min(Library.MAX_FINE_PER_LOAN);
    }

    void markReturned(LocalDate on) {
        returned = on;
    }

    void renew(int days) {
        due = due.plusDays(days);
        renewals++;
    }

    public long getId() { return id; }
    public String getIsbn() { return isbn; }
    public String getMemberId() { return memberId; }
    public LocalDate getBorrowed() { return borrowed; }
    public LocalDate getDue() { return due; }
    public LocalDate getReturned() { return returned; }
    public int getRenewals() { return renewals; }
}
