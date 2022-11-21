package games.findmurderer.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.findmurderer.actions.Kill;
import games.findmurderer.MurderGameState;
import games.findmurderer.actions.LookAt;
import games.findmurderer.actions.Move;
import games.findmurderer.actions.Query;
import games.findmurderer.components.Person;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.ScreenHighlight;
import players.human.ActionController;
import utilities.Utils;
import utilities.Vector2D;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class MurderGUI extends AbstractGUIManager {
    MurderBoardView view;
    JTextPane notebook;

    public MurderGUI(GamePanel parent, Game game, ActionController ac) {
        super(parent, ac, 500);

        if (game == null) return;

        MurderGameState gameState = (MurderGameState) game.getGameState();
        view = new MurderBoardView(gameState);

        // Set width/height of display
        this.width = Math.max(defaultDisplayWidth, defaultItemSize * gameState.getGrid().getWidth() + 220);
        this.height = defaultItemSize * gameState.getGrid().getHeight();

        JPanel infoPanel = createGameStateInfoPanel("Find the Murderer", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new ScreenHighlight[]{view},
                width, defaultActionPanelHeight, true);

        notebook = new JTextPane();
        JScrollPane pane = new JScrollPane(notebook);
        pane.setPreferredSize(new Dimension(200, 300));
        pane.setMaximumSize(new Dimension(200, 300));
        pane.setMinimumSize(new Dimension(200, 300));

        parent.setLayout(new BorderLayout());
        parent.add(view, BorderLayout.CENTER);
        parent.add(infoPanel, BorderLayout.NORTH);
        parent.add(actionPanel, BorderLayout.SOUTH);
        parent.add(pane, BorderLayout.EAST);
        parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + defaultCardHeight));
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();
    }

    /**
     * Only shows actions for highlighted cell.
     * @param player - current player acting.
     * @param gameState - current game state to be used in updating visuals.
     */
    @Override
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState.getGameStatus() == Utils.GameResult.GAME_ONGOING) {
            MurderGameState mgs = (MurderGameState) gameState;
            List<core.actions.AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState);
            ArrayList<Rectangle> highlight = view.getHighlight();
            HashMap<Rectangle, Vector2D> rectToGridPosMap = view.getRectToGridPosMap();

            resetActionButtons();

            int idx = 0;
            if (highlight.size() > 0) {
                Rectangle r = highlight.get(0);
                Vector2D highlightedPosInGrid = rectToGridPosMap.get(r);
                if (highlightedPosInGrid != null) {
                    Person p = mgs.getGrid().getElement(highlightedPosInGrid.getX(), highlightedPosInGrid.getY());
                    for (AbstractAction abstractAction : actions) {
                        if (abstractAction instanceof Kill) {
                            int target = ((Kill) abstractAction).target;
                            if (p != null && target == p.getComponentID()) {
                                actionButtons[idx].setVisible(true);
                                actionButtons[idx++].setButtonAction(abstractAction, "Kill " + target);
                            }
                        } else if (abstractAction instanceof Query) {
                            int target = ((Query) abstractAction).targetID;
                            if (p != null && target == p.getComponentID()) {
                                actionButtons[idx].setVisible(true);
                                actionButtons[idx++].setButtonAction(abstractAction, "Query " + target);
                            }
                        } else if (abstractAction instanceof LookAt) {
                            Vector2D target = ((LookAt) abstractAction).target;
                            if (target.equals(highlightedPosInGrid)) {
                                actionButtons[idx].setVisible(true);
                                actionButtons[idx++].setButtonAction(abstractAction, "Look at " + target);
                            }
                        } else if (abstractAction instanceof Move) {
                            Vector2D target = ((Move) abstractAction).toPos;
                            if (target.equals(highlightedPosInGrid)) {
                                actionButtons[idx].setVisible(true);
                                actionButtons[idx++].setButtonAction(abstractAction, "Move to " + target);
                            }
                        }
                    }
                }
            }
            for (AbstractAction abstractAction : actions) {
                if (!(abstractAction instanceof Kill) && !(abstractAction instanceof Query) && !(abstractAction instanceof LookAt) && !(abstractAction instanceof Move)) {
                    actionButtons[idx].setVisible(true);
                    actionButtons[idx++].setButtonAction(abstractAction, gameState);
                }
            }
        }
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            MurderGameState mgs = (MurderGameState) gameState;
            view.updateComponent(mgs.getGrid());
            view.update(mgs);

            String notebookText = "";
            TreeMap<Integer, String> mapping = new TreeMap<>();
            for (Person p: mgs.getGrid().getNonNullComponents()) {
                if (!p.interactionHistory.isEmpty()) {
                    mapping.put(p.getComponentID(), "Person " + p.getComponentID() + "\n" + p.interactionHistory.toString() + "\n");
                }
            }
            for (int key: mapping.keySet()) {
                notebookText += mapping.get(key);
            }
            if (!notebook.getText().equals(notebookText)) {
                notebook.setText(notebookText);
                notebook.setCaretPosition(0);
            }
        }
    }
}
