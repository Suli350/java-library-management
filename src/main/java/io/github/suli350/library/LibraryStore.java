package io.github.suli350.library;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Stores the library as three CSV files (books.csv, members.csv, loans.csv) in one folder. */
public class LibraryStore {

    private final Path dir;

    public LibraryStore(Path dir) {
        this.dir = dir;
    }

    public Path getDirectory() {
        return dir;
    }

    public void save(Library library) throws IOException {
        Files.createDirectories(dir);
        List<String> books = new ArrayList<>();
        books.add("isbn,title,author,year,total,available");
        for (Book b : library.getBooks()) {
            books.add(Csv.join(b.getIsbn(), b.getTitle(), b.getAuthor(), String.valueOf(b.getYear()),
                    String.valueOf(b.getTotalCopies()), String.valueOf(b.getAvailableCopies())));
        }
        List<String> members = new ArrayList<>();
        members.add("id,name,email,joined,fines");
        for (Member m : library.getMembers()) {
            members.add(Csv.join(m.getId(), m.getName(), m.getEmail(), m.getJoined().toString(),
                    m.getFinesOwed().toPlainString()));
        }
        List<String> loans = new ArrayList<>();
        loans.add("id,isbn,member,borrowed,due,returned,renewals");
        for (Loan l : library.getLoans()) {
            loans.add(Csv.join(String.valueOf(l.getId()), l.getIsbn(), l.getMemberId(),
                    l.getBorrowed().toString(), l.getDue().toString(),
                    l.getReturned() == null ? "" : l.getReturned().toString(), String.valueOf(l.getRenewals())));
        }
        Files.write(dir.resolve("books.csv"), books, StandardCharsets.UTF_8);
        Files.write(dir.resolve("members.csv"), members, StandardCharsets.UTF_8);
        Files.write(dir.resolve("loans.csv"), loans, StandardCharsets.UTF_8);
    }

    public void load(Library library) throws IOException {
        List<Book> books = new ArrayList<>();
        for (List<String> r : rows("books.csv", 6)) {
            books.add(new Book(r.get(0), r.get(1), r.get(2), Integer.parseInt(r.get(3)),
                    Integer.parseInt(r.get(4)), Integer.parseInt(r.get(5))));
        }
        List<Member> members = new ArrayList<>();
        for (List<String> r : rows("members.csv", 5)) {
            members.add(new Member(r.get(0), r.get(1), r.get(2), LocalDate.parse(r.get(3)),
                    new BigDecimal(r.get(4))));
        }
        List<Loan> loans = new ArrayList<>();
        for (List<String> r : rows("loans.csv", 7)) {
            loans.add(new Loan(Long.parseLong(r.get(0)), r.get(1), r.get(2), LocalDate.parse(r.get(3)),
                    LocalDate.parse(r.get(4)), r.get(5).isEmpty() ? null : LocalDate.parse(r.get(5)),
                    Integer.parseInt(r.get(6))));
        }
        library.restore(books, members, loans);
    }

    private List<List<String>> rows(String name, int fields) throws IOException {
        Path file = dir.resolve(name);
        List<List<String>> rows = new ArrayList<>();
        if (!Files.exists(file)) {
            return rows;
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }
            List<String> r = Csv.split(lines.get(i));
            if (r.size() != fields) {
                throw new IOException(name + " line " + (i + 1) + ": expected " + fields + " fields");
            }
            rows.add(r);
        }
        return rows;
    }
}
