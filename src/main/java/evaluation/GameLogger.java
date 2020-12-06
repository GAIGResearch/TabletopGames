package evaluation;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.*;

import java.io.*;
import java.util.*;

import static core.CoreConstants.*;
import static java.util.stream.Collectors.*;

public class GameLogger implements IGameListener {

    public GameLogger(List<GameEvents> eventFilter, String logFile, boolean append) {
        this.logFile = logFile;
        this.append = append;
        this.filter = new ArrayList<>(eventFilter);
    }

    private List<IGameAttribute> gameAttributes = new ArrayList<>();
    private boolean append;
    private String logFile;
    private List<GameEvents> filter;
    private FileWriter writer;

    public void addAttribute(IGameAttribute attribute) {
        gameAttributes.add(attribute);
    }

    public void addAttributes(List<IGameAttribute> attributes) {
        gameAttributes.addAll(attributes);
    }

    public void clearAttributes() {
        gameAttributes = new ArrayList<>();
    }

    @Override
    public void onEvent(GameEvents event, AbstractGameState state, AbstractAction action) {
        if (filter.contains(event)) {
            String allData = gameAttributes.stream()
                    .map(att -> att.getAsString(state, action))
                    .collect(joining("\t"));
            ;
            writeData(allData + "\n");
        }
    }

    private void writeData(String data) {
        // first we open the file, and th
        try {
            if (writer == null) {
                boolean fileExists = new File(logFile).exists();
                writer = new FileWriter(logFile, append);
                if (!append || !fileExists) {
                    writer.write(getHeader());
                }
            }
            writer.write(data);
        } catch (IOException e) {
            e.printStackTrace();
            throw new AssertionError("Problem with file " + logFile + " : " + e.getMessage());
        }
    }

    private String getHeader() {
        String headerData = gameAttributes.stream()
                .map(IGameAttribute::name)
                .collect(joining("\t"));
        return headerData + "\n";
    }

    public void close() {
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
                writer = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new AssertionError("Problem closing file " + logFile + " : " + e.getMessage());
        }
    }
}
