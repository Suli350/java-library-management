package io.github.suli350.library;

/** ISBN-10 / ISBN-13 validation and normalisation (hyphens and spaces are ignored). */
public final class Isbn {

    private Isbn() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            throw new LibraryException("ISBN is required");
        }
        String isbn = raw.replace("-", "").replace(" ", "").toUpperCase();
        if (!isValid(isbn)) {
            throw new LibraryException("Invalid ISBN: " + raw);
        }
        return isbn;
    }

    public static boolean isValid(String isbn) {
        if (isbn.matches("\\d{9}[\\dX]")) {
            int sum = 0;
            for (int i = 0; i < 10; i++) {
                char c = isbn.charAt(i);
                int digit = c == 'X' ? 10 : c - '0';
                sum += digit * (10 - i);
            }
            return sum % 11 == 0;
        }
        if (isbn.matches("\\d{13}")) {
            int sum = 0;
            for (int i = 0; i < 13; i++) {
                int digit = isbn.charAt(i) - '0';
                sum += i % 2 == 0 ? digit : digit * 3;
            }
            return sum % 10 == 0;
        }
        return false;
    }
}
