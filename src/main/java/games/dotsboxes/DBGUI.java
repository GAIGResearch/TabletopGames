package games.dotsboxes;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import players.human.ActionController;
import players.human.HumanGUIPlayer;
import utilities.Utils;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DBGUI extends AbstractGUI {
    DBGridBoardView view;

    public DBGUI(AbstractGameState gameState, ActionController ac) {
        this(gameState, ac, defaultDisplayWidth, defaultDisplayHeight);
    }

    public DBGUI(AbstractGameState gameState, ActionController ac,
                 int displayWidth, int displayHeight) {
        super(ac, 100);
        this.width = displayWidth;
        this.height = displayHeight;

        view = new DBGridBoardView(((DBGameState)gameState));

        JPanel infoPanel = createGameStateInfoPanel("Dots and Boxes", gameState, width, defaultInfoPanelHeight);
        getContentPane().add(view, BorderLayout.CENTER);
        getContentPane().add(infoPanel, BorderLayout.NORTH);
        getContentPane().add(new JLabel("Human player: click on 2 adjacent dots to place your edge."), BorderLayout.SOUTH);

        setFrameProperties();
    }

    @Override
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        DBEdge db = view.getHighlight();
        if (gameState.getGameStatus() == Utils.GameResult.GAME_ONGOING && db != null) {
            List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState);
            boolean found = false;
            for (AbstractAction a: actions) {
                AddGridCellEdge aa = (AddGridCellEdge) a;
                if (aa.edge.equals(db)) {
                    ac.addAction(a);
                    found = true;
                    break;
                }
            }
            if (!found) System.out.println("Invalid action, click 2 adjacent dots to select an edge.");
            view.highlight = null;
        }
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            view.updateGameState(((DBGameState)gameState));
            if (player instanceof HumanGUIPlayer) {
                updateActionButtons(player, gameState);
            }
        }
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(Math.max(width,view.getPreferredSize().width), view.getPreferredSize().height + defaultInfoPanelHeight);
    }
}
