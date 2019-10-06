package io.github.suli350.library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IsbnTest {

    @Test
    void validIsbns() {
        assertTrue(Isbn.isValid("9780134685991"));   // Effective Java, 3rd ed.
        assertTrue(Isbn.isValid("0306406152"));
        assertTrue(Isbn.isValid("080442957X"));
        assertEquals("9780134685991", Isbn.normalize("978-0-13-468599-1"));
    }

    @Test
    void invalidIsbns() {
        assertFalse(Isbn.isValid("9780134685992"));
        assertFalse(Isbn.isValid("0306406153"));
        assertFalse(Isbn.isValid("12345"));
        assertThrows(LibraryException.class, () -> Isbn.normalize("hello"));
    }
}
