package io.github.suli350.library;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        Path dir = args.length > 0
                ? Paths.get(args[0])
                : Paths.get(System.getProperty("user.home"), ".library-manager");
        Library library = new Library(LocalDate::now);
        LibraryStore store = new LibraryStore(dir);
        store.load(library);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        new CommandShell(library, store, in, System.out).run();
    }
}
