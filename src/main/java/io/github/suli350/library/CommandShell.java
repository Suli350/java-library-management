package io.github.suli350.library;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Interactive command line. Arguments with spaces go in quotes:
 * <pre>add-book 978-0-13-468599-1 "Effective Java" "Joshua Bloch" 2018 2</pre>
 */
public class CommandShell {

    private final Library library;
    private final LibraryStore store;
    private final BufferedReader in;
    private final PrintStream out;

    public CommandShell(Library library, LibraryStore store, BufferedReader in, PrintStream out) {
        this.library = library;
        this.store = store;
        this.in = in;
        this.out = out;
    }

    public void run() throws IOException {
        out.println("Library Manager — type 'help' for commands");
        while (true) {
            out.print("library> ");
            out.flush();
            String line = in.readLine();
            if (line == null) {
                save();
                return;
            }
            if (!execute(line)) {
                return;
            }
        }
    }

    /** Runs one command line. Returns false when the shell should exit. */
    public boolean execute(String line) throws IOException {
        List<String> args;
        try {
            args = tokenize(line);
        } catch (IllegalArgumentException e) {
            out.println("  ✗ " + e.getMessage());
            return true;
        }
        if (args.isEmpty()) {
            return true;
        }
        String cmd = args.remove(0).toLowerCase();
        try {
            switch (cmd) {
                case "help": help(); break;
                case "add-book": addBook(args); break;
                case "remove-book":
                    need(args, 1, "remove-book <isbn>");
                    library.removeBook(args.get(0));
                    out.println("  ✓ Removed");
                    break;
                case "books": list(library.search("")); break;
                case "search":
                    need(args, 1, "search <text>");
                    list(library.search(String.join(" ", args)));
                    break;
                case "add-member": {
                    need(args, 1, "add-member \"<name>\" [email]");
                    Member m = library.registerMember(args.get(0), args.size() > 1 ? args.get(1) : "");
                    out.println("  ✓ Registered " + m.getName() + " as " + m.getId());
                    break;
                }
                case "members": library.getMembers().forEach(m -> out.println("  " + m)); break;
                case "member": memberReport(args); break;
                case "checkout": {
                    need(args, 2, "checkout <member-id> <isbn>");
                    Loan loan = library.checkout(args.get(0), args.get(1));
                    out.println("  ✓ Loan #" + loan.getId() + ": \"" + library.findBook(loan.getIsbn()).getTitle()
                            + "\" due " + loan.getDue());
                    break;
                }
                case "return": {
                    need(args, 1, "return <loan-id>");
                    BigDecimal fine = library.returnBook(parseLong(args.get(0)));
                    out.println(fine.signum() > 0 ? "  ✓ Returned late, fine charged: $" + fine : "  ✓ Returned on time");
                    break;
                }
                case "renew": {
                    need(args, 1, "renew <loan-id>");
                    Loan loan = library.renew(parseLong(args.get(0)));
                    out.println("  ✓ Renewed, now due " + loan.getDue());
                    break;
                }
                case "pay": {
                    need(args, 2, "pay <member-id> <amount>");
                    BigDecimal change = library.payFine(args.get(0), new BigDecimal(args.get(1)));
                    Member m = library.findMember(args.get(0));
                    out.println("  ✓ Payment received. Still owed: $" + m.getFinesOwed()
                            + (change.signum() > 0 ? "   Change: $" + change : ""));
                    break;
                }
                case "loans": printLoans(library.activeLoans(null)); break;
                case "overdue": printLoans(library.overdueLoans()); break;
                case "save": save(); break;
                case "exit":
                case "quit":
                    save();
                    out.println("Goodbye!");
                    return false;
                default: out.println("  Unknown command '" + cmd + "'. Type 'help'.");
            }
        } catch (LibraryException e) {
            out.println("  ✗ " + e.getMessage());
        } catch (NumberFormatException e) {
            out.println("  ✗ Not a number: " + e.getMessage());
        }
        return true;
    }

    private void addBook(List<String> args) {
        need(args, 4, "add-book <isbn> \"<title>\" \"<author>\" <year> [copies]");
        int copies = args.size() > 4 ? Integer.parseInt(args.get(4)) : 1;
        Book b = library.addBook(args.get(0), args.get(1), args.get(2), Integer.parseInt(args.get(3)), copies);
        out.println("  ✓ \"" + b.getTitle() + "\" now has " + b.getTotalCopies() + " cop"
                + (b.getTotalCopies() == 1 ? "y" : "ies"));
    }

    private void memberReport(List<String> args) {
        need(args, 1, "member <member-id>");
        Member m = library.findMember(args.get(0));
        out.println("  " + m);
        printLoans(library.activeLoans(m.getId()));
    }

    private void list(List<Book> books) {
        if (books.isEmpty()) {
            out.println("  (no books)");
        }
        books.forEach(b -> out.println("  " + b));
    }

    private void printLoans(List<Loan> loans) {
        if (loans.isEmpty()) {
            out.println("  (no loans)");
            return;
        }
        for (Loan l : loans) {
            Book b = library.findBook(l.getIsbn());
            String flag = l.isOverdue(library.today())
                    ? "  OVERDUE " + l.daysLate(library.today()) + "d, fine $" + l.fineOn(library.today()) : "";
            out.printf("  #%-4d %-6s %-36s due %s%s%n", l.getId(), l.getMemberId(),
                    Book.truncate(b.getTitle(), 36), l.getDue(), flag);
        }
    }

    private void save() throws IOException {
        store.save(library);
        out.println("  ✓ Saved to " + store.getDirectory());
    }

    private void help() {
        out.println("  Books    add-book <isbn> \"<title>\" \"<author>\" <year> [copies]");
        out.println("           remove-book <isbn> | books | search <text>");
        out.println("  Members  add-member \"<name>\" [email] | members | member <id>");
        out.println("  Loans    checkout <member-id> <isbn> | return <loan-id> | renew <loan-id>");
        out.println("           loans | overdue | pay <member-id> <amount>");
        out.println("  Other    save | help | exit");
        out.println("  Rules    " + Library.LOAN_DAYS + "-day loans, max " + Library.MAX_ACTIVE_LOANS
                + " books, $" + Library.FINE_PER_DAY + "/day late (max $" + Library.MAX_FINE_PER_LOAN
                + " per book), 1 renewal");
    }

    private static void need(List<String> args, int n, String usage) {
        if (args.size() < n) {
            throw new LibraryException("Usage: " + usage);
        }
    }

    private static long parseLong(String s) {
        return Long.parseLong(s.replace("#", ""));
    }

    /** Splits on spaces, keeping "quoted text" together. */
    static List<String> tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean hasToken = false;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                quoted = !quoted;
                hasToken = true;
            } else if (Character.isWhitespace(c) && !quoted) {
                if (hasToken) {
                    tokens.add(current.toString());
                    current.setLength(0);
                    hasToken = false;
                }
            } else {
                current.append(c);
                hasToken = true;
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("Missing closing quote");
        }
        if (hasToken) {
            tokens.add(current.toString());
        }
        return tokens;
    }
}
