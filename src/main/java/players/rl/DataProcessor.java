package players.rl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class DataProcessor {

    // Alias used for readability and clarity
    static class DBEntry extends HashMap<DBCol, Object> {
    }

    // An entry for the database. Content defined in DataProcessor::formatData
    enum DBCol {
        // The ID of this entry (Z+)
        ID(Integer.class),
        // The full filename of this entry
        // TODO: Filename(String.class),
        // The alpha value used [0, 1]
        Alpha(Float.class),
        // The gamma value used [0, 1]
        Gamma(Float.class),
        // The epsilon value used [0, 1]
        Epsilon(Float.class),
        // The solver used (Q-Learning, SARSA, etc.)
        Solver(String.class),
        // The class name of the used IStateHeuristic
        Heuristic(String.class),
        // The class name of the used players.rl.QWeightsDataStructure
        QWeightsDataStructure(String.class),
        // The class name of the used players.rl.RLFeatureVector
        RLFeatureVector(String.class),
        // The total number of games played by this entry
        NGamesTotal(Integer.class),
        // The number of games played by this entry with these parameters & settings
        NGamesThisId(Integer.class),
        // The ID of the entry which was used as a starting point for this training
        StartID(Integer.class),
        // The filename of the entry used as a starting point for this training
        // TODO: StartFilename(String.class),
        // The total number of games played before using these parameters & settings
        StartNGames(Integer.class);

        final Class<?> type;

        DBCol(Class<?> type) {
            this.type = type;
        }

        Object parse(String value) {
            // Special cases for some entries that are normally numbers
            if (Arrays.asList("-", "DEL").contains(value))
                return value;
            // Parse according to type
            if (type.isAssignableFrom(Integer.class))
                return Integer.parseInt(value);
            if (type.isAssignableFrom(Float.class))
                return Float.parseFloat(value);
            if (type.isAssignableFrom(Double.class))
                return Double.parseDouble(value);
            return value;
        }

        void addToObjectNode(ObjectNode objectNode, Object value) {
            if (value instanceof Integer)
                objectNode.put(name(), (Integer) value);
            else if (value instanceof Float)
                objectNode.put(name(), (Float) value);
            else if (value instanceof Double)
                objectNode.put(name(), (Double) value);
            else
                objectNode.put(name(), (String) value);

        }

    }

    private static final String[] header = Arrays.stream(DBCol.values()).map(Enum::name).toArray(String[]::new);

    private final int db_id = DBCol.ID.ordinal();

    private QWeightsDataStructure qwds;

    private static final String qWeightsFolderName = "qWeights";
    private static final String dbFileName = "qWeightsDB.csv";

    // Matches [number].json or [number]_[anything].json, captures the number. E.g.:
    // 1.json; 23.json; 4_text0.json; 5_text_0.json; 6_.json, 7__text.json, 8_0.json
    static final String allowedFilenameRegex = "^(\\d+)(?:_\\w*)?\\.json$";

    private final String gameName;

    DataProcessor(QWeightsDataStructure qwds, String gameName) {
        this.qwds = qwds;
        this.gameName = gameName;
        createMissingFoldersAndFiles();
    }

    int writeData(int nGames) {
        int id = findFirstFreeId();
        writeDatabase(updateEntries(id, nGames, false), gameName);
        return id;
    }

    void writeData(int id, int nGames) {
        writeDatabase(updateEntries(id, nGames, true), gameName);
    }

    private void createMissingFoldersAndFiles() {
        new File(getQWeightsFolderPath(gameName)).mkdirs();
        initDatabase();
    }

    private DBEntry formatData(int id, int nGamesAdded) {
        int startId = qwds.params.qWeightsFileId;

        Integer _nGamesThisId = (Integer) readDatabaseEntry(id, DBCol.NGamesThisId);
        int nGamesThisId = (_nGamesThisId != null ? _nGamesThisId : 0) + nGamesAdded;

        Integer _startNGames = (Integer) readDatabaseEntry(startId, DBCol.NGamesTotal);
        int startNGames = _startNGames != null ? _startNGames : 0;

        return new DBEntry() {
            {
                put(DBCol.ID, id);
                put(DBCol.Alpha, qwds.trainingParams.alpha);
                put(DBCol.Gamma, qwds.trainingParams.gamma);
                put(DBCol.Epsilon, qwds.params.epsilon);
                put(DBCol.Solver, qwds.trainingParams.solver.name());
                put(DBCol.Heuristic, qwds.trainingParams.heuristic.getClass().getCanonicalName());
                put(DBCol.QWeightsDataStructure, qwds.getClass().getCanonicalName());
                put(DBCol.RLFeatureVector, qwds.params.features.getClass().getCanonicalName());
                put(DBCol.NGamesTotal, startNGames + nGamesThisId);
                put(DBCol.NGamesThisId, nGamesThisId);
                put(DBCol.StartID, startId == 0 ? "-" : startId);
                put(DBCol.StartNGames, startId == 0 ? "-" : startNGames);
            }
        };
    }

    private static String getQWeightsFolderPath(String gameName) {
        return RLPlayer.resourcesPath + gameName + "/" + qWeightsFolderName + "/";
    }

    private static String getDBPath(String gameName) {
        return RLPlayer.resourcesPath + gameName + "/" + dbFileName + "/";
    }

    private Object readDatabaseEntry(int id, DBCol col) {
        DBEntry entry = readDatabaseEntry(id);
        if (entry != null)
            return entry.get(col);
        return null;
    }

    private DBEntry readDatabaseEntry(int id) {
        try (CSVReader reader = new CSVReader(new FileReader(getDBPath(gameName)))) {
            String[] nextLine;
            reader.skip(1); // Header
            while ((nextLine = reader.readNext()) != null)
                if (Integer.parseInt(nextLine[db_id]) == id)
                    return strArray2DBEntry(nextLine);
            reader.close();
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static DBEntry strArray2DBEntry(String[] entry) {
        return new DBEntry() {
            {
                for (DBCol dbE : DBCol.values())
                    put(dbE, dbE.parse(entry[dbE.ordinal()]));
            }
        };
    }

    static List<DBEntry> readDatabase(String gameName) {
        try (CSVReader reader = new CSVReader(new FileReader(getDBPath(gameName)))) {
            List<String[]> db = reader.readAll().stream().collect(Collectors.toCollection(LinkedList::new));
            db.remove(0); // Remove header
            // Convert each entry from String[] to Map<DBEntry, String>
            return db.stream().map(e -> strArray2DBEntry(e)).collect(Collectors.toCollection(LinkedList::new));
        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void initDatabase() {
        File file = new File(getDBPath(gameName));
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

    static void writeDatabase(List<DBEntry> entries, String gameName) {
        File file = new File(getDBPath(gameName));
        file.setWritable(true);
        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
            writer.writeNext(header);
            for (DBEntry line : entries)
                writer.writeNext(
                        Arrays.stream(DBCol.values()).map(col -> line.getOrDefault(col, "").toString())
                                .toArray(String[]::new));
        } catch (IOException e) {
            e.printStackTrace();
        }
        file.setReadOnly();
    }

    private List<DBEntry> updateEntries(int id, int nGames, boolean overwrite) {
        DBEntry entry = formatData(id, nGames);
        // First, write the current q-Weights to file [id].txt
        writeQWeightsToFile(entry);
        // Then, format the entry for the database, and add it to the database
        List<DBEntry> entries = readDatabase(gameName);
        if (overwrite)
            entries.removeIf(e -> (int) e.get(DBCol.ID) == id);
        entries.add(entry);
        entries.sort((e1, e2) -> Integer.compare((int) e1.get(DBCol.ID), (int) e2.get(DBCol.ID)));
        return entries;
    }

    private ObjectNode createQWeightsObjectNode(ObjectMapper objectMapper, DBEntry entry) {
        ObjectNode entryMetadata = objectMapper.createObjectNode();
        for (DBCol col : DBCol.values())
            col.addToObjectNode(entryMetadata, entry.get(col));

        Map<String, Double> stateMap = qwds.qWeightsToStateMap();
        JsonNode weightsData = objectMapper.convertValue(stateMap, ObjectNode.class);

        ObjectNode node = objectMapper.createObjectNode();
        node.set("Metadata", entryMetadata);
        node.set("Weights", weightsData);

        return node;
    }

    private void writeQWeightsToFile(DBEntry entry) {
        String writePath = getQWeightsFolderPath(gameName) + entry.get(DBCol.ID) + ".json";

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = createQWeightsObjectNode(objectMapper, entry);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(writePath))) {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
            writer.write(json);
        } catch (IOException e) {
            System.out.println("An error occurred while writing beta to the file: " +
                    e.getMessage());
            System.exit(1);
        }
    }

    private int findFirstFreeId() {
        File folder = new File(getQWeightsFolderPath(gameName));
        File[] files = folder.listFiles();
        List<Integer> unusedIds = IntStream.rangeClosed(1, files.length + 1).boxed()
                .collect(Collectors.toCollection(LinkedList::new));
        for (File file : files) {
            Matcher matcher = Pattern.compile(DataProcessor.allowedFilenameRegex).matcher(file.getName());
            if (file.isFile() && matcher.matches()) {
                int id = Integer.parseInt(matcher.group(1));
                unusedIds.removeIf(n -> n == id);
            }
        }
        return unusedIds.get(0);
    }
}
