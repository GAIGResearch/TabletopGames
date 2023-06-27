package players.rl;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class DataProcessor {

    private enum Db {
        id, alpha, gamma, epsilon, solver, nGamesTotal, nGamesThisId, startId, startNGames
    }

    private final String[] header = Arrays.stream(Db.values()).map(Enum::name).toArray(String[]::new);

    private final int db_id = Db.id.ordinal();

    private QWeightsDataStructure qwds;

    private final String dbFileName = "qWeightsDB.csv";
    private final String dbPath;
    public final String qWeightsFolderPath;

    DataProcessor(QWeightsDataStructure qwds) {
        this.qwds = qwds;
        this.dbPath = qwds.getGameFolderPath() + dbFileName;
        this.qWeightsFolderPath = qwds.getQWeightsFolderPath();
        createMissingFoldersAndFiles();
        cleanDeletedFilesFromDB();
    }

    int writeData(int nGames) {
        int id = findFirstFreeId();
        writeDatabase(updateEntries(id, nGames, false));
        return id;
    }

    void writeData(int id, int nGames) {
        writeDatabase(updateEntries(id, nGames, true));
    }

    private void createMissingFoldersAndFiles() {
        new File(qWeightsFolderPath).mkdirs();
        initDatabase();
    }

    private String[] formatData(int id, int nGamesAdded) {
        int startId = qwds.params.qWeightsFileId;

        String _nGamesThisId = readDatabaseEntry(id, Db.nGamesThisId);
        int nGamesThisId = (_nGamesThisId != null ? Integer.parseInt(_nGamesThisId) : 0) + nGamesAdded;

        String _startNGames = readDatabaseEntry(startId, Db.nGamesTotal);
        int startNGames = _startNGames != null ? Integer.parseInt(_startNGames) : 0;

        Map<Db, String> entries = new HashMap<Db, String>() {
            {
                put(Db.id, Integer.toString(id));
                put(Db.alpha, Double.toString(qwds.trainingParams.alpha));
                put(Db.gamma, String.format("%.3f", qwds.trainingParams.gamma));
                put(Db.epsilon, Double.toString(qwds.params.epsilon));
                put(Db.solver, qwds.trainingParams.solver.name());
                put(Db.nGamesTotal, Integer.toString(startNGames + nGamesThisId));
                put(Db.nGamesThisId, Integer.toString(nGamesThisId));
                put(Db.startId, Integer.toString(startId));
                put(Db.startNGames, Integer.toString(startNGames));
            }
        };
        return Arrays.stream(Db.values()).map(col -> entries.getOrDefault(col, "")).toArray(String[]::new);
    }

    private String readDatabaseEntry(int id, Db col) {
        String[] entry = readDatabaseEntry(id);
        if (entry != null)
            return entry[col.ordinal()];
        return null;
    }

    private String[] readDatabaseEntry(int id) {
        try (CSVReader reader = new CSVReader(new FileReader(dbPath))) {
            String[] nextLine;
            reader.skip(1); // Header
            while ((nextLine = reader.readNext()) != null)
                if (Integer.parseInt(nextLine[db_id]) == id)
                    return nextLine;
            reader.close();
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<String[]> readDatabase() {
        try (CSVReader reader = new CSVReader(new FileReader(dbPath))) {
            List<String[]> db = reader.readAll().stream().collect(Collectors.toCollection(LinkedList::new));
            db.remove(0);
            return db;
        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void initDatabase() {
        File file = new File(dbPath);
        boolean fileExists = file.exists();
        if (!fileExists) {
            try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
                file.createNewFile();
                writer.writeNext(header);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        file.setReadOnly();
    }

    private void writeDatabase(List<String[]> entries) {
        File file = new File(dbPath);
        file.setWritable(true);
        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
            writer.writeNext(header);
            for (String[] line : entries)
                writer.writeNext(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
        file.setReadOnly();
    }

    void cleanDeletedFilesFromDB() {
        List<String[]> entries = readDatabase();
        File folder = new File(qWeightsFolderPath);
        File[] files = folder.listFiles();
        List<Integer> fileIds = new LinkedList<>();
        for (File file : files)
            if (file.isFile() && file.getName().matches("\\d+\\.txt"))
                fileIds.add(Integer.parseInt(file.getName().replace(".txt", "")));
        // Remove all database entries who don't have a corresponding file
        entries.removeIf(e -> !fileIds.contains(Integer.parseInt(e[db_id])));
        writeDatabase(entries);
    }

    private List<String[]> updateEntries(int id, int nGames, boolean overwrite) {
        // First, write the current q-Weights to file [id].txt
        writeQWeightsToFile(id);
        // Then, format the entry for the database, and add it to the database
        String[] entry = formatData(id, nGames);
        List<String[]> entries = readDatabase();
        if (overwrite)
            entries.removeIf(e -> Integer.parseInt(e[db_id]) == id);
        entries.add(entry);
        entries.sort((e1, e2) -> Integer.compare(Integer.parseInt(e1[db_id]), Integer.parseInt(e2[db_id])));
        return entries;
    }

    private void writeQWeightsToFile(int id) {
        String outputText = qwds.qWeightsToString();
        String writePath = qWeightsFolderPath + id + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(writePath))) {
            writer.write(outputText);
        } catch (IOException e) {
            System.out.println("An error occurred while writing beta to the file: " + e.getMessage());
        }
    }

    private int findFirstFreeId() {
        File folder = new File(qWeightsFolderPath);
        File[] files = folder.listFiles();
        List<Integer> unusedIds = IntStream.rangeClosed(1, files.length + 1).boxed()
                .collect(Collectors.toCollection(LinkedList::new));
        for (File file : files) {
            if (file.isFile() && file.getName().matches("\\d+\\.txt")) {
                int id = Integer.parseInt(file.getName().replace(".txt", ""));
                unusedIds.removeIf(n -> n == id);
            }
        }
        return unusedIds.get(0);
    }
}
