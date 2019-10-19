package io.github.suli350.library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LibraryTest {

    private static final String EFFECTIVE_JAVA = "9780134685991";
    private static final String CLEAN_CODE = "9780132350884";
    private static final String PRAGMATIC = "9780201616224";
    private static final String SICP = "9780262510875";

    private LocalDate today = LocalDate.of(2019, 10, 1);
    private Library lib;
    private Member ann;

    @BeforeEach
    void setUp() {
        lib = new Library(() -> today);
        lib.addBook(EFFECTIVE_JAVA, "Effective Java", "Joshua Bloch", 2018, 2);
        lib.addBook(CLEAN_CODE, "Clean Code", "Robert C. Martin", 2008, 1);
        lib.addBook(PRAGMATIC, "The Pragmatic Programmer", "Hunt, Thomas", 1999, 1);
        lib.addBook(SICP, "Structure and Interpretation of Computer Programs", "Abelson, Sussman", 1996, 1);
        ann = lib.registerMember("Ann Lee", "ann@example.com");
    }

    @Test
    void addingSameIsbnAddsCopies() {
        Book b = lib.addBook("978-0-13-468599-1", "Effective Java", "Joshua Bloch", 2018, 3);
        assertEquals(5, b.getTotalCopies());
        assertEquals(4, lib.getBooks().size());
    }

    @Test
    void checkoutAndReturnOnTime() {
        Loan loan = lib.checkout("m001", EFFECTIVE_JAVA);
        assertEquals(today.plusDays(14), loan.getDue());
        assertEquals(1, lib.findBook(EFFECTIVE_JAVA).getAvailableCopies());
        today = today.plusDays(14);
        assertEquals(BigDecimal.ZERO.compareTo(lib.returnBook(loan.getId())), 0);
        assertEquals(2, lib.findBook(EFFECTIVE_JAVA).getAvailableCopies());
        assertThrows(LibraryException.class, () -> lib.returnBook(loan.getId()));
    }

    @Test
    void lateReturnChargesCappedFine() {
        Loan a = lib.checkout("M001", EFFECTIVE_JAVA);
        Loan b = lib.checkout("M001", CLEAN_CODE);
        today = today.plusDays(14 + 6);                   // 6 days late
        assertEquals(new BigDecimal("1.50"), lib.returnBook(a.getId()));
        today = today.plusDays(100);                      // very late -> capped
        assertEquals(new BigDecimal("10.00"), lib.returnBook(b.getId()));
        assertEquals(new BigDecimal("11.50"), ann.getFinesOwed());
    }

    @Test
    void checkoutRules() {
        Member bob = lib.registerMember("Bob", "");
        lib.checkout("M001", EFFECTIVE_JAVA);
        lib.checkout("M001", CLEAN_CODE);
        lib.checkout("M001", PRAGMATIC);
        assertThrows(LibraryException.class, () -> lib.checkout("M001", SICP));        // max 3
        assertThrows(LibraryException.class, () -> lib.checkout(bob.getId(), CLEAN_CODE)); // no copies
        lib.checkout(bob.getId(), EFFECTIVE_JAVA);                                      // 2nd copy
        assertThrows(LibraryException.class, () -> lib.checkout("M999", SICP));        // unknown member
    }

    @Test
    void membersWithFinesOrOverdueBooksAreBlocked() {
        Loan loan = lib.checkout("M001", EFFECTIVE_JAVA);
        today = today.plusDays(20);
        assertThrows(LibraryException.class, () -> lib.checkout("M001", SICP));   // overdue
        lib.returnBook(loan.getId());                                             // $1.50
        lib.checkout("M001", SICP);                                               // under the limit
        ann.addFine(new BigDecimal("4.00"));
        assertThrows(LibraryException.class, () -> lib.checkout("M001", CLEAN_CODE)); // $5.50 > $5
        assertEquals(new BigDecimal("0.00"), lib.payFine("M001", new BigDecimal("5.50")));
        lib.checkout("M001", CLEAN_CODE);
    }

    @Test
    void paymentReturnsChange() {
        ann.addFine(new BigDecimal("2.00"));
        assertEquals(new BigDecimal("3.00"), lib.payFine("M001", new BigDecimal("5.00")));
        assertEquals(0, ann.getFinesOwed().signum());
    }

    @Test
    void renewOnceOnlyAndNotWhenOverdue() {
        Loan loan = lib.checkout("M001", EFFECTIVE_JAVA);
        lib.renew(loan.getId());
        assertEquals(today.plusDays(28), loan.getDue());
        assertThrows(LibraryException.class, () -> lib.renew(loan.getId()));
        Loan other = lib.checkout("M001", SICP);
        today = today.plusDays(15);
        assertThrows(LibraryException.class, () -> lib.renew(other.getId()));
    }

    @Test
    void cannotRemoveBookOnLoan() {
        lib.checkout("M001", CLEAN_CODE);
        assertThrows(LibraryException.class, () -> lib.removeBook(CLEAN_CODE));
        lib.removeBook(SICP);
        assertEquals(3, lib.getBooks().size());
    }

    @Test
    void searchByTitleAuthorOrIsbn() {
        assertEquals(1, lib.search("bloch").size());
        assertEquals(1, lib.search("978-0-13-235088").size());
        assertEquals(4, lib.search("").size());
        assertEquals("Clean Code", lib.search("")
                .get(0).getTitle()); // sorted by title
    }

    @Test
    void overdueReport() {
        lib.checkout("M001", EFFECTIVE_JAVA);
        today = today.plusDays(15);
        assertEquals(1, lib.overdueLoans().size());
    }

    @Test
    void storeRoundTrip() throws IOException {
        Loan loan = lib.checkout("M001", EFFECTIVE_JAVA);
        Path dir = Files.createTempDirectory("library");
        new LibraryStore(dir).save(lib);

        Library loaded = new Library(() -> today);
        new LibraryStore(dir).load(loaded);
        assertEquals(4, loaded.getBooks().size());
        assertEquals(1, loaded.findBook(EFFECTIVE_JAVA).getAvailableCopies());
        assertEquals("Hunt, Thomas", loaded.findBook(PRAGMATIC).getAuthor());
        assertEquals(loan.getDue(), loaded.findLoan(loan.getId()).getDue());
        assertEquals("M002", loaded.registerMember("New", "").getId());
        assertEquals(2, loaded.checkout("M001", SICP).getId());
    }

    @Test
    void tokenizerKeepsQuotedText() {
        assertEquals(Arrays.asList("add-book", "123", "Effective Java", "Joshua Bloch", "2018"),
                CommandShell.tokenize("add-book 123 \"Effective Java\"   \"Joshua Bloch\" 2018"));
        assertEquals(Arrays.asList("a", ""), CommandShell.tokenize("a \"\""));
        assertThrows(IllegalArgumentException.class, () -> CommandShell.tokenize("x \"open"));
    }

    @Test
    void shellSession() throws IOException {
        String script = String.join("\n",
                "add-member \"Sam Green\" sam@example.com",
                "checkout M002 978-0-13-235088-4",
                "checkout M002 9780132350884",
                "search clean",
                "return 1",
                "bogus",
                "exit") + "\n";
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        Path dir = Files.createTempDirectory("library");
        new CommandShell(lib, new LibraryStore(dir), new BufferedReader(new StringReader(script)),
                new PrintStream(buf, true, StandardCharsets.UTF_8)).run();
        String out = buf.toString(StandardCharsets.UTF_8);
        assertTrue(out.contains("Registered Sam Green as M002"), out);
        assertTrue(out.contains("Loan #1: \"Clean Code\" due 2019-10-15"), out);
        assertTrue(out.contains("already has a copy"), out);
        assertTrue(out.contains("Returned on time"), out);
        assertTrue(out.contains("Unknown command 'bogus'"), out);
        assertTrue(Files.exists(dir.resolve("loans.csv")));
    }
}
