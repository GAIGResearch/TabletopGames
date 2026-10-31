package evaluation.jsonBag;

import core.AbstractPlayer;
import core.Game;
import evaluation.summarisers.TAGNumericStatSummary;
import games.GameType;
import org.jetbrains.annotations.NotNull;
import players.jsonBagPlayers.JSONBagOSLAPlayer;
import players.jsonBagPlayers.Tokenizer;
import players.simple.OSLAPlayer;
import players.simple.SimultaneousOSLAPlayer;
import utilities.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class JSONBagValueFunctionExperiments {

    public static void main(String[] args) {
        String gameName = Utils.getArg(args, "game", GameType.Dominion.name());
        int n = Utils.getArg(args, "n", 100);
        int nPlayers = Utils.getArg(args, "nPlayers", 4);
        int topKFeatures = Utils.getArg(args, "topK", 500);
        boolean includeNone = Utils.getArg(args, "includeNone", false);
        String defaultExperimentRootPath = "Experiments/valueFuncTest2/" + gameName;
        String experimentRootPath = Utils.getArg(args, "experimentRootPath", defaultExperimentRootPath);

        GameType gameTypeTest;
        try {
            gameTypeTest = GameType.valueOf(gameName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid GameType: " + gameName, e);
        }

        if (n <= 0) {
            throw new IllegalArgumentException("n must be > 0, but was " + n);
        }
        if (nPlayers < 2) {
            throw new IllegalArgumentException("nPlayers must be at least 2");
        }

        System.out.println("Program parameters: gameName=" + gameName
            + ", n=" + n
            + ", nPlayers=" + nPlayers
            + ", topKFeatures=" + topKFeatures
            + ", includeNone=" + includeNone
            + ", experimentRootPath=" + experimentRootPath);

        int nPrototypes = nPlayers;

        String csvOutputPath = getOutputCSVPath(experimentRootPath);

        initialiseCsv(csvOutputPath);

        List<String> folderNames = getExperimentFolders(experimentRootPath);
        if (folderNames.isEmpty()) {
            throw new IllegalStateException("No experiment folders found in " + experimentRootPath);
        }

        long globalStart = System.currentTimeMillis();
        int experimentCount = 0;

        for (String prototypesFolderName : folderNames) {
            if (prototypesFolderName.contains("singleFS")) {
                System.out.println("SKIPPING: prototype folder contains 'singleFS' | folder=" + prototypesFolderName);
                continue;
            }

            List<String> topFeaturesFolderCandidates = new ArrayList<>(folderNames);
            if (includeNone) {
                topFeaturesFolderCandidates.add("NONE");
            }

            for (String topFeaturesFolderName : topFeaturesFolderCandidates) {
                String prototypesPath = experimentRootPath + "/" + prototypesFolderName;
                List<Map<String, Integer>> fullPrototypes = Tokenizer.loadPrototypes(prototypesPath, nPrototypes, true);

                List<String> topFeaturesFilterList;
                if ("NONE".equals(topFeaturesFolderName)) {
                    topFeaturesFilterList = Collections.emptyList();
                } else {
                    String topFeaturesFolderPath = experimentRootPath + "/" + topFeaturesFolderName;
                    topFeaturesFilterList = loadTopFeatures(topFeaturesFolderPath, topKFeatures);
                    if (topFeaturesFilterList.isEmpty()) {
                        System.out.println("SKIPPING: no top-features found | folder=" + topFeaturesFolderName);
                        continue;
                    }

                    Set<String> topFeaturesFilterSet = new HashSet<>(topFeaturesFilterList);
                    for (Map<String, Integer> prototype : fullPrototypes) {
                        Tokenizer.filter(prototype, topFeaturesFilterSet, true);
                    }
                }

                for (int jsonBagOSLAPlayerIndex = 0; jsonBagOSLAPlayerIndex < nPlayers; jsonBagOSLAPlayerIndex++) {
                    ArrayList<AbstractPlayer> players = new ArrayList<>();
                    if (gameTypeTest != GameType.Wonders7) {
                        for (int i = 0; i < nPlayers - 1; i++) {
                            players.add(new OSLAPlayer());
                        }
                    }
                    else {
                        for (int i = 0; i < nPlayers - 1; i++) {
                            players.add(new SimultaneousOSLAPlayer());
                        }
                    }

                    JSONBagOSLAPlayer jsonBagOSLAPlayer = new JSONBagOSLAPlayer(fullPrototypes, topFeaturesFilterList);
                    if (gameTypeTest == GameType.Wonders7) {
                        jsonBagOSLAPlayer.setSimultaneousMode(true);
                    }
                    players.add(jsonBagOSLAPlayerIndex, jsonBagOSLAPlayer);

                    experimentCount++;
                    System.out.println("EXPERIMENT " + experimentCount
                            + " | prototypeFolder=" + prototypesFolderName
                            + " | topFeaturesFolder=" + topFeaturesFolderName
                            + " | fullPrototypes=true"
                            + " | jsonBagOSLAPlayerIndex=" + jsonBagOSLAPlayerIndex);

                    long[] seeds = new long[n];
                    Random rnd = new Random();
                    for (int i = 0; i < n; i++) {
                        seeds[i] = rnd.nextInt();
                    }
                    long experimentStart = System.currentTimeMillis();
                    TAGNumericStatSummary[] stats = Game.runMany(Collections.singletonList(gameTypeTest), players, n, seeds, null, false, null, 0);
                    double experimentDurationSeconds = (System.currentTimeMillis() - experimentStart) / 1000.0;
                    appendCsvRows(csvOutputPath, gameTypeTest, prototypesFolderName, topFeaturesFolderName, topKFeatures, jsonBagOSLAPlayerIndex, stats, experimentDurationSeconds);
                }
            }
        }

        long totalDuration = System.currentTimeMillis() - globalStart;;
        System.out.println("FINISHED " + experimentCount + " EXPERIMENTS IN " + totalDuration / 1000 + " SECONDS");
        System.out.println("Average experiment duration: " + (totalDuration/1000)/experimentCount + " SECONDS");
        System.out.println("CSV WRITTEN TO " + csvOutputPath);
    }

    @NotNull
    private static String getOutputCSVPath(String experimentRootPath) {
        String csvName = "jsonbag_pair_statistics.csv";
        String resultsDir = experimentRootPath + "/results";

        File resultsDirFile = new File(resultsDir);
        if (!resultsDirFile.exists()) {
            if (!resultsDirFile.mkdirs()) {
                throw new RuntimeException("Failed to create results directory: " + resultsDir);
            }
        }

        String baseName = csvName;
        String extension = "";
        int dotIndex = csvName.lastIndexOf('.');
        if (dotIndex >= 0) {
            baseName = csvName.substring(0, dotIndex);
            extension = csvName.substring(dotIndex);
        }

        File csvFile = new File(resultsDir, csvName);
        int index = 1;
        while (csvFile.exists()) {
            String candidateName = baseName + "_" + index + extension;
            csvFile = new File(resultsDir, candidateName);
            index++;
        }
        return csvFile.getPath();
    }

    private static List<String> getExperimentFolders(String experimentRootPath) {
        File root = new File(experimentRootPath);
        File[] folders = root.listFiles(File::isDirectory);
        if (folders == null) return Collections.emptyList();

        List<String> names = new ArrayList<>();
        for (File folder : folders) {
            if (!"results".equals(folder.getName())) {
                names.add(folder.getName());
            }
        }
        Collections.sort(names);
        return names;
    }

    private static List<String> loadTopFeatures(String topFeaturesFolderPath, int topKFeatures) {
        String topFeaturesFilename = "features-top" + topKFeatures + ".json";
        String topFeaturesPath = topFeaturesFolderPath + "/" + topFeaturesFilename;

        File topFeaturesFile = new File(topFeaturesPath);
        if (!topFeaturesFile.exists()) {
            return Collections.emptyList();
        }
        return Tokenizer.loadStringList(topFeaturesPath);
    }

    private static void initialiseCsv(String csvOutputPath) {
        String header = "game,prototypeFolder,topFeaturesFolder,topKFeatures,jsonBagOSLAPlayerIndex,n,mean,sd,min,max,experimentDurationSeconds\n";
        try {
            Files.writeString(Path.of(csvOutputPath), header);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialise CSV at " + csvOutputPath, e);
        }
    }

    private static void appendCsvRows(String csvOutputPath,
                                      GameType gameType,
                                      String prototypesFolderName,
                                      String topFeaturesFolderName,
                                          int topKFeatures,
                          int jsonBagOSLAPlayerIndex,
                          TAGNumericStatSummary[] stats,
                          double experimentDurationSeconds) {
        StringBuilder sb = new StringBuilder();
        TAGNumericStatSummary s = stats[jsonBagOSLAPlayerIndex];
        sb.append(gameType.name()).append(',')
                .append(prototypesFolderName).append(',')
                .append(topFeaturesFolderName).append(',')
                            .append(topKFeatures).append(',')
            .append(jsonBagOSLAPlayerIndex).append(',')
                .append(s.n()).append(',')
                .append(s.mean()).append(',')
                .append(s.sd()).append(',')
                .append(s.min()).append(',')
                .append(s.max()).append(',')
                .append(experimentDurationSeconds)
                .append('\n');

        try {
            Files.writeString(Path.of(csvOutputPath), sb.toString(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Failed to append CSV rows to " + csvOutputPath, e);
        }
    }
}