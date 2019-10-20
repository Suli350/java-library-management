# Library Management System (Java, command line)

A small but complete library system: a catalogue of books with multiple copies,
members, loans, renewals, late fines and reports, driven from an interactive
command shell and saved as CSV files.

## Example session

```
library> add-book 978-0-13-468599-1 "Effective Java" "Joshua Bloch" 2018 2
  ✓ "Effective Java" now has 2 copies
library> add-member "Ann Lee" ann@example.com
  ✓ Registered Ann Lee as M001
library> checkout M001 9780134685991
  ✓ Loan #1: "Effective Java" due 2019-10-15
library> overdue
  (no loans)
library> return 1
  ✓ Returned on time
library> exit
```

## Rules

| Rule | Value |
|---|---|
| Loan period | 14 days |
| Max books per member | 3 |
| Renewals | 1 per loan, not when overdue |
| Late fine | $0.25 per day, max $10 per book |
| Borrowing blocked | with overdue books, or fines above $5 |

ISBN-10 and ISBN-13 check digits are validated, and hyphens are optional.

## Commands

```
add-book <isbn> "<title>" "<author>" <year> [copies]   remove-book <isbn>
books | search <text>
add-member "<name>" [email] | members | member <id>
checkout <member-id> <isbn> | return <loan-id> | renew <loan-id>
loans | overdue | pay <member-id> <amount>
save | help | exit
```

## Design

- `Library`: all business rules. It takes a `Supplier<LocalDate>` clock, so tests can move time forward.
- `Book`, `Member`, `Loan`: domain objects
- `Isbn`: checksum validation
- `LibraryStore` + `Csv`: persistence to `books.csv`, `members.csv`, `loans.csv`
- `CommandShell`: tokenizer (quoted arguments) and command dispatch

## Build and run

```bash
mvn test
mvn package
java -jar target/library-management-1.0.0.jar              # data in ~/.library-manager
java -jar target/library-management-1.0.0.jar ./demo-data   # custom folder
```

## Ideas for extending it

- Reservations (hold queue) when all copies are out
- Email reminders two days before the due date
- A Swing or JavaFX front end on top of the same `Library` class
