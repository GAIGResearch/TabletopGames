package players.rl;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import players.rl.DataProcessor.DBCol;
import players.rl.DataProcessor.DBEntry;

class DatabaseJanitor {

    private static int getMetadataID(String filePath) {
        try {
            JsonNode data = new ObjectMapper().readTree(new File(filePath));
            return data.get("Metadata").get("ID").asInt();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return -1;
    }

    private static File getQWeightsFolder(File gameFolder) {
        File[] _qWeightsFolder = gameFolder.listFiles(f -> f.isDirectory() &&
                f.getName().equals("qWeights"));
        return _qWeightsFolder.length == 0 ? null : _qWeightsFolder[0];
    }

    private static List<Integer> renameFilesAndGetIDs(File qWeightsFolder) throws IOException {
        List<Integer> fileIds = new LinkedList<>(); // A list of all file IDs of this gameFolder
        for (File file : qWeightsFolder.listFiles(f -> f.isFile())) {
            int fileId = getMetadataID(file.getAbsolutePath());
            Matcher matcher = Pattern.compile(DataProcessor.allowedFilenameRegex).matcher(file.getName());
            if (matcher.matches()) {
                // Check whether the ID matches the ID in the metadata
                int fileNameID = Integer.parseInt(matcher.group(1));
                if (fileNameID != fileId)
                    addLeadingID(file, fileId);
            } else
                addLeadingID(file, fileId);

            if (fileIds.contains(fileId))
                throw new IllegalArgumentException(
                        "Found multiple entries with ID " + fileId + " in "
                                + qWeightsFolder.getParentFile().getCanonicalPath()
                                + ". Resolve manually.");
            fileIds.add(fileId);
        }

        return fileIds;
    }

    private static void addLeadingID(File file, int id) {
        try {
            File renamedFile = new File(file.getParent(), id + "_" + file.getName());
            String rename = "file: \"" + file.getCanonicalPath() + "\" -> \"" + renamedFile.getCanonicalPath() + "\"";
            if (file.renameTo(renamedFile))
                System.out.println("Renamed " + rename);
            else
                throw new IOException("ERROR: Failed to rename" + rename + ". Consider manually renaming the file.");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void cleanupAllGames() {
        File resourcesFolder = new File(RLPlayer.resourcesPath);
        if (!resourcesFolder.exists() || !resourcesFolder.isDirectory())
            return;

        for (File gameFolder : resourcesFolder.listFiles(f -> f.isDirectory()))
            cleanupGame(gameFolder.getName());
    }

    private static void cleanupGame(String gameName) {
        try {
            File gameFolder = new File(RLPlayer.resourcesPath + gameName);
            if (!gameFolder.exists() || !gameFolder.isDirectory())
                throw new IllegalArgumentException("Error: Folder does not exist: " + gameFolder.getCanonicalPath());
            File qWeightsFolder = getQWeightsFolder(gameFolder);
            if (qWeightsFolder == null)
                return;
            List<Integer> fileIds = renameFilesAndGetIDs(qWeightsFolder);
            cleanDeleted(gameFolder.getName(), fileIds);
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void cleanDeleted(String gameName, List<Integer> fileIds) {
        List<DBEntry> entries = DataProcessor.readDatabase(gameName);
        // Remove all database entries who don't have a corresponding file
        entries.removeIf(e -> !fileIds.contains((int) e.get(DBCol.ID)));
        // Remove all references to these entries in the StartID column (keep
        // StartNGames)
        entries.forEach(e -> {
            String startId = (String) e.get(DBCol.StartID);
            try {
                if (!fileIds.contains(Integer.parseInt(startId)))
                    e.put(DBCol.StartID, "DEL");
            } catch (NumberFormatException exc) {
                // StartId can sometimes be '-' or 'DEL', thats fine.
                // If it's anything else, something went wrong
                if (!startId.equals("-") && !startId.equals("DEL"))
                    exc.printStackTrace();
            }
        });
        DataProcessor.writeDatabase(entries, gameName);
    }

    public static void main(String[] args) {
        cleanupAllGames();
    }
}
