package players.rl;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import players.rl.dataStructures.QWeightsDataStructure;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DataProcessor {

    private enum Db {
        id, alpha, gamma, epsilon, solver, nGames, startId, startNGames
    }

    private final String resourcesPath = "src/main/java/players/rl/resources/";

    private final String dbFileName = "qWeightsDB.csv";

    private final String[] header = Arrays.stream(Db.values()).map(Enum::name).toArray(String[]::new);

    private QWeightsDataStructure qwds;

    private String dbPath;
    private String gameFolderPath;
    private String qWeightsFolderPath;

    public DataProcessor(QWeightsDataStructure qwds, String gameName) {
        this.qwds = qwds;
        this.gameFolderPath = resourcesPath + gameName + "/";
        this.dbPath = gameFolderPath + dbFileName;
        this.qWeightsFolderPath = gameFolderPath + "qWeights/";
        createMissingFoldersAndFiles();
    }

    private void createMissingFoldersAndFiles() {
        new File(qWeightsFolderPath).mkdirs();
        initDatabase();
    }

    private void writeData(int id) {
        String[] dbData = formatData(id);
        writeQWeightsToFile(id);
        writeDatabase(dbData);
    }

    public void writeData() {
        writeData(findFirstFreeId());
    }

    public void overwriteData() {
        writeData(qwds.params.qWeightsFileId);
    }

    private String[] formatData(int id) {
        int startId = qwds.params.qWeightsFileId;
        String _startNGames = getEntryFromDatabase(startId, Db.nGames);
        final String startNGames = _startNGames != null ? _startNGames : "0";
        Map<Db, String> entries = new HashMap<Db, String>() {
            {
                put(Db.id, Integer.toString(id));
                put(Db.alpha, Double.toString(qwds.trainingParams.alpha));
                put(Db.gamma, Double.toString(qwds.trainingParams.gamma));
                put(Db.epsilon, Double.toString(qwds.params.epsilon));
                put(Db.solver, qwds.trainingParams.solver.name());
                put(Db.nGames, Integer.toString(qwds.trainingParams.nGames));
                put(Db.startId, Integer.toString(startId));
                put(Db.startNGames, startNGames);
            }
        };
        return Arrays.stream(Db.values()).map(col -> entries.getOrDefault(col, "")).toArray(String[]::new);
    }

    private String getEntryFromDatabase(int id, Db col) {
        String[] entry = getEntryFromDatabase(id);
        if (entry != null)
            return entry[col.ordinal()];
        return null;
    }

    private String[] getEntryFromDatabase(int id) {
        try (CSVReader reader = new CSVReader(new FileReader(dbPath))) {
            String[] nextLine;
            reader.skip(1); // Header
            while ((nextLine = reader.readNext()) != null)
                if (Integer.parseInt(nextLine[Db.id.ordinal()]) == id)
                    return nextLine;
        } catch (IOException | CsvValidationException e) {
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

    private void writeDatabase(String[] entry) {
        File file = new File(dbPath);
        file.setWritable(true);
        try (CSVWriter writer = new CSVWriter(new FileWriter(file, true))) {
            writer.writeNext(entry);
        } catch (IOException e) {
            e.printStackTrace();
        }
        file.setReadOnly();
    }

    public void tryReadQWeightsFromFile() {
        String readPath = qWeightsFolderPath + qwds.params.qWeightsFileId + ".txt";
        Path path = Paths.get(readPath);
        if (Files.exists(path) && Files.isRegularFile(path)) {
            try {
                String[] weightStrings = Files.readString(path).split("\n");
                qwds.parseQWeights(weightStrings);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
