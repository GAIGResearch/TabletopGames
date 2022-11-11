package games.findmurderer.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.findmurderer.actions.Kill;
import games.findmurderer.MurderGameState;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.ScreenHighlight;
import players.human.ActionController;
import utilities.Utils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MurderGUI extends AbstractGUIManager {
    MurderBoardView view;

    public MurderGUI(GamePanel parent, Game game, ActionController ac) {
        super(parent, ac, 500);

        if (game == null) return;

        MurderGameState gameState = (MurderGameState) game.getGameState();
        view = new MurderBoardView(gameState.getGrid());

        // Set width/height of display
        this.width = Math.max(defaultDisplayWidth, defaultItemSize * gameState.getGrid().getWidth());
        this.height = defaultItemSize * gameState.getGrid().getHeight();

        JPanel infoPanel = createGameStateInfoPanel("Find the Murderer", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new ScreenHighlight[]{view},
                width, defaultActionPanelHeight, true);

        parent.setLayout(new BorderLayout());
        parent.add(view, BorderLayout.CENTER);
        parent.add(infoPanel, BorderLayout.NORTH);
        parent.add(actionPanel, BorderLayout.SOUTH);
        parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + defaultCardHeight + 20));
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
            List<core.actions.AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState);
            ArrayList<Rectangle> highlight = view.getHighlight();
            HashMap<Rectangle, Integer> rectToComponentIDMap = view.getRectToComponentIDMap();

            resetActionButtons();

            int idx = 0;
            if (highlight.size() > 0) {
                Rectangle r = highlight.get(0);
                for (AbstractAction abstractAction : actions) {
                    if (abstractAction instanceof Kill) {
                        int target = ((Kill)abstractAction).target;
                        if (rectToComponentIDMap.containsKey(r) && target == rectToComponentIDMap.get(r)) {
                            actionButtons[idx].setVisible(true);
                            actionButtons[idx++].setButtonAction(abstractAction, "Kill " + target);
                            break;
                        }
                    }
                }
            }
            for (AbstractAction abstractAction : actions) {
                if (!(abstractAction instanceof Kill)) {
                    actionButtons[idx].setVisible(true);
                    actionButtons[idx++].setButtonAction(abstractAction, gameState);
                }
            }
        }
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            view.updateComponent(((MurderGameState)gameState).getGrid());
        }
    }
}
