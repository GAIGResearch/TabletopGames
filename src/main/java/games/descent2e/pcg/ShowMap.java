package games.descent2e.pcg;

import com.fasterxml.jackson.databind.ObjectMapper;
import core.properties.PropertyStringArray;
import games.descent2e.gui.DescentGridBoardView;
import org.apache.commons.io.FileUtils;
import org.jdesktop.swingx.border.DropShadowBorder;
import org.json.simple.parser.ParseException;
import utilities.Pair;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;

public class ShowMap {
    private PCGBoard quest;
    private List<PCGNode> board;

    private ShowMAPElite parentElite;
    private ShowFeasibleBoards parentList;

    private final JFrame window;

    private final int maxWidth = 1400;
    private final int maxHeight = 800;
    private final int shadowSize = 10;

    private final String dataPath = "data/descent2e/img/";

    public ShowMap(CreateOffspring co, PCGBoard quest, int id, HashMap<String, Float> scores, ShowMAPElite parentElite, ShowFeasibleBoards parentList) throws IOException {

        this.parentElite = parentElite;
        this.parentList = parentList;
        this.quest = quest;
        this.board = quest.board;

        window = new JFrame(quest.name);
        window.setSize(maxWidth, maxHeight);
        window.setResizable(false);
        window.setLocationRelativeTo(null);
        window.setBackground(Color.BLACK);

        window.setLayout(new BorderLayout());

        Border blackline = BorderFactory.createLineBorder(Color.black);

        JPanel top = new JPanel();
        top.setBackground(Color.CYAN);
        top.setBorder(blackline);
        top.setLayout(new FlowLayout());
        JLabel nameLabel = new JLabel(quest.name);
        top.add(nameLabel);
        window.add(top, BorderLayout.PAGE_START);

        JPanel left = new JPanel();
        left.setLayout(new FlowLayout(FlowLayout.CENTER));
        left.setBackground(Color.CYAN);
        left.setPreferredSize(new Dimension(300, 700));

        JPanel fitness = new JPanel();
        fitness.setLayout(new BoxLayout(fitness, BoxLayout.Y_AXIS));
        fitness.setBackground(Color.WHITE);
        TitledBorder fitnessBorder = new TitledBorder(blackline, "Fitness");
        fitnessBorder.setTitleJustification(TitledBorder.CENTER);
        fitness.setBorder(fitnessBorder);
        fitness.setLayout(new BoxLayout(fitness, BoxLayout.Y_AXIS));
        fitness.setPreferredSize(new Dimension(300, 390));
        JLabel fitnessLabel = new JLabel("Score: " + scores.get("Fitness"));

        float size = scores.get("Size");
        float height = scores.get("Height");
        float width = scores.get("Width");
        JLabel boardSize = new JLabel((int) size + " Spaces (" + (int) height + "x" + (int) width + ")");
        fitness.add(fitnessLabel);
        fitness.add(boardSize);

        float totalWeightsModifier = 10f / (5f + co.W_SIZE + co.W_GROUPS + co.W_HEALTH + co.W_HEIGHT + co.W_WIDTH);
        float connected = scores.get("Connectedness");
        JLabel connectedLabel = new JLabel(connected == 1f ? "Fully Connected Layout" : "Disconnected Layout");
        JLabel connectedWeight = new JLabel(Float.toString(connected * totalWeightsModifier));
        float geometry = scores.get("Geometry");
        JLabel geometryLabel = new JLabel(geometry == 1f ? "Valid Geometry" : "Invalid Geometry");
        JLabel geometryWeight = new JLabel(Float.toString(connected * totalWeightsModifier));
        float repeats = scores.get("Repeats");
        JLabel repeatsLabel = new JLabel(repeats == 1f ? "No Repeating Monster Groups" : "Repeating Monster Groups");
        JLabel repeatsWeight = new JLabel(Float.toString(repeats * totalWeightsModifier));
        float spawning = scores.get("Spawning");
        JLabel spawningLabel = new JLabel(spawning == 1f ? "All Legal Spawning Positions" : "Illegal Spawning Positions");
        JLabel spawningWeight = new JLabel(Float.toString(spawning * totalWeightsModifier));
        float consistency = scores.get("Consistency");
        JLabel consistencyLabel = new JLabel(consistency == 1f ? "Consistent Tile Sides" : "Inconsistent Tile Sides");
        JLabel consistencyWeight = new JLabel(Float.toString(consistency * totalWeightsModifier));

        JLabel sizeLabel = new JLabel("Board Size: " + (int) size);
        JLabel sizeWeight = new JLabel(Float.toString(co.W_SIZE * (1f - (Math.abs(co.IDEAL_SIZE - size) / co.IDEAL_SIZE)) * totalWeightsModifier));
        float groups = scores.get("Groups");
        JLabel groupsLabel = new JLabel("Monster Groups: " + (int) groups);
        JLabel groupsWeight = new JLabel(Float.toString(co.W_GROUPS * (1f - (Math.abs(co.IDEAL_GROUP - groups) / co.IDEAL_GROUP)) * totalWeightsModifier));
        float health = scores.get("Health");
        JLabel healthLabel = new JLabel("Average Health: " + health + " HP");
        JLabel healthWeight = new JLabel(Float.toString(co.W_HEALTH * (1f - (Math.abs(co.IDEAL_HEALTH - health) / co.IDEAL_HEALTH)) * totalWeightsModifier));
        JLabel heightLabel = new JLabel("Board Height: " + (int) height);
        JLabel heightWeight = new JLabel(Float.toString(co.W_HEIGHT * (1f - (Math.abs(co.IDEAL_HEIGHT - height) / co.IDEAL_HEIGHT)) * totalWeightsModifier));
        JLabel widthLabel = new JLabel("Board Width: " + (int) width);
        JLabel widthWeight = new JLabel(Float.toString(co.W_WIDTH * (1f - (Math.abs(co.IDEAL_WIDTH - width) / co.IDEAL_WIDTH)) * totalWeightsModifier));

        fitness.add(connectedLabel);
        fitness.add(connectedWeight);
        fitness.add(geometryLabel);
        fitness.add(geometryWeight);
        fitness.add(repeatsLabel);
        fitness.add(repeatsWeight);
        fitness.add(spawningLabel);
        fitness.add(spawningWeight);
        fitness.add(consistencyLabel);
        fitness.add(consistencyWeight);
        fitness.add(sizeLabel);
        fitness.add(sizeWeight);
        fitness.add(groupsLabel);
        fitness.add(groupsWeight);
        fitness.add(healthLabel);
        fitness.add(healthWeight);
        fitness.add(heightLabel);
        fitness.add(heightWeight);
        fitness.add(widthLabel);
        fitness.add(widthWeight);

        JPanel tilesPanel = new JPanel();
        tilesPanel.setLayout(new GridLayout(0,1,5,5));
        TitledBorder tilesBorder = new TitledBorder(blackline, "Tiles");
        tilesBorder.setTitleJustification(TitledBorder.CENTER);
        tilesPanel.setBorder(tilesBorder);
        tilesPanel.setBackground(Color.WHITE);
        tilesPanel.setPreferredSize(new Dimension(300, 325));

        JPanel tileCountHold = new JPanel(new FlowLayout(FlowLayout.CENTER));
        tileCountHold.setBackground(Color.WHITE);
        JLabel tileCount = new JLabel(board.size() + " Tiles Used");
        tileCountHold.add(tileCount);
        tilesPanel.add(tileCountHold);

        HashMap<String, Integer> tileOrder = new HashMap<>();

        int colourID = 0;
        List<Color> colours = DescentGridBoardView.colours;
        for (String tile : co.gridRefs.get(id).keySet()) {
            tileOrder.put(tile, colourID);
            JLabel nodeName = new JLabel(tile);
            JPanel nodeColour = new JPanel();
            nodeColour.setPreferredSize(new Dimension(15, 8));
            nodeColour.setMaximumSize(new Dimension(15,8));
            nodeColour.setBackground(colours.get(colourID % colours.size()));
            nodeColour.setBorder(blackline);
            colourID++;
            JPanel node = new JPanel(new FlowLayout(FlowLayout.CENTER));
            node.setBackground(Color.WHITE);
            node.add(nodeName);
            node.add(nodeColour);
            tilesPanel.add(node);
        }

        left.add(fitness);
        left.add(tilesPanel);
        window.add(left, BorderLayout.LINE_START);

        JPanel viewhold = new JPanel();
        viewhold.setBackground(Color.BLACK);

        DescentGridBoardView view = new DescentGridBoardView(co.boards.get(id), co.boardTiles.get(id), co.gridRefs.get(id), co.tileRefs.get(id), dataPath, shadowSize, maxWidth/2 + 100, maxHeight/2 + 100);
        view.setBackground(Color.BLACK);
        DropShadowBorder shadow = new DropShadowBorder(Color.black, shadowSize, 0.9f, 12, true, true, true, true);
        view.setBorder(shadow);

        viewhold.add(view);
        window.add(viewhold, BorderLayout.CENTER);

        JPanel positions = new JPanel();
        positions.setBackground(Color.CYAN);
        positions.setLayout(new BorderLayout());

        positions.setPreferredSize(new Dimension(300, 700));

        JPanel traitsContainer = new JPanel(new GridLayout(1, 2, 5, 5));
        traitsContainer.setBackground(Color.CYAN);

        JPanel traits = new JPanel();
        traits.setLayout(new BoxLayout(traits, BoxLayout.Y_AXIS));
        traits.setBackground(Color.WHITE);
        TitledBorder traitsText = new TitledBorder(blackline, "Monster Traits");
        traitsText.setTitleJustification(TitledBorder.CENTER);
        traits.setBorder(traitsText);
        for (String trait: quest.monsterTraits) {
            JLabel t = new JLabel(trait);
            traits.add(t);
        }

        JPanel openGroups = new JPanel();
        openGroups.setLayout(new BoxLayout(openGroups, BoxLayout.Y_AXIS));
        openGroups.setBackground(Color.WHITE);
        TitledBorder openText = new TitledBorder(blackline, "Open Group Options");
        openText.setTitleJustification(TitledBorder.CENTER);
        openGroups.setBorder(openText);

        if (quest.monsterTraits.contains("All")) {
            openGroups.add(new JLabel("All Monsters Legal"));
        }
        for (String monster : GenerateBoards.monsters.keySet()) {
            String[] monsterTraits = ((PropertyStringArray) GenerateBoards.monsters.get(monster).get("super").getProperty("traits")).getValues();
            for (String mTrait : monsterTraits) {
                if (quest.monsterTraits.contains(mTrait)) {
                    JLabel m = new JLabel(monster);
                    openGroups.add(m);
                    break;
                }
            }
        }

        traitsContainer.add(traits);
        traitsContainer.add(openGroups);
        positions.add(traitsContainer, BorderLayout.PAGE_START);

        String dataPath = "data/descent2e/img/";
        int imageSize = 50;

        BufferedImage heroIcon = ImageIO.read(new File(dataPath + "heroes/healer.png"));
        JLabel heroPicture = new JLabel(new ImageIcon(heroIcon.getScaledInstance(imageSize, imageSize, Image.SCALE_SMOOTH)));

        JPanel spawningContainer = new JPanel();
        spawningContainer.setBackground(Color.WHITE);
        spawningContainer.setLayout(new BoxLayout(spawningContainer, BoxLayout.Y_AXIS));
        TitledBorder positionText = new TitledBorder(blackline,"Starting Positions:");
        positionText.setTitleJustification(TitledBorder.CENTER);
        spawningContainer.setBorder(positionText);
        JPanel heroSpawnContainer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        heroSpawnContainer.setBackground(Color.WHITE);
        String heroTile = quest.heroStartingPosition;
        JLabel heroSpawn = new JLabel("Heroes: " + heroTile);
        JPanel heroColour = new JPanel();
        heroColour.setPreferredSize(new Dimension(15, 8));
        heroColour.setMaximumSize(new Dimension(15,8));
        colourID = tileOrder.getOrDefault(heroTile, -1);
        heroColour.setBackground(colours.get(colourID % colours.size()));
        heroColour.setBorder(blackline);
        heroSpawnContainer.add(heroSpawn);
        heroSpawnContainer.add(heroColour);
        heroSpawnContainer.add(heroPicture);
        spawningContainer.add(heroSpawnContainer);

        for (Pair<String, String> monster : quest.monsters) {
            JPanel monsterContainer = new JPanel(new FlowLayout(FlowLayout.LEFT));
            monsterContainer.setBackground(Color.WHITE);

            String m = monster.a.split(":")[0];
            String pos = monster.b;
            boolean isLieutenant = monster.a.split(":")[1].contains("lieutenant");

            String monsterPath = dataPath;

            if (m.equals("OpenSmall")) {
                m = "Open Group (Small)";
                monsterPath += "tokens/mysteryobjective.png";
            }
            else if (m.equals("Open")) {
                m += " Group";
                monsterPath += "tokens/mysteryobjective.png";
            }
            else {
                if (isLieutenant)
                    monsterPath += "monsters/" + m.replace(" ", "-").toLowerCase() + ".png";
                else
                    monsterPath += "monsters/" + m.replace(" ", "-").toLowerCase() + "-master.png";
            }

            BufferedImage monsterIcon = ImageIO.read(new File(monsterPath));
            JLabel monsterPicture = new JLabel(new ImageIcon(monsterIcon.getScaledInstance(imageSize, imageSize, Image.SCALE_SMOOTH)));

            JLabel monsterSpawn = new JLabel(m + ": " + pos);

            JPanel monsterColour = new JPanel();
            monsterColour.setPreferredSize(new Dimension(15, 8));
            monsterColour.setMaximumSize(new Dimension(15,8));
            colourID = tileOrder.getOrDefault(pos, colours.size()-1);
            monsterColour.setBackground(colours.get(colourID % colours.size()));
            monsterColour.setBorder(blackline);

            monsterContainer.add(monsterSpawn);
            monsterContainer.add(monsterColour);
            monsterContainer.add(monsterPicture);
            spawningContainer.add(monsterContainer);
        }

        positions.add(spawningContainer, BorderLayout.CENTER);

        JButton save = new JButton("Save");
        if (co.isBoardSaved(id)) {
            save.setText("Saved!");
            save.setEnabled(false);
        }
        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                save.setText("Saved!");
                save.setEnabled(false);
                try {
                    co.saveBoard(id);
                    save(quest);
                } catch (IOException | ParseException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        JButton exit = new JButton("Exit");
        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hide();
                if (parentElite != null)
                    parentElite.show();
                if (parentList != null)
                    parentList.show();
            }
        });
        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(1, 2, 5, 5));
        buttons.add(save);
        buttons.add(exit);
        positions.add(buttons, BorderLayout.PAGE_END);

        window.add(positions, BorderLayout.LINE_END);
    }

    public void show() {
        window.setVisible(true);
    }

    public void hide() {
        window.setVisible(false);
    }

    public void save(PCGBoard quest) throws IOException, ParseException {
        String questPath = "data/descent2e/mainQuests.json";
        String boardPath = "data/descent2e/boards.json";
        String pcgPath = "data/descent2e/campaigns/PCG.json";

        Files.writeString(Path.of(pcgPath), "{\n  \"name\": \"PCG\",\n  \"quests\":\n    [\n      \"" + quest.name + "\"\n    ]\n}\n");

        String questData = FileUtils.readFileToString(new File(questPath), StandardCharsets.UTF_8);
        questData = questData.substring(0, questData.lastIndexOf("]") - 1) + ", ";
        Files.writeString(Path.of(questPath), questData);

        String boardData = FileUtils.readFileToString(new File(boardPath), StandardCharsets.UTF_8);
        boardData = boardData.substring(0, boardData.lastIndexOf("]") - 1) + ", ";
        Files.writeString(Path.of(boardPath), boardData);

        StringBuilder outputQ = new StringBuilder("{\"");
        outputQ.append("id\":\"").append(quest.name).append("\"");
        outputQ.append(",\"act\":").append(quest.act);
        outputQ.append(",\"starting-gold\":").append(quest.gold);
        outputQ.append(",\"starting-xp\":").append(quest.startingXP);
        outputQ.append(",\"traits\": [\"").append(String.join("\", \"", quest.monsterTraits)).append("\"]");
        outputQ.append(",\"monsters\": [");
        int monsterMax = quest.monsters.size();
        int monsterCounter = 0;
        for (Pair<String, String> monster : quest.monsters) {
            monsterCounter++;
            outputQ.append("[\"").append(monster.a).append("\", \"").append(monster.b).append("\"]");
            if (monsterCounter < monsterMax)
                outputQ.append(",");
            else
                outputQ.append("]");
        }

        outputQ.append(",\"tokens\": []");
        outputQ.append(",\"rules\": []");
        outputQ.append(",\"game-over\": [{" +
                "\"id\": \"CountGameOver\"," +
                "\"count\": {" +
                "\"type\": \"NFiguresAlive\"," +
                "\"figureNameContains\": \"Hero\"}," +
                "\"target\": 0," +
                "\"comparison-type\": \"Equal\"," +
                "\"result-heroes\": \"LOSE_GAME\"," +
                "\"result-overlord\": \"WIN_GAME\"" +
                "},{" +
                "\"id\": \"CountGameOver\"," +
                "\"count\": {" +
                "\"type\": \"NFiguresAlive\"," +
                "\"figureNameContains\": \"Monster\"}," +
                "\"target\": 0," +
                "\"comparison-type\": \"Equal\"," +
                "\"result-heroes\": \"WIN_GAME\"," +
                "\"result-overlord\": \"LOSE_GAME\"" +
                "}]");

        outputQ.append(",\"common-rewards\": [{" +
                "\"rewardType\": \"Attribute\"," +
                "\"attribute\": \"XP\"," +
                "\"value\": 1.0" +
                "}]");
        outputQ.append(",\"overlord-rewards\": [{" +
                "\"rewardType\": \"Attribute\"," +
                "\"attribute\": \"XP\"," +
                "\"value\": 1.0," +
                "\"mustWinToReceive\": true" +
                "}]");
        outputQ.append(",\"boards\": [ \"").append(quest.name.toLowerCase()).append("\"]");
        outputQ.append(",\"starting-tile\": \"").append(quest.heroStartingPosition).append("\"");

        outputQ.append("}");

        ObjectMapper mapper = new ObjectMapper();
        Object q = mapper.readValue(outputQ.toString(), Object.class);
        String prettyQ = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(q) + "]";
        Files.writeString(Path.of(questPath),prettyQ + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        StringBuilder outputB = new StringBuilder("{");

        outputB.append("\"type\": \"graph\",");
        outputB.append("\"verticesKey\": \"name\",");
        outputB.append("\"neighboursKey\": \"neighbours\",");
        outputB.append("\"maxNeighbours\": -1,");
        outputB.append("\"id\": \"").append(quest.name.toLowerCase()).append("\",");
        outputB.append("\"nodes\": [");
        int nodeCount = 0;
        for (PCGNode node : board) {
            nodeCount++;

            List<Connection> connections = node.connects;
            HashMap<Connection, String> neighbours = node.neighbours;

            outputB.append("{ \"name\": [\"String\", \"").append(node.name).append("\"],");
            outputB.append("\"orientation\": [\"Integer\", ").append(node.orientation).append("],");

            StringBuilder neighbourString = new StringBuilder("\"neighbours\": [\"String[]\", [");
            StringBuilder connectionString = new StringBuilder("\"connections\": [\"String[]\", [");

            int neighbourCount = 0;
            for (Connection c : connections) {
                String connect = switch(c) {
                    case NORTH -> "N-0";
                    case EAST -> "E-0";
                    case SOUTH -> "S-0";
                    case WEST -> "W-0";
                };
                neighbourCount++;
                neighbourString.append("\"").append(neighbours.get(c)).append("\"");
                connectionString.append("\"").append(connect).append("\"");
                if (neighbourCount < neighbours.size()) {
                    neighbourString.append(", ");
                    connectionString.append(", ");
                }
                else {
                    neighbourString.append("]],");
                    connectionString.append("]]}");
                }
            }
            outputB.append(neighbourString);
            outputB.append(connectionString);
            if (nodeCount < quest.board.size())
                outputB.append(",");
            else
                outputB.append("]}");
        }

        Object b = mapper.readValue(outputB.toString(), Object.class);
        String prettyB = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(b) +"]";
        Files.writeString(Path.of(boardPath),prettyB + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
