package evaluation;

import core.AbstractGameState;
import core.interfaces.IGameAttribute;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class GameLogger {

    public GameLogger(String logFile, boolean append) {
        this.logFile = logFile;
        this.append = append;
    }

    private List<IGameAttribute<?>> gameAttributes = new ArrayList<>();
    private boolean append;
    private String logFile;

    public void addAttribute(IGameAttribute<?> attribute) {
        gameAttributes.add(attribute);
    }
    public void addAttributes(List<IGameAttribute<?>> attributes) {
        gameAttributes.addAll(attributes);
    }
    public void clearAttributes() {
        gameAttributes = new ArrayList<>();
    }

    public void processState(AbstractGameState state) {
        String allData = gameAttributes.stream()
                .map(att -> att.getAsString(state))
                .collect(Collectors.joining("\t"));;
        writeData(allData + "\n");
    }

    private void writeData(String data) {
        // first we open the file, and th
        try {
            boolean fileExists = new File(logFile).exists();
            FileWriter writer = new FileWriter(logFile, append);
            if (!fileExists) {
                writer.write(getHeader());
            }
            writer.write(data);
            writer.flush();
            writer.close();
        }catch (IOException e) {
            e.printStackTrace();
            throw new AssertionError("Problem with file " + logFile + " : " + e.getMessage());
        }
    }

    private String getHeader() {
        String headerData = gameAttributes.stream()
                .map(IGameAttribute::name)
                .collect(Collectors.joining("\t"));
        return headerData + "\n";
    }

}
