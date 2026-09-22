package games.descent2e.pcg;

import core.components.GraphBoard;
import core.components.GridBoard;
import core.properties.Property;
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

    public static List<PCGBoard> startingBoards = new ArrayList<>();

    public static HashMap<String, HashMap<String, Monster>> monsters;
    public static HashMap<String, HashMap<String, Monster>> lieutenants;

    static int generationID = 1;

    public static void main(String[] args) throws IOException, InterruptedException, InvocationTargetException {

        DescentGameData data = new DescentGameData();
        data.load(path);
        originalBoards = data.getBoardConfigurations();
        originalQuests = data.getQuests();
        tiles = data.getTiles();
        monsters = data.getMonsters();
        lieutenants = data.getLieutenants();

        for (int i = 0; i < CreateOffspring.endcapLimit; i++) {
            String id = "-" + (i+1);
            GridBoard tile = getTileByName("endcap1A");
            assert tile != null;
            GridBoard copy = tile.copyNewID();
            copy.getProperties().clear();
            for (int prop_key : tile.getProperties().keySet()) {
                Property newProp = tile.getProperties().get(prop_key).copy();
                copy.getProperties().put(prop_key, newProp);
            }
            copy.setComponentName(copy.getComponentName() + id);
            tiles.add(copy);
            tile = getTileByName("endcap1B");
            assert tile != null;
            copy = tile.copyNewID();
            for (int prop_key : tile.getProperties().keySet()) {
                Property newProp = tile.getProperties().get(prop_key).copy();
                copy.getProperties().put(prop_key, newProp);
            }
            copy.setComponentName(copy.getComponentName() + id);
            tiles.add(copy);
        }
        for (int i = 0; i < CreateOffspring.extenderLimit; i++) {
            String id = "-" + (i+1);
            GridBoard tile = getTileByName("extender1A");
            assert tile != null;
            GridBoard copy = tile.copyNewID();
            copy.getProperties().clear();
            for (int prop_key : tile.getProperties().keySet()) {
                Property newProp = tile.getProperties().get(prop_key).copy();
                copy.getProperties().put(prop_key, newProp);
            }
            copy.setComponentName(copy.getComponentName() + id);
            tiles.add(copy);
            tile = getTileByName("extender1B");
            assert tile != null;
            copy = tile.copyNewID();
            for (int prop_key : tile.getProperties().keySet()) {
                Property newProp = tile.getProperties().get(prop_key).copy();
                copy.getProperties().put(prop_key, newProp);
            }
            copy.setComponentName(copy.getComponentName() + id);
            tiles.add(copy);
        }
        for (int i = 0; i < CreateOffspring.transitionLimit; i++) {
            String id = "-" + (i+1);
            GridBoard tile = getTileByName("transition");
            assert tile != null;
            GridBoard copy = tile.copyNewID();
            copy.getProperties().clear();
            for (int prop_key : tile.getProperties().keySet()) {
                Property newProp = tile.getProperties().get(prop_key).copy();
                copy.getProperties().put(prop_key, newProp);
            }
            copy.setComponentName(copy.getComponentName() + id);
            tiles.add(copy);
        }

        for (int i = 0; i < originalQuests.size(); i++) {
            PCGBoard board = new PCGBoard(originalQuests.get(i), originalBoards.get(i));
            startingBoards.add(board);
        }

        boolean testing = true;
        if (testing) {
            CreateOffspring co = new CreateOffspring(500,200,10,30);
            co.begin(generationID);
        }
        else {
            GenerateBoardsGUI gui = new GenerateBoardsGUI(generationID);
            gui.load();
        }
    }

    public static GridBoard getTileByName(String name) {
        for (GridBoard tile : tiles) {
            if (tile.getComponentName().equals(name)) {
                GridBoard copy = tile.copy();
                copy.getProperties().clear();
                for (int prop_key : tile.getProperties().keySet()) {
                    Property newProp = tile.getProperties().get(prop_key).copy();
                    copy.getProperties().put(prop_key, newProp);
                }
                return copy;
            }
        }
        return null;
    }
}
