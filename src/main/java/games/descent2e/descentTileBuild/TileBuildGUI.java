package games.descent2e.descentTileBuild;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.BoardNode;
import core.components.GridBoard;
import core.properties.PropertyVector2D;
import games.descent2e.DescentTypes;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;
import players.human.HumanGUIPlayer;
import utilities.Pair;
import utilities.Path;
import utilities.Vector2D;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static core.CoreConstants.coordinateHash;

public class TileBuildGUI extends AbstractGUIManager {
    TileBuildGridBoardView view;
    TerrainOptionsView terrainOptionsView;

    Pair<String, Rectangle> terrainType;
    Vector2D cell;

    int width, height;

    public TileBuildGUI(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);

        TileBuildState dgs = (TileBuildState) game.getGameState();


        view = new TileBuildGridBoardView(dgs, dgs.tile);
        width = view.getPreferredSize().width;
        height = view.getPreferredSize().height;

        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));

        JTextField gridWidth = new JTextField(""+dgs.tile.getWidth(), 10);
        JTextField gridHeight = new JTextField(""+dgs.tile.getHeight(), 10);
        JButton updateGridSize = new JButton("Update grid size");
        updateGridSize.addActionListener(e -> {
            int w = Integer.parseInt(gridWidth.getText());
            int h = Integer.parseInt(gridHeight.getText());
            int maxSize = ((TileBuildParameters)dgs.getGameParameters()).maxGridSize;
            if (w <= maxSize && h <= maxSize) {
                // Adjust grid in game state
                dgs.tile.setWidthHeight(w, h);
                // Adjust display size
                view.setPreferredSize(new Dimension(w, h));
                width = view.getPreferredSize().width;
                height = view.getPreferredSize().height;
                parent.repaint();
            }
        });

        JTextField nameField = new JTextField("Tile ID", 10);
        JTextField imgPath = new JTextField(10);
        JTextArea jsonArea = new JTextArea(10, 10);

        JButton getjson = new JButton("Generate JSON");
        getjson.addActionListener(e -> {
            GridBoard tile = dgs.tile.copy();

            // Add edge tiles around the grid
            int minX = tile.getWidth()-1;
            int minY = tile.getHeight()-1;
            int maxX = 0;
            int maxY = 0;
            for (int i = 0; i < tile.getHeight(); i++) {
                for (int j = 0; j < tile.getWidth(); j++) {
                    if (tile.getElement(j, i) != null && DescentTypes.TerrainType.isInsideTerrain(tile.getElement(j, i).getComponentName())) {
                        if (j < minX) minX = j;
                        if (i < minY) minY = i;
                        if (i > maxY) maxY = i;
                        if (j > maxX) maxX = j;
                    }
                }
            }
            int w = tile.getWidth();
            int h = tile.getHeight();
            int offX = 0;
            int offY = 0;
            if (minX == 0) {
                w++;
                offX = 1;
            }
            if (minY == 0) {
                h++;
                offY = 1;
            }
            if (maxX == tile.getWidth() - 1) {
                w++;
            }
            if (maxY == tile.getHeight() - 1) {
                h++;
            }
            tile.setWidthHeight(w, h, offX, offY);

            // TODO check if all "open" spaces are exactly 2-wide and not next to each other

            // Grid print out in correct format
            String gridValues = "[";
            for (int i = 0; i < tile.getHeight(); i++) {
                gridValues += "\n[";
                for (int j = 0; j < tile.getWidth(); j++) {
                    if (tile.getElement(j, i) == null) {
                        gridValues += "\"" + tile.getElement(j, i) + "\", ";
                    } else {
                        gridValues += "\"" + tile.getElement(j, i).getComponentName() + "\", ";
                    }
                }
                gridValues = gridValues.substring(0, gridValues.length()-2);
                gridValues += "],";
            }
            gridValues = gridValues.substring(0, gridValues.length()-1);
            gridValues += "\n]";

            // Generate jSON format from this
            String json = "{\n";
            json += "\"id\": \"" + nameField.getText() + "\",\n";
            json += "\"size\": [" + tile.getWidth() + ", " + tile.getHeight() + "],\n";
            json += "\"img\": \"" + imgPath.getText() + "\",\n";
            json += "\"class\": \"String\",\n";
            json += "\"grid\": " + gridValues + "\n}\n";
            jsonArea.setText(json);
            /*
             {
             "id": "13A1",
             "size": [6, 3],
             "img": "",
             "class": "String",
             "grid":
             [
             ["edge", "edge", "open", "open", "edge", "edge"],
             ["edge","plain", "plain", "plain", "plain", "edge"],
             ["edge", "edge", "open", "open", "edge", "edge"]
             ]
             }
             */
        });
        JButton getPath = new JButton("Show shortest path");
        getPath.addActionListener(e -> {
            // Compute path from oldHighlight to highlight
            if (view.oldHighlight != null && view.highlight != null) {
                BoardNode node1 = dgs.tile.getElement(view.oldHighlight.getX(), view.oldHighlight.getY());
                BoardNode node2 = dgs.tile.getElement(view.highlight.getX(), view.highlight.getY());
                if (node1 != null && node2 != null) {
                    Path p = dgs.pathfinder.getPath(dgs, node1.getComponentID(), node2.getComponentID());
                    ArrayList<Vector2D> points = new ArrayList<>();
                    for (int i : p.points) {
                        points.add(((PropertyVector2D) (dgs.getComponentById(i)).getProperty(coordinateHash)).values);
                    }
                    view.path = points;
                }
            }
        });

        JPanel adjustSize = new JPanel();
        adjustSize.add(new JLabel("Tile size (w x h): "));
        adjustSize.add(gridWidth);
        adjustSize.add(gridHeight);
        adjustSize.add(updateGridSize);

        JPanel imgPathPan = new JPanel();
        imgPathPan.add(new JLabel("Background img path: "));
        imgPathPan.add(imgPath);

        actions.add(adjustSize);
        actions.add(nameField);
        actions.add(imgPathPan);
        JPanel buttons = new JPanel();
        buttons.add(getjson);
        buttons.add(getPath);
        actions.add(buttons);

        JScrollPane pane = new JScrollPane(jsonArea);
        pane.setPreferredSize(new Dimension(200, 200));
        actions.add(pane);

        // Display terrain type, depending on selection of terrain + coordinates in grid, only some actions are available
        terrainOptionsView = new TerrainOptionsView();
        JComponent actionPanel = createActionPanel(new IScreenHighlight[]{terrainOptionsView, view}, width, defaultActionPanelHeight);

        JPanel east = new JPanel();
        east.add(actions);
        east.add(terrainOptionsView);

        parent.setLayout(new BorderLayout());
        parent.add(east, BorderLayout.EAST);
        parent.add(view, BorderLayout.CENTER);
        parent.add(actionPanel, BorderLayout.SOUTH);
    }

    @Override
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        Pair<String, Rectangle> terrainType = terrainOptionsView.highlight;
        Vector2D cell = view.highlight;

        if (terrainType != null && cell != null) {
//            if (this.cell != null && !this.cell.equals(cell) || this.terrainType != null && !this.terrainType.equals(terrainType)) {
//                resetActionButtons();
//            }

            // Find the right action
            List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState);
            for (AbstractAction a: actions) {
                if (a instanceof SetGridValueAction) {
                    if (((SetGridValueAction) a).getX() == cell.getX() && ((SetGridValueAction) a).getY() == cell.getY()) {
                        String actionStr;
                        if (((SetGridValueAction) a).getValue(gameState) != null) {
                            actionStr = ((SetGridValueAction) a).getValue(gameState).getComponentName();
                        } else {
                            actionStr = "null";
                        }
                        if (actionStr.equalsIgnoreCase(terrainType.a)) {
                            ac.addAction(a);
//                            terrainOptionsView.highlight = null;
                            view.highlight = null;
                            view.oldHighlight = null;
                            view.path = null;
                        }
                    }
                }
            }
        }

        this.cell = cell;
        this.terrainType = terrainType;
    }

    @Override
    public int getMaxActionSpace() {
        return (DescentTypes.TerrainType.getWalkableTerrains().size()+2)
                *((TileBuildParameters)game.getGameState().getGameParameters()).maxGridSize
                *((TileBuildParameters)game.getGameState().getGameParameters()).maxGridSize;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            view.updateComponent(((TileBuildState)gameState).tile);
            if (player instanceof HumanGUIPlayer) {
                updateActionButtons(player, gameState);
            }
        }
        parent.repaint();
    }

}
