package players.rl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

class DataProcessor {

    // An entry for the database. Content defined in DataProcessor::formatData
    enum DBCol {
        // The seed used
        Seed,
        // The alpha value used [0, 1]
        Alpha,
        // The gamma value used [0, 1]
        Gamma,
        // The epsilon value used [0, 1]
        Epsilon,
        // The solver used (Q-Learning, SARSA, etc.)
        Solver,
        // The class name of the used IStateHeuristic
        Heuristic,
        // The class name of the used players.rl.QWeightsDataStructure
        QWeightsDataStructure,
        // The class name of the used players.rl.RLFeatureVector
        RLFeatureVector,
        // The total number of games played by this entry
        NGamesTotal,
        // The number of games played by this entry with these parameters & settings
        NGamesWithTheseSettings,
        // All data these weights were trained on previously
        ContinuedFrom;
    }

    private QWeightsDataStructure qwds;

    private final String gameName;

    private File outfile = null;
    private ObjectNode metadata = null;

    DataProcessor(QWeightsDataStructure qwds, String gameName) {
        this.qwds = qwds;
        this.gameName = gameName;
        createMissingFoldersAndFiles();
        initFile();
        initMetadata();
    }

    private void initFile() {
        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String formattedDate = dateFormat.format(currentDate);
        outfile = new File(Paths.get(getQWeightsFolderPath(gameName), formattedDate + ".json").toString());
    }

    private void initMetadata() {
        JsonNode existingMetadata = readFileMetadata(qwds.params.getInfilePath());
        int existingNGames = existingMetadata == null ? 0 : existingMetadata.get(DBCol.NGamesTotal.name()).asInt();
        ObjectMapper om = new ObjectMapper();
        metadata = om.createObjectNode()
                .put(DBCol.Seed.name(), qwds.playerParams.getRandomSeed())
                .put(DBCol.Alpha.name(), qwds.trainingParams.alpha)
                .put(DBCol.Gamma.name(), qwds.trainingParams.gamma)
                .put(DBCol.Epsilon.name(), qwds.playerParams.epsilon)
                .put(DBCol.Solver.name(), qwds.trainingParams.solver.name())
                .put(DBCol.Heuristic.name(), qwds.trainingParams.heuristic.getClass().getCanonicalName())
                .put(DBCol.QWeightsDataStructure.name(), qwds.getClass().getCanonicalName())
                .put(DBCol.RLFeatureVector.name(), qwds.playerParams.features.getClass().getCanonicalName())
                .put(DBCol.NGamesTotal.name(), existingNGames)
                .put(DBCol.NGamesWithTheseSettings.name(), 0)
                .set(DBCol.ContinuedFrom.name(), existingMetadata);
    }

    JsonNode readFileMetadata(String pathname) {
        if (pathname == null)
            return null;
        try {
            JsonNode data = new ObjectMapper().readTree(new File(pathname));
            return data.get("Metadata");
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    void writeData(int nGames) {
        addGames(nGames);
        writeQWeightsToFile();
    }

    private void createMissingFoldersAndFiles() {
        new File(getQWeightsFolderPath(gameName)).mkdirs();
    }

    private void addGames(int nGamesToAdd) {
        for (DBCol col : Arrays.asList(DBCol.NGamesTotal, DBCol.NGamesWithTheseSettings))
            metadata.put(col.name(), metadata.get(col.name()).asInt() + nGamesToAdd);
    }

    private String getQWeightsFolderPath(String gameName) {
        return Paths.get(RLPlayer.resourcesPath, QWeightsDataStructure.qWeightsFolderName, gameName).toString();
    }

    private JsonNode createJsonNode(ObjectMapper objectMapper) {
        Map<String, Double> stateMap = qwds.qWeightsToStateMap();
        JsonNode weightsData = objectMapper.convertValue(stateMap, ObjectNode.class);
        ObjectNode outNode = objectMapper.createObjectNode();
        outNode.set("Metadata", metadata);
        outNode.set("Weights", weightsData);
        return outNode;
    }

    private void writeQWeightsToFile() {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = createJsonNode(objectMapper);

        outfile.setWritable(true);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outfile))) {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
            writer.write(json);
            outfile.setReadOnly();
        } catch (IOException e) {
            System.out.println("An error occurred while writing beta to the file: " +
                    e.getMessage());
            System.exit(1);
        }
    }
}
