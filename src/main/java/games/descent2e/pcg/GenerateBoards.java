package games.descent2e.pcg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.crypto.tink.subtle.Random;
import core.components.BoardNode;
import core.components.GraphBoard;
import core.components.GridBoard;
import core.properties.Property;
import core.properties.PropertyInt;
import core.properties.PropertyStringArray;
import games.descent2e.DescentGameData;
import games.descent2e.components.Monster;
import games.descent2e.concepts.Quest;
import org.apache.hadoop.shaded.com.nimbusds.jose.shaded.json.JSONObject;
import org.apache.hadoop.shaded.com.nimbusds.jose.shaded.json.JSONStyle;
import utilities.Pair;
import utilities.Vector2D;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.List;

import static core.CoreConstants.nodeHash;
import static games.descent2e.pcg.ControlVariables.*;
import static games.descent2e.pcg.FitnessFunction.*;

public class GenerateBoards {

    public static final List<String> positions = List.of("N-0", "E-0", "S-0", "W-0");

    public static final String path = "data/descent2e/";

    public static List<GraphBoard> originalBoards = new ArrayList<>();
    public static List<Quest> originalQuests = new ArrayList<>();

    public static List<GridBoard> tiles = new ArrayList<>();

    public static HashMap<String, HashMap<String, Monster>> monsters;
    public static HashMap<String, HashMap<String, Monster>> lieutenants;

    static final JFrame mainWindow = new JFrame("Descent (Second Edition) Procedurally Generated Board Creator");
    static JPanel mainPanel;

    public static void main(String[] args) throws IOException {

        DescentGameData data = new DescentGameData();
        data.load(path);
        originalBoards = data.getBoardConfigurations();
        originalQuests = data.getQuests();
        tiles = data.getTiles();
        monsters = data.getMonsters();
        lieutenants = data.getLieutenants();

        mainWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainWindow.setSize(1200, 500);
        mainWindow.setResizable(false);
        mainWindow.setLocationRelativeTo(null);
        mainWindow.setLayout(new GridLayout(0, 1,10, 10));

        mainPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        mainPanel.setBackground(Color.CYAN);

        mainWindow.add(mainPanel);

        boolean testing = true;

        if (testing) {
            CreateOffspring co = new CreateOffspring();
            co.begin();
        }
        else {
            Button create = makeButton("Generate!");
            mainPanel.add(create);
            create.setEnabled(true);

            create.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(create.isEnabled()) {
                        CreateOffspring co = new CreateOffspring();
                        co.begin();
                        create.setEnabled(false);
                    }
                }
            });

            mainWindow.setVisible(true);
        }

    }

    public static Button makeButton(String label) {
        Button button = new Button(label);
        button.setFocusable(false);
        button.setFont(new Font("Arial", Font.PLAIN, 10));
        return button;
    }
}
