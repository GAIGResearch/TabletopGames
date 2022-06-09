package games.descent2e.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.properties.PropertyString;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTurnOrder;
import games.descent2e.actions.Move;
import games.descent2e.components.Hero;
import gui.AbstractGUIManager;
import gui.GamePanel;
import players.human.ActionController;
import players.human.HumanGUIPlayer;
import utilities.Utils;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.List;

public class DescentGUI extends AbstractGUIManager {
    DescentGridBoardView view;
    int maxWidth = 1500;
    int maxHeight = 750;
    JLabel actingFigureLabel;
    DescentHeroView[] heroAreas;
    JPanel overlordArea;

    public DescentGUI(GamePanel panel, AbstractGameState gameState, ActionController ac) {
        super(panel, ac, 100);  // TODO: calculate/approximate max action space

        DescentGameState dgs = (DescentGameState) gameState;

        view = new DescentGridBoardView(dgs.getMasterBoard(), dgs, maxWidth/3,maxHeight/2);
        actingFigureLabel = new JLabel();

        JPanel infoPanel = createGameStateInfoPanel("Descent2e", gameState, maxWidth/2, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new Collection[]{view.highlights}, maxWidth/2, defaultActionPanelHeight,
                this::onMouseEnter, this::onMouseExit);

        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.X_AXIS));
        overlordArea = new JPanel(); // Will have cards and other stuff e.g. fatigue TODO
        overlordArea.setPreferredSize(new Dimension(maxWidth/2, defaultActionPanelHeight));
        south.add(overlordArea);
        south.add(actionPanel);

        JPanel eastWrapper = new JPanel();
        eastWrapper.setLayout(new BoxLayout(eastWrapper, BoxLayout.Y_AXIS));
        JPanel east = new JPanel();
        east.setLayout(new GridLayout(0,2));
        heroAreas = new DescentHeroView[dgs.getHeroes().size()];
        for (int i = 0; i < dgs.getHeroes().size(); i++) {
            Hero hero = dgs.getHeroes().get(i);
            heroAreas[i] = new DescentHeroView(dgs, hero, i, maxWidth/3, maxHeight/4);
            // TODO
            east.add(heroAreas[i]);
        }
        eastWrapper.add(Box.createRigidArea(new Dimension(0, 20)));
        eastWrapper.add(east);

        JPanel west = new JPanel();
        west.setLayout(new BoxLayout(west, BoxLayout.Y_AXIS));
        west.add(infoPanel);
        west.add(view);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));
        center.add(west);
        center.add(Box.createRigidArea(new Dimension(20, 0)));
        center.add(eastWrapper);

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(center);
        panel.add(south);
        panel.setPreferredSize(new Dimension(maxWidth, maxHeight));
    }

    private void onMouseEnter(ActionButton ab) {
        view.highlights.clear();
        if (ab.getButtonAction() instanceof Move) {
            view.highlights.addAll(((Move) ab.getButtonAction()).getPositionsTraveled());
        }
    }
    private void onMouseExit(ActionButton ab) {
        view.highlights.clear();
    }

    protected JPanel createGameStateInfoPanel(String gameTitle, AbstractGameState gameState, int width, int height) {
        JPanel gameInfo = new JPanel();
        gameInfo.setLayout(new BoxLayout(gameInfo, BoxLayout.Y_AXIS));
        gameInfo.add(new JLabel("<html><h1>" + gameTitle + "</h1></html>"));

        updateGameStateInfo(gameState);

        gameInfo.add(gameStatus);
//        gameInfo.add(playerStatus);
//        gameInfo.add(playerScores);
        gameInfo.add(gamePhase);
        gameInfo.add(turnOwner);
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);
        gameInfo.add(actingFigureLabel);

        gameInfo.setPreferredSize(new Dimension(width / 2 - 10, height));

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
        wrapper.add(Box.createRigidArea(new Dimension(10, 0)));
        wrapper.add(gameInfo);

        historyInfo.setPreferredSize(new Dimension(width / 2 - 10, height - 10));
        historyContainer = new JScrollPane(historyInfo);
        historyContainer.setMaximumSize(new Dimension(width / 2 - 25, height - 25));
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

    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState.getGameStatus() == Utils.GameResult.GAME_ONGOING && !(actionButtons == null)) {
            List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState);
            for (int i = 0; i < actions.size() && i < maxActionSpace; i++) {
                actionButtons[i].setVisible(true);
                actionButtons[i].setButtonAction(actions.get(i), gameState);
                actionButtons[i].setBackground(Color.white);
            }
            for (int i = actions.size(); i < actionButtons.length; i++) {
                actionButtons[i].setVisible(false);
                actionButtons[i].setButtonAction(null, "");
            }
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
