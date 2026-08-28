package games.descent2e.pcg;

import core.components.GraphBoard;
import core.components.GridBoard;
import games.descent2e.DescentGameData;
import games.descent2e.components.Monster;
import games.descent2e.concepts.Quest;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

public class GenerateBoards {

    public static final List<String> positions = List.of("N-0", "E-0", "S-0", "W-0");

    public static final String path = "data/descent2e/";

    public static List<GraphBoard> originalBoards = new ArrayList<>();
    public static List<Quest> originalQuests = new ArrayList<>();

    public static List<GridBoard> tiles = new ArrayList<>();

    public static HashMap<String, HashMap<String, Monster>> monsters;
    public static HashMap<String, HashMap<String, Monster>> lieutenants;

    public static void main(String[] args) throws IOException, InterruptedException, InvocationTargetException {

        DescentGameData data = new DescentGameData();
        data.load(path);
        originalBoards = data.getBoardConfigurations();
        originalQuests = data.getQuests();
        tiles = data.getTiles();
        monsters = data.getMonsters();
        lieutenants = data.getLieutenants();

        boolean testing = false;
        if (testing) {
            CreateOffspring co = new CreateOffspring(10,0,0,30);
            co.begin();
        }
        else {
            GenerateBoardsGUI gui = new GenerateBoardsGUI();
            gui.load();
        }
    }
}
