package io.github.suli350.library;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** All library rules live here. The shell and the store only call these methods. */
public class Library {

    public static final int LOAN_DAYS = 14;
    public static final int MAX_ACTIVE_LOANS = 3;
    public static final int MAX_RENEWALS = 1;
    public static final BigDecimal FINE_PER_DAY = new BigDecimal("0.25");
    public static final BigDecimal MAX_FINE_PER_LOAN = new BigDecimal("10.00");
    public static final BigDecimal FINE_BLOCK_LIMIT = new BigDecimal("5.00");

    private final Map<String, Book> books = new LinkedHashMap<>();
    private final Map<String, Member> members = new LinkedHashMap<>();
    private final Map<Long, Loan> loans = new LinkedHashMap<>();
    private final Supplier<LocalDate> clock;
    private int nextMemberNumber = 1;
    private long nextLoanId = 1;

    public Library(Supplier<LocalDate> clock) {
        this.clock = clock;
    }

    public LocalDate today() {
        return clock.get();
    }

    // ------------------------------------------------------------------ books
    /** Adds a new title, or more copies of an existing one. */
    public Book addBook(String rawIsbn, String title, String author, int year, int copies) {
        if (copies < 1) {
            throw new LibraryException("Copies must be at least 1");
        }
        String isbn = Isbn.normalize(rawIsbn);
        Book existing = books.get(isbn);
        if (existing != null) {
            existing.addCopies(copies);
            return existing;
        }
        Book book = new Book(isbn, title, author, year, copies, copies);
        books.put(isbn, book);
        return book;
    }

    public void removeBook(String rawIsbn) {
        Book book = findBook(rawIsbn);
        if (book.getAvailableCopies() != book.getTotalCopies()) {
            throw new LibraryException("Cannot remove \"" + book.getTitle() + "\": copies are on loan");
        }
        books.remove(book.getIsbn());
    }

    public Book findBook(String rawIsbn) {
        Book book = books.get(Isbn.normalize(rawIsbn));
        if (book == null) {
            throw new LibraryException("No book with ISBN " + rawIsbn);
        }
        return book;
    }

    public List<Book> search(String query) {
        return books.values().stream()
                .filter(b -> b.matches(query.trim()))
                .sorted(Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public Collection<Book> getBooks() {
        return Collections.unmodifiableCollection(books.values());
    }

    // ------------------------------------------------------------------ members
    public Member registerMember(String name, String email) {
        String id = String.format("M%03d", nextMemberNumber);
        Member m = new Member(id, name, email, today(), new BigDecimal("0.00"));
        nextMemberNumber++;
        members.put(id, m);
        return m;
    }

    public Member findMember(String id) {
        Member m = members.get(id == null ? null : id.trim().toUpperCase());
        if (m == null) {
            throw new LibraryException("No member with id " + id);
        }
        return m;
    }

    public Collection<Member> getMembers() {
        return Collections.unmodifiableCollection(members.values());
    }

    // ------------------------------------------------------------------ loans
    public Loan checkout(String memberId, String rawIsbn) {
        Member member = findMember(memberId);
        Book book = findBook(rawIsbn);
        List<Loan> active = activeLoans(member.getId());
        if (active.size() >= MAX_ACTIVE_LOANS) {
            throw new LibraryException(member.getName() + " already has " + MAX_ACTIVE_LOANS + " books on loan");
        }
        if (active.stream().anyMatch(l -> l.isOverdue(today()))) {
            throw new LibraryException(member.getName() + " has overdue books; return them first");
        }
        if (member.getFinesOwed().compareTo(FINE_BLOCK_LIMIT) > 0) {
            throw new LibraryException(member.getName() + " owes $" + member.getFinesOwed()
                    + " in fines (limit $" + FINE_BLOCK_LIMIT + ")");
        }
        if (active.stream().anyMatch(l -> l.getIsbn().equals(book.getIsbn()))) {
            throw new LibraryException(member.getName() + " already has a copy of this book");
        }
        book.lend();
        Loan loan = new Loan(nextLoanId++, book.getIsbn(), member.getId(), today(),
                today().plusDays(LOAN_DAYS), null, 0);
        loans.put(loan.getId(), loan);
        return loan;
    }

    /** Returns the book and charges any late fine. Returns the fine charged. */
    public BigDecimal returnBook(long loanId) {
        Loan loan = findLoan(loanId);
        if (!loan.isActive()) {
            throw new LibraryException("Loan " + loanId + " was already returned on " + loan.getReturned());
        }
        BigDecimal fine = loan.fineOn(today());
        loan.markReturned(today());
        books.get(loan.getIsbn()).giveBack();
        if (fine.signum() > 0) {
            members.get(loan.getMemberId()).addFine(fine);
        }
        return fine;
    }

    public Loan renew(long loanId) {
        Loan loan = findLoan(loanId);
        if (!loan.isActive()) {
            throw new LibraryException("Loan " + loanId + " is not active");
        }
        if (loan.isOverdue(today())) {
            throw new LibraryException("Overdue loans cannot be renewed");
        }
        if (loan.getRenewals() >= MAX_RENEWALS) {
            throw new LibraryException("Loan " + loanId + " has already been renewed");
        }
        loan.renew(LOAN_DAYS);
        return loan;
    }

    public BigDecimal payFine(String memberId, BigDecimal amount) {
        return findMember(memberId).pay(amount);
    }

    public Loan findLoan(long id) {
        Loan loan = loans.get(id);
        if (loan == null) {
            throw new LibraryException("No loan with id " + id);
        }
        return loan;
    }

    public List<Loan> activeLoans(String memberId) {
        return loans.values().stream()
                .filter(Loan::isActive)
                .filter(l -> memberId == null || l.getMemberId().equals(memberId))
                .collect(Collectors.toList());
    }

    public List<Loan> overdueLoans() {
        LocalDate today = today();
        return loans.values().stream()
                .filter(l -> l.isOverdue(today))
                .sorted(Comparator.comparing(Loan::getDue))
                .collect(Collectors.toList());
    }

    public Collection<Loan> getLoans() {
        return Collections.unmodifiableCollection(loans.values());
    }

    // ------------------------------------------------------------------ used by the store
    void restore(List<Book> bookList, List<Member> memberList, List<Loan> loanList) {
        books.clear();
        members.clear();
        loans.clear();
        bookList.forEach(b -> books.put(b.getIsbn(), b));
        memberList.forEach(m -> members.put(m.getId(), m));
        loanList.forEach(l -> loans.put(l.getId(), l));
        nextMemberNumber = 1 + memberList.stream()
                .mapToInt(m -> Integer.parseInt(m.getId().substring(1))).max().orElse(0);
        nextLoanId = 1 + loanList.stream().mapToLong(Loan::getId).max().orElse(0);
    }

    List<Member> memberList() {
        return new ArrayList<>(members.values());
    }
}
