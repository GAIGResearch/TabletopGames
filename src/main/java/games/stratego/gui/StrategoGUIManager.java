package games.stratego.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import games.stratego.StrategoGameState;
import games.stratego.actions.Move;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;
import players.human.HumanGUIPlayer;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class StrategoGUIManager extends AbstractGUIManager implements IScreenHighlight {

    StrategoBoardView view;

    public StrategoGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);

        if (game == null) return;

        StrategoGameState gameState = (StrategoGameState) game.getGameState();
        view = new StrategoBoardView(gameState);
        for (AbstractPlayer p: game.getPlayers()) {
            if (p instanceof HumanGUIPlayer) {
                view.addHumanPlayerID(p.getPlayerID());
            }
        }

        // Set width/height of display
        this.width = Math.max(defaultDisplayWidth, defaultItemSize * gameState.getGridBoard().getWidth());
        this.height = defaultItemSize * gameState.getGridBoard().getHeight();

        JPanel infoPanel = createGameStateInfoPanel("Stratego", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new IScreenHighlight[]{this},
                width, defaultActionPanelHeight);

        parent.setLayout(new BorderLayout());
        parent.add(view, BorderLayout.CENTER);
        parent.add(infoPanel, BorderLayout.NORTH);
        parent.add(actionPanel, BorderLayout.SOUTH);
        parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + 10));
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();
    }

    @Override
    public int getMaxActionSpace() {
        return 100;
    }

    /**
     * Only shows actions for highlighted cell.
     * @param player - current player acting.
     * @param gameState - current game state to be used in updating visuals.
     */
    @Override
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING) {
            List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState);
            ArrayList<Rectangle> highlight = view.getHighlight();

            boolean activated = false;
            StrategoGameState gs = (StrategoGameState) gameState;
            if (highlight.size() == 2) {  // Need from and to squares
                Rectangle r1 = highlight.get(0);
                Rectangle r2 = highlight.get(1);
                int i = 1;
                for (AbstractAction abstractAction : actions) {
                    Move action = (Move) abstractAction;
                    if (action.from(gs).getX() == r1.x/defaultItemSize && action.from(gs).getY() == r1.y/defaultItemSize &&
                        action.to(gs).getX() == r2.x/defaultItemSize && action.to(gs).getY() == r2.y/defaultItemSize ||
                        action.from(gs).getX() == r2.x/defaultItemSize && action.from(gs).getY() == r2.y/defaultItemSize &&
                        action.to(gs).getX() == r1.x/defaultItemSize && action.to(gs).getY() == r1.y/defaultItemSize) {
                        actionButtons[0].setVisible(true);
                        actionButtons[0].setButtonAction(action, action.getPOString(gs));
                        activated = true;
                        break;
                    }
//                    if (action instanceof NormalMove) {
//                        actionButtons[i].setVisible(true);
//                        actionButtons[i].setButtonAction(action, action.getString(gameState));
//                        i++;
//                        activated = true;
//                    }
                }
            }
            if (!activated){
                actionButtons[0].setVisible(false);
                actionButtons[0].setButtonAction(null, "");
            }
        }
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            view.updateComponent(((StrategoGameState)gameState).getGridBoard());
        }
    }

    @Override
    public void clearHighlights() {
        view.getHighlight().clear();
    }
}
