package games.descent2e.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTurnOrder;
import gui.AbstractGUIManager;
import gui.GamePanel;
import players.human.ActionController;
import players.human.HumanGUIPlayer;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

public class DescentGUI extends AbstractGUIManager {
    DescentGridBoardView view;
    int width, height;
    int maxWidth = 800;
    int maxHeight = 600;
    JLabel actingFigureLabel;

    public DescentGUI(GamePanel panel, AbstractGameState gameState, ActionController ac) {
        super(panel, ac, 100);  // TODO: calculate/approximate max action space

        DescentGameState dgs = (DescentGameState) gameState;

        view = new DescentGridBoardView(dgs.getMasterBoard(), dgs);
        width = view.getPreferredSize().width;
        height = view.getPreferredSize().height;
        actingFigureLabel = new JLabel();

        JPanel infoPanel = createGameStateInfoPanel("Descent2e", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new Collection[0], width, defaultActionPanelHeight);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(infoPanel);

//        JScrollPane pane = new JScrollPane(view);
//        pane.setPreferredSize(new Dimension(maxWidth, maxHeight));

        panel.setLayout(new BorderLayout());
        panel.add(view, BorderLayout.CENTER);
        panel.add(north, BorderLayout.NORTH);
        panel.add(actionPanel, BorderLayout.SOUTH);
    }

    protected JPanel createGameStateInfoPanel(String gameTitle, AbstractGameState gameState, int width, int height) {
        JPanel gameInfo = new JPanel();
        gameInfo.setLayout(new BoxLayout(gameInfo, BoxLayout.Y_AXIS));
        gameInfo.add(new JLabel("<html><h1>" + gameTitle + "</h1></html>"));

        updateGameStateInfo(gameState);

        gameInfo.add(gameStatus);
        gameInfo.add(playerStatus);
        gameInfo.add(playerScores);
        gameInfo.add(gamePhase);
        gameInfo.add(turnOwner);
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);
        gameInfo.add(actingFigureLabel);

        gameInfo.setPreferredSize(new Dimension(width / 2 - 10, height));

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new FlowLayout());
        wrapper.add(gameInfo);

        historyInfo.setPreferredSize(new Dimension(width / 2 - 10, height));
        historyContainer = new JScrollPane(historyInfo);
        historyContainer.setPreferredSize(new Dimension(width / 2 - 25, height));
        wrapper.add(historyContainer);
        return wrapper;
    }

    protected void updateGameStateInfo(AbstractGameState gameState) {
        super.updateGameStateInfo(gameState);
        DescentTurnOrder dto = (DescentTurnOrder)gameState.getTurnOrder();
        DescentGameState dgs = (DescentGameState)gameState;
        if (dgs.getCurrentPlayer() == dgs.getOverlordPlayer()) {
            actingFigureLabel.setText("Acting monster: " + dgs.getActingFigure().getComponentName());
        } else {
            actingFigureLabel.setText("Acting hero: " + dgs.getActingFigure().getComponentName() + "(" +dto.getHeroFigureActingNext() + ")");
        }
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            if (player instanceof HumanGUIPlayer) {
                updateActionButtons(player, gameState);
            }
            view.updateGameState((DescentGameState) gameState);
        }
        parent.repaint();
    }

}
