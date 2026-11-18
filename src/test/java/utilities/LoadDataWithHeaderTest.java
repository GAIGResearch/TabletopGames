package utilities;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LoadDataWithHeaderTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private String writeFile(String name, String header, List<String> rows) throws IOException {
        File file = folder.newFile(name);
        List<String> lines = new ArrayList<>();
        lines.add(header);
        lines.addAll(rows);
        Files.write(file.toPath(), lines);
        return file.getPath();
    }

    private static List<String> rows(int from, int toExclusive) {
        List<String> retValue = new ArrayList<>();
        for (int i = from; i < toExclusive; i++)
            retValue.add(i + "\t" + (i * 10) + "\t" + "x" + i);
        return retValue;
    }

    private static List<String> firstColumn(List<List<String>> data) {
        return data.stream().map(List::getFirst).toList();
    }

    private static List<String> numbers(int from, int toExclusive) {
        List<String> retValue = new ArrayList<>();
        for (int i = from; i < toExclusive; i++)
            retValue.add(String.valueOf(i));
        return retValue;
    }

    @Test
    public void keepsOnlyTheLastRowsOfASingleFileInOrder() throws IOException {
        String file = writeFile("data.txt", "A\tB\tC", rows(0, 100));
        Pair<List<String>, List<List<String>>> data = Utils.loadDataWithHeader("\t", 10, file);
        assertEquals(List.of("A", "B", "C"), data.a);
        assertEquals(numbers(90, 100), firstColumn(data.b));
        assertEquals(List.of("99", "990", "x99"), data.b.getLast());
    }

    @Test
    public void returnsAllRowsWhenFewerThanTheLimit() throws IOException {
        String file = writeFile("data.txt", "A\tB\tC", rows(0, 5));
        Pair<List<String>, List<List<String>>> data = Utils.loadDataWithHeader("\t", 10, file);
        assertEquals(numbers(0, 5), firstColumn(data.b));
    }

    @Test
    public void lastRowsAreTakenAcrossFilesInOrder() throws IOException {
        String first = writeFile("first.txt", "A\tB\tC", rows(0, 20));
        String second = writeFile("second.txt", "A\tB\tC", rows(20, 25));
        Pair<List<String>, List<List<String>>> data = Utils.loadDataWithHeader("\t", 8, first, second);
        // the last 8 rows: 3 from the end of the first file, then all 5 from the second
        assertEquals(numbers(17, 25), firstColumn(data.b));
    }

    @Test
    public void rowsWithTheWrongColumnCountDoNotCountTowardsTheLimit() throws IOException {
        List<String> data = new ArrayList<>(rows(0, 10));
        data.add("bad\trow");                 // too few columns
        data.add("10\t100\tx10");
        data.add("bad\trow\twith\tfour");     // too many columns
        data.add("11\t110\tx11");
        String file = writeFile("data.txt", "A\tB\tC", data);
        Pair<List<String>, List<List<String>>> loaded = Utils.loadDataWithHeader("\t", 5, file);
        assertEquals(List.of("7", "8", "9", "10", "11"), firstColumn(loaded.b));
    }

    @Test
    public void matchesTheTailOfTheUnlimitedLoad() throws IOException {
        String first = writeFile("first.txt", "A\tB\tC", rows(0, 50));
        String second = writeFile("second.txt", "A\tB\tC", rows(50, 70));
        List<List<String>> all = Utils.loadDataWithHeader("\t", first, second).b;
        List<List<String>> limited = Utils.loadDataWithHeader("\t", 30, first, second).b;
        assertEquals(all.subList(all.size() - 30, all.size()), limited);
    }
}
