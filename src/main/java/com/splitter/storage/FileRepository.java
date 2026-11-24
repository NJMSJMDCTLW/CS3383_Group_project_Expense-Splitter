package com.splitter.storage;

import com.splitter.exception.StorageException;
import com.splitter.exception.ValidationException;
import com.splitter.meta.AIGenerated;
import com.splitter.model.Expense;
import com.splitter.model.Group;
import com.splitter.model.Member;
import com.splitter.model.Money;
import com.splitter.model.SplitMethod;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * CSV implementation of {@link Repository} (DESIGN.md 4.3). No third-party library; the file is always
 * read and written as UTF-8.
 *
 * <pre>
 * EXPENSE_SPLITTER,1
 * GROUP,name
 * NEXT_ID,n
 * MEMBER,name
 * EXPENSE,id,description,payer,method,totalCents
 * SHARE,expenseId,member,cents
 * </pre>
 *
 * Fields that contain a comma, a quote or a line break are put in double quotes, and a quote inside a
 * quoted field is written twice.
 */
@AIGenerated(tool = "Claude Code", prompt = "P06", refinedBy = "Wang Man Nga", changes = "Replaced invisible BOM literal with an escape; save() now writes a UTF-8 BOM so Excel shows non-ASCII names correctly (Issue #2)")
public class FileRepository implements Repository {

    /** Excel needs to recognise a CSV file as UTF-8. */
    private static final String BOM = "\uFEFF";
    private static final String MARKER = "EXPENSE_SPLITTER";
    private static final String VERSION = "1";

    /** One parsed, non-blank line of the file. */
    private record Rec(int line, List<String> fields) {
        String type() {
            return fields.get(0);
        }
    }

    /** An expense read from the file, before it is validated and added to the group. */
    private static final class Pending {
        private final int line;
        private final int id;
        private final String description;
        private final Member payer;
        private final SplitMethod method;
        private final long totalCents;
        private final Map<Member, Money> shares = new LinkedHashMap<>();

        private Pending(int line, int id, String description, Member payer, SplitMethod method,
                        long totalCents) {
            this.line = line;
            this.id = id;
            this.description = description;
            this.payer = payer;
            this.method = method;
            this.totalCents = totalCents;
        }
    }

    // ------------------------------------------------------------------ save

    @Override
    public void save(Group g, Path file) throws StorageException {
        Objects.requireNonNull(g, "group");
        Objects.requireNonNull(file, "file");
        String content = serialize(g);
        Path target = file.toAbsolutePath();
        Path dir = target.getParent();
        if (dir == null) {
            throw new StorageException("Cannot save to \"" + file + "\": it is not a valid file path.", null);
        }
        Path temp = null;
        try {
            // Write a temporary file first, so a failed save never leaves a half-written file behind.
            temp = Files.createTempFile(dir, "expense-splitter-", ".tmp");
            Files.writeString(temp, BOM + content, StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // The original problem is more useful to the user than this one.
                }
            }
            throw new StorageException("Cannot save to \"" + file + "\": " + describe(e), e);
        }
    }

    private static String serialize(Group g) {
        StringBuilder out = new StringBuilder();
        append(out, MARKER, VERSION);
        append(out, "GROUP", g.getName());
        append(out, "NEXT_ID", Integer.toString(g.getNextExpenseId()));
        for (Member member : g.getMembers()) {
            append(out, "MEMBER", member.getName());
        }
        for (Expense expense : g.getExpenses()) {
            append(out, "EXPENSE", Integer.toString(expense.getId()), expense.getDescription(),
                    expense.getPayer().getName(), expense.getMethod().name(),
                    Long.toString(expense.getTotal().cents()));
            for (Map.Entry<Member, Money> share : expense.getShares().entrySet()) {
                append(out, "SHARE", Integer.toString(expense.getId()), share.getKey().getName(),
                        Long.toString(share.getValue().cents()));
            }
        }
        return out.toString();
    }

    private static void append(StringBuilder out, String... fields) {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(quote(fields[i]));
        }
        out.append('\n');
    }

    private static String quote(String field) {
        boolean special = field.indexOf(',') >= 0 || field.indexOf('"') >= 0
                || field.indexOf('\n') >= 0 || field.indexOf('\r') >= 0;
        if (!special) {
            return field;
        }
        return '"' + field.replace("\"", "\"\"") + '"';
    }

    // ------------------------------------------------------------------ load

    @Override
    public Group load(Path file) throws StorageException {
        Objects.requireNonNull(file, "file");
        List<Rec> recs = readRecords(file);
        if (recs.isEmpty()) {
            throw readError(file, "the file is empty.");
        }

        Rec first = recs.get(0);
        if (!first.type().equals(MARKER)) {
            throw readError(file, "line " + first.line() + ": this is not an Expense Splitter file "
                    + "(the first line must start with " + MARKER + ").");
        }
        requireFields(file, first, 2);
        if (!first.fields().get(1).equals(VERSION)) {
            throw readError(file, "line " + first.line() + ": unsupported format version \""
                    + first.fields().get(1) + "\" (this program reads version " + VERSION + ").");
        }

        Rec groupRec = requireRecord(file, recs, 1, "GROUP", 2);
        Rec nextRec = requireRecord(file, recs, 2, "NEXT_ID", 2);
        Group group;
        try {
            group = new Group(groupRec.fields().get(1));
        } catch (ValidationException e) {
            throw readError(file, "line " + groupRec.line() + ": " + e.getMessage());
        }
        int nextId = parseInt(file, nextRec, 1, "next expense id");

        List<Pending> pending = new ArrayList<>();
        Map<Integer, Pending> pendingById = new LinkedHashMap<>();
        for (int i = 3; i < recs.size(); i++) {
            Rec rec = recs.get(i);
            switch (rec.type()) {
                case "MEMBER":
                    requireFields(file, rec, 2);
                    try {
                        group.addMember(new Member(rec.fields().get(1)));
                    } catch (ValidationException e) {
                        throw readError(file, "line " + rec.line() + ": " + e.getMessage());
                    }
                    break;
                case "EXPENSE":
                    requireFields(file, rec, 6);
                    Pending p = readExpense(file, rec);
                    if (pendingById.containsKey(p.id)) {
                        throw readError(file, "line " + rec.line() + ": the expense id " + p.id
                                + " appears more than once.");
                    }
                    pending.add(p);
                    pendingById.put(p.id, p);
                    break;
                case "SHARE":
                    requireFields(file, rec, 4);
                    readShare(file, rec, pendingById);
                    break;
                default:
                    throw readError(file, "line " + rec.line() + ": unknown record type \"" + rec.type()
                            + "\".");
            }
        }

        for (Pending p : pending) {
            try {
                Expense expense = new Expense(p.id, p.description, p.payer, Money.ofCents(p.totalCents),
                        p.method, p.shares);
                group.addExpense(expense);
            } catch (ValidationException e) {
                throw readError(file, "line " + p.line + ": the data of expense " + p.id
                        + " is inconsistent: " + e.getMessage());
            }
        }
        try {
            group.restoreNextExpenseId(nextId);
        } catch (ValidationException e) {
            throw readError(file, "line " + nextRec.line() + ": " + e.getMessage());
        }
        return group;
    }

    private static Pending readExpense(Path file, Rec rec) throws StorageException {
        int id = parseInt(file, rec, 1, "expense id");
        String description = rec.fields().get(2);
        Member payer = member(file, rec, 3);
        SplitMethod method;
        try {
            method = SplitMethod.valueOf(rec.fields().get(4));
        } catch (IllegalArgumentException e) {
            throw readError(file, "line " + rec.line() + ": unknown split method \"" + rec.fields().get(4)
                    + "\".");
        }
        long total = parseLong(file, rec, 5, "total");
        return new Pending(rec.line(), id, description, payer, method, total);
    }

    private static void readShare(Path file, Rec rec, Map<Integer, Pending> pendingById)
            throws StorageException {
        int expenseId = parseInt(file, rec, 1, "expense id");
        Pending target = pendingById.get(expenseId);
        if (target == null) {
            throw readError(file, "line " + rec.line() + ": this share belongs to the expense " + expenseId
                    + ", which is not declared before this line.");
        }
        Member member = member(file, rec, 2);
        long cents = parseLong(file, rec, 3, "share");
        if (target.shares.containsKey(member)) {
            throw readError(file, "line " + rec.line() + ": the expense " + expenseId
                    + " has more than one share for \"" + member.getName() + "\".");
        }
        target.shares.put(member, Money.ofCents(cents));
    }

    private static Member member(Path file, Rec rec, int index) throws StorageException {
        try {
            return new Member(rec.fields().get(index));
        } catch (ValidationException e) {
            throw readError(file, "line " + rec.line() + ": " + e.getMessage());
        }
    }

    private static int parseInt(Path file, Rec rec, int index, String what) throws StorageException {
        String text = rec.fields().get(index);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw readError(file, "line " + rec.line() + ": \"" + text + "\" is not a valid " + what + ".");
        }
    }

    private static long parseLong(Path file, Rec rec, int index, String what) throws StorageException {
        String text = rec.fields().get(index);
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw readError(file, "line " + rec.line() + ": \"" + text + "\" is not a valid " + what
                    + " (a whole number of cents is expected).");
        }
    }

    private static void requireFields(Path file, Rec rec, int count) throws StorageException {
        if (rec.fields().size() != count) {
            throw readError(file, "line " + rec.line() + ": a " + rec.type() + " record needs " + count
                    + " fields but has " + rec.fields().size() + ".");
        }
    }

    private static Rec requireRecord(Path file, List<Rec> recs, int index, String type, int fieldCount)
            throws StorageException {
        if (index >= recs.size()) {
            throw readError(file, "the file is incomplete: the " + type + " record is missing.");
        }
        Rec rec = recs.get(index);
        if (!rec.type().equals(type)) {
            throw readError(file, "line " + rec.line() + ": a " + type + " record was expected but "
                    + rec.type() + " was found.");
        }
        requireFields(file, rec, fieldCount);
        return rec;
    }

    // --------------------------------------------------------------- reading

    /** Reads the file as UTF-8 and parses every non-blank line into fields. */
    private static List<Rec> readRecords(Path file) throws StorageException {
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            throw readError(file, "the file does not exist.");
        } catch (CharacterCodingException e) {
            throw readError(file, "the file is not valid UTF-8 text.");
        } catch (IOException e) {
            throw readError(file, "the file cannot be read (" + describe(e) + ").");
        }
        List<Rec> recs = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (i == 0 && line.startsWith(BOM)) {
                line = line.substring(1);
            }
            if (line.isBlank()) {
                continue;
            }
            recs.add(new Rec(i + 1, parseLine(file, i + 1, line)));
        }
        return recs;
    }

    /** Splits one line into fields; a field in double quotes may contain commas and doubled quotes. */
    private static List<String> parseLine(Path file, int lineNo, String line) throws StorageException {
        List<String> fields = new ArrayList<>();
        int n = line.length();
        int i = 0;
        while (true) {
            StringBuilder field = new StringBuilder();
            if (i < n && line.charAt(i) == '"') {
                i++;
                boolean closed = false;
                while (i < n) {
                    char c = line.charAt(i);
                    if (c == '"') {
                        if (i + 1 < n && line.charAt(i + 1) == '"') {
                            field.append('"');
                            i += 2;
                        } else {
                            i++;
                            closed = true;
                            break;
                        }
                    } else {
                        field.append(c);
                        i++;
                    }
                }
                if (!closed) {
                    throw readError(file, "line " + lineNo + ": a quoted field is not closed.");
                }
                if (i < n && line.charAt(i) != ',') {
                    throw readError(file, "line " + lineNo + ": unexpected text after a closing quote.");
                }
            } else {
                while (i < n && line.charAt(i) != ',') {
                    if (line.charAt(i) == '"') {
                        throw readError(file, "line " + lineNo
                                + ": a quote may only appear inside a quoted field.");
                    }
                    field.append(line.charAt(i));
                    i++;
                }
            }
            fields.add(field.toString());
            if (i >= n) {
                break;
            }
            i++;
        }
        return fields;
    }

    // -------------------------------------------------------------- messages

    private static StorageException readError(Path file, String problem) {
        return new StorageException("Cannot read \"" + file + "\": " + problem, null);
    }

    private static String describe(IOException e) {
        if (e instanceof NoSuchFileException) {
            return "the file or folder does not exist";
        }
        if (e instanceof AccessDeniedException) {
            return "access denied";
        }
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }
}
