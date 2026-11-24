package com.splitter.storage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.splitter.exception.StorageException;
import com.splitter.meta.AIGenerated;
import com.splitter.model.Expense;
import com.splitter.model.Group;
import com.splitter.model.Member;
import com.splitter.model.Money;
import com.splitter.model.SplitMethod;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Package storage, 10 test cases. Techniques: round-trip (state), boundary for next id, and equivalence
 * classes for the error branches that must raise StorageException (DESIGN.md 4.3).
 */
@AIGenerated(tool = "Claude Code", prompt = "T02")
class FileRepositoryTest {

    @TempDir
    Path dir;

    private final FileRepository repo = new FileRepository();

    private static Group sample() {
        Group g = new Group("Trip");
        Member a = new Member("A");
        Member b = new Member("B");
        g.addMember(a);
        g.addMember(b);
        Map<Member, Money> shares = new LinkedHashMap<>();
        shares.put(a, Money.ofCents(6000));
        shares.put(b, Money.ofCents(4000));
        g.addExpense(new Expense(g.allocateExpenseId(), "Lunch", a, Money.ofCents(10000),
                SplitMethod.EXACT, shares));
        g.allocateExpenseId();
        return g;
    }

    @Test
    @DisplayName("TC-F01 round-trips members, order, expense, shares and next id (state)")
    void roundTrip() throws StorageException {
        Path file = dir.resolve("g.csv");
        Group original = sample();
        repo.save(original, file);
        Group loaded = repo.load(file);
        assertEquals(original.getName(), loaded.getName());
        assertEquals(original.getMembers(), loaded.getMembers());
        assertEquals(1, loaded.getExpenses().size());
        assertEquals(original.getExpenses().get(0).getShares(),
                loaded.getExpenses().get(0).getShares());
        assertEquals(original.getNextExpenseId(), loaded.getNextExpenseId());
    }

    @Test
    @DisplayName("TC-F02 next id stays after an undone expense (boundary: encoded NEXT_ID)")
    void nextIdPreserved() throws StorageException {
        Group g = new Group("G");
        Member a = new Member("A");
        g.addMember(a);
        g.allocateExpenseId();
        g.allocateExpenseId();
        Path file = dir.resolve("g.csv");
        repo.save(g, file);
        assertEquals(3, repo.load(file).getNextExpenseId());
    }

    @Test
    @DisplayName("TC-F03 names/descriptions with commas and quotes survive (equivalence: special chars)")
    void specialCharacters() throws StorageException {
        Group g = new Group("Trip, \"2026\"");
        Member li = new Member("Li, \"Ben\"");
        g.addMember(li);
        Map<Member, Money> shares = new LinkedHashMap<>();
        shares.put(li, Money.ofCents(100));
        g.addExpense(new Expense(g.allocateExpenseId(), "dinner, \"shared\"", li, Money.ofCents(100),
                SplitMethod.EQUAL, shares));
        Path file = dir.resolve("g.csv");
        repo.save(g, file);
        Group loaded = repo.load(file);
        assertEquals(li, loaded.getMembers().get(0));
        assertEquals("dinner, \"shared\"", loaded.getExpenses().get(0).getDescription());
    }

    @Test
    @DisplayName("TC-F04 Chinese names survive round trip (UTF-8)")
    void chineseNames() throws StorageException {
        Group g = new Group("Trip");
        Member cn = new Member("小明");
        g.addMember(cn);
        g.addExpense(new Expense(g.allocateExpenseId(), "午餐", cn, Money.ofCents(100), SplitMethod.EQUAL,
                Map.of(cn, Money.ofCents(100))));
        Path file = dir.resolve("g.csv");
        repo.save(g, file);
        assertEquals(cn, repo.load(file).getMembers().get(0));
    }

    @Test
    @DisplayName("TC-F05 saved file starts with UTF-8 BOM EF BB BF (boundary bytes 4.3)")
    void writesBom() throws IOException {
        Path file = dir.resolve("g.csv");
        try {
            repo.save(new Group("G"), file);
        } catch (StorageException e) {
            throw new AssertionError(e);
        }
        byte[] bytes = Files.readAllBytes(file);
        assertArrayEquals(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
                new byte[]{bytes[0], bytes[1], bytes[2]});
    }

    @Test
    @DisplayName("TC-F06 a file without BOM still loads (equivalence 4.3)")
    void loadsWithoutBom() throws IOException, StorageException {
        Path file = dir.resolve("g.csv");
        Files.writeString(file, "EXPENSE_SPLITTER,1\nGROUP,G\nNEXT_ID,1\n", UTF_8);
        assertEquals("G", repo.load(file).getName());
    }

    @Test
    @DisplayName("TC-F07 missing and empty files raise StorageException (equivalence)")
    void missingAndEmpty() throws IOException {
        assertThrows(StorageException.class, () -> repo.load(dir.resolve("nope.csv")));
        Path empty = dir.resolve("empty.csv");
        Files.createFile(empty);
        assertThrows(StorageException.class, () -> repo.load(empty));
    }

    @Test
    @DisplayName("TC-F08 wrong version and unknown record type raise StorageException (equivalence)")
    void wrongVersionAndType() throws IOException {
        Path wrongVersion = dir.resolve("v.csv");
        Files.writeString(wrongVersion, "EXPENSE_SPLITTER,9\nGROUP,G\nNEXT_ID,1\n", UTF_8);
        assertThrows(StorageException.class, () -> repo.load(wrongVersion));
        Path unknown = dir.resolve("t.csv");
        Files.writeString(unknown, "EXPENSE_SPLITTER,1\nGROUP,G\nNEXT_ID,1\nBOGUS,x\n", UTF_8);
        assertThrows(StorageException.class, () -> repo.load(unknown));
    }

    @Test
    @DisplayName("TC-F09 shares that do not sum to total raise StorageException (equivalence)")
    void inconsistentShares() throws IOException {
        Path file = dir.resolve("g.csv");
        Files.writeString(file, "EXPENSE_SPLITTER,1\nGROUP,G\nNEXT_ID,2\nMEMBER,A\n"
                + "EXPENSE,1,d,A,EQUAL,100\nSHARE,1,A,50\n", UTF_8);
        assertThrows(StorageException.class, () -> repo.load(file));
    }

    @Test
    @DisplayName("TC-F10 duplicate expense id and SHARE before EXPENSE raise StorageException (equivalence)")
    void duplicateAndOrderErrors() throws IOException {
        Path dup = dir.resolve("dup.csv");
        Files.writeString(dup, "EXPENSE_SPLITTER,1\nGROUP,G\nNEXT_ID,3\nMEMBER,A\n"
                + "EXPENSE,1,d,A,EQUAL,100\nSHARE,1,A,100\n"
                + "EXPENSE,1,e,A,EQUAL,100\nSHARE,1,A,100\n", UTF_8);
        assertThrows(StorageException.class, () -> repo.load(dup));
        Path order = dir.resolve("order.csv");
        Files.writeString(order, "EXPENSE_SPLITTER,1\nGROUP,G\nNEXT_ID,2\nMEMBER,A\n"
                + "SHARE,1,A,100\nEXPENSE,1,d,A,EQUAL,100\n", UTF_8);
        assertThrows(StorageException.class, () -> repo.load(order));
    }

    @Test
    @DisplayName("TC-F11 bad number (non-numeric cents) raises StorageException (equivalence)")
    void badNumberRejected() throws IOException {
        Path file = dir.resolve("bad.csv");
        Files.writeString(file, "EXPENSE_SPLITTER,1\nGROUP,G\nNEXT_ID,2\nMEMBER,A\n"
                + "EXPENSE,1,d,A,EQUAL,abc\n", UTF_8);
        assertThrows(StorageException.class, () -> repo.load(file));
    }

    @Test
    @DisplayName("TC-F12 duplicate share for one member raises StorageException (equivalence)")
    void duplicateShareRejected() throws IOException {
        Path file = dir.resolve("dup.csv");
        Files.writeString(file, "EXPENSE_SPLITTER,1\nGROUP,G\nNEXT_ID,2\nMEMBER,A\n"
                + "EXPENSE,1,d,A,EQUAL,100\nSHARE,1,A,60\nSHARE,1,A,40\n", UTF_8);
        assertThrows(StorageException.class, () -> repo.load(file));
    }

    @Test
    @DisplayName("TC-F13 an unclosed quoted field raises StorageException (equivalence)")
    void unclosedQuoteRejected() throws IOException {
        Path file = dir.resolve("q.csv");
        Files.writeString(file, "EXPENSE_SPLITTER,1\nGROUP,\"G\nNEXT_ID,1\n", UTF_8);
        assertThrows(StorageException.class, () -> repo.load(file));
    }

    @Test
    @DisplayName("TC-F14 a file that is not valid UTF-8 raises StorageException (equivalence)")
    void nonUtf8Rejected() throws IOException {
        Path file = dir.resolve("bin.csv");
        Files.write(file, new byte[]{(byte) 0xFF, (byte) 0xFE, (byte) 0x00, (byte) 0x81});
        assertThrows(StorageException.class, () -> repo.load(file));
    }

    @Test
    @DisplayName("TC-F15 a SHARE for an unknown member raises StorageException (equivalence)")
    void unknownMemberRejected() throws IOException {
        Path file = dir.resolve("u.csv");
        Files.writeString(file, "EXPENSE_SPLITTER,1\nGROUP,G\nNEXT_ID,2\nMEMBER,A\n"
                + "EXPENSE,1,d,A,EQUAL,100\nSHARE,1,B,100\n", UTF_8);
        assertThrows(StorageException.class, () -> repo.load(file));
    }
}
