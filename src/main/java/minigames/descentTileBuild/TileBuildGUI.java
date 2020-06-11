package minigames.descentTileBuild;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.GridBoard;
import games.descent.DescentTypes;
import players.ActionController;
import players.HumanGUIPlayer;
import utilities.Pair;
import utilities.Vector2D;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collection;

public class TileBuildGUI extends AbstractGUI {
    TileBuildGridBoardView view;
    TerrainOptionsView terrainOptionsView;

    int width, height;
    private TileBuildState gameState;

    public TileBuildGUI(AbstractGameState gameState, ActionController ac) {
        super(ac, (DescentTypes.TerrainType.getWalkableTiles().size()+2)
                *((TileBuildParameters)gameState.getGameParameters()).maxGridSize
                *((TileBuildParameters)gameState.getGameParameters()).maxGridSize);

        TileBuildState dgs = (TileBuildState) gameState;
        this.gameState = dgs;

        view = new TileBuildGridBoardView(dgs.tile);
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
            int maxSize = ((TileBuildParameters)gameState.getGameParameters()).maxGridSize;
            if (w <= maxSize && h <= maxSize) {
                // Adjust grid in game state
                dgs.tile.setWidthHeight(w, h);
                // Adjust display size
                view.setPreferredSize(new Dimension(w, h));
                width = view.getPreferredSize().width;
                height = view.getPreferredSize().height;
                repaint();
            }
        });

        JTextField nameField = new JTextField("Tile ID", 10);
        JTextField imgPath = new JTextField(10);
        JTextArea jsonArea = new JTextArea(10, 10);

        JButton getjson = new JButton("Generate JSON");
        getjson.addActionListener(e -> {
            GridBoard<String> tile = ((TileBuildState) gameState).tile;

            // Grid print out in correct format
            // TODO: add edge tiles around the grid
            String gridValues = "[";
            for (int i = 0; i < tile.getHeight(); i++) {
                gridValues += "\n[";
                for (int j = 0; j < tile.getWidth(); j++) {
                    gridValues += "\"" + tile.getElement(j, i) + "\", ";
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
        actions.add(getjson);

        JScrollPane pane = new JScrollPane(jsonArea);
        pane.setPreferredSize(new Dimension(200, 200));
        actions.add(pane);

        // Display terrain type, depending on selection of terrain + coordinates in grid, only some actions are available
        terrainOptionsView = new TerrainOptionsView();
        JComponent actionPanel = createActionPanel(new Collection[]{new ArrayList<Object>() {{
            add(terrainOptionsView.highlight);
            add(view.highlight);
        }}}, width, defaultActionPanelHeight);

        JPanel east = new JPanel();
        east.add(actions);
        east.add(terrainOptionsView);

        getContentPane().add(east, BorderLayout.EAST);
        getContentPane().add(view, BorderLayout.CENTER);
        getContentPane().add(actionPanel, BorderLayout.SOUTH);

        setFrameProperties();
    }

    @Override
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        Pair<String, Rectangle> terrainType = terrainOptionsView.highlight;
        Vector2D cell = view.highlight;

        if (terrainType != null && cell != null) {
            // Find the right action
            List<AbstractAction> actions = gameState.getActions();
            for (AbstractAction a: actions) {
                if (a instanceof SetGridValueAction) {
                    if (((SetGridValueAction) a).getX() == cell.getX() && ((SetGridValueAction) a).getY() == cell.getY()) {
                        String actionStr = (String)((SetGridValueAction) a).getValue();
                        if (actionStr.equalsIgnoreCase(terrainType.a)){
                            actionButtons[0].setButtonAction(a, "Apply terrain type");
                            actionButtons[0].setVisible(true);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            view.updateComponent(((TileBuildState)gameState).tile);
            if (player instanceof HumanGUIPlayer) {
                updateActionButtons(player, gameState);
            }
            this.gameState = (TileBuildState) gameState;
        }
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width*2 + 200 + defaultItemSize*TerrainOptionsView.inARow, height + defaultActionPanelHeight);
    }
}
