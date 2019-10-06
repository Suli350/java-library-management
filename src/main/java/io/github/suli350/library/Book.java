package io.github.suli350.library;

public class Book {

    private final String isbn;
    private final String title;
    private final String author;
    private final int year;
    private int totalCopies;
    private int availableCopies;

    public Book(String isbn, String title, String author, int year, int totalCopies, int availableCopies) {
        if (title == null || title.isBlank() || author == null || author.isBlank()) {
            throw new LibraryException("Title and author are required");
        }
        if (totalCopies < 1 || availableCopies < 0 || availableCopies > totalCopies) {
            throw new LibraryException("Invalid copy counts");
        }
        this.isbn = isbn;
        this.title = title.trim();
        this.author = author.trim();
        this.year = year;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    void addCopies(int n) {
        totalCopies += n;
        availableCopies += n;
    }

    void lend() {
        if (availableCopies == 0) {
            throw new LibraryException("No copies of \"" + title + "\" are available");
        }
        availableCopies--;
    }

    void giveBack() {
        availableCopies++;
    }

    public boolean matches(String query) {
        String q = query.toLowerCase();
        return title.toLowerCase().contains(q) || author.toLowerCase().contains(q)
                || isbn.contains(q.replace("-", ""));
    }

    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public int getYear() { return year; }
    public int getTotalCopies() { return totalCopies; }
    public int getAvailableCopies() { return availableCopies; }

    @Override
    public String toString() {
        return String.format("%-13s  %-36s %-22s %4d  %d/%d available", isbn, truncate(title, 36),
                truncate(author, 22), year, availableCopies, totalCopies);
    }

    static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
