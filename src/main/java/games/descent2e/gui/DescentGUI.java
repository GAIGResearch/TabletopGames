package games.descent2e.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;
import games.descent2e.DescentParameters;
import games.descent2e.DescentTurnOrder;
import games.descent2e.actions.Move;
import games.descent2e.components.Hero;
import gui.AbstractGUIManager;
import gui.GamePanel;
import org.jdesktop.swingx.border.DropShadowBorder;
import players.human.ActionController;
import players.human.HumanGUIPlayer;
import utilities.ImageIO;
import utilities.Utils;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static games.descent2e.gui.DescentHeroView.titleFont;

public class DescentGUI extends AbstractGUIManager {
    DescentGridBoardView view;
    final int maxWidth = 1500;
    final int maxHeight = 750;
    JLabel actingFigureLabel, currentQuestLabel;
    DescentHeroView[] heroAreas;
    JPanel overlordArea;

    static boolean prettyVersion = true;  // Turn off to not draw images
    static Color foregroundColor = Color.black;

    public DescentGUI(GamePanel panel, AbstractGameState gameState, ActionController ac) {
        super(panel, ac, 100);  // TODO: calculate/approximate max action space

        DescentGameState dgs = (DescentGameState) gameState;
        if (prettyVersion) {
            panel.setBackground(ImageIO.GetInstance().getImage(((DescentParameters) gameState.getGameParameters()).dataPath + "img/bg2.jpg"));
            panel.setOpacity(1f);
            foregroundColor = Color.white;
        }

        int shadowSize = 10;
        view = new DescentGridBoardView(dgs.getMasterBoard(), dgs, shadowSize, maxWidth/2,maxHeight/2);
        DropShadowBorder shadow = new DropShadowBorder(Color.black, shadowSize, 0.9f, 12, true, true, true, true);
        view.setBorder(shadow);
        actingFigureLabel = new JLabel();
        currentQuestLabel = new JLabel();

        JPanel eastWrapper = new JPanel();
        eastWrapper.setOpaque(false);
        eastWrapper.setLayout(new BoxLayout(eastWrapper, BoxLayout.Y_AXIS));
        JPanel east = new JPanel();
        east.setOpaque(false);
        east.setLayout(new GridLayout(0,2));
        heroAreas = new DescentHeroView[dgs.getHeroes().size()];
        for (int i = 0; i < dgs.getHeroes().size(); i++) {
            Hero hero = dgs.getHeroes().get(i);
            heroAreas[i] = new DescentHeroView(dgs, hero, i, maxWidth/3, maxHeight/4);
            east.add(heroAreas[i]);
        }
        eastWrapper.add(Box.createRigidArea(new Dimension(0, 20)));
        eastWrapper.add(east);

        JPanel infoPanel = createGameStateInfoPanel("Descent2e", gameState, maxWidth/2, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new Collection[]{view.highlights}, maxWidth/2, defaultActionPanelHeight,
                this::onMouseEnter, this::onMouseExit);

        JPanel south = new JPanel();
        south.setOpaque(false);
        south.setLayout(new BoxLayout(south, BoxLayout.X_AXIS));
        overlordArea = new JPanel(); // Will have cards and other stuff e.g. fatigue TODO
        overlordArea.setOpaque(false);
        overlordArea.setPreferredSize(new Dimension(maxWidth/2, defaultActionPanelHeight));
        south.add(overlordArea);
        south.add(actionPanel);

        JPanel west = new JPanel();
        west.setOpaque(false);
        west.setLayout(new BoxLayout(west, BoxLayout.Y_AXIS));
        west.add(infoPanel);
        west.add(view);

        JPanel center = new JPanel();
        center.setOpaque(false);
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
        gameInfo.setOpaque(false);
        gameInfo.setLayout(new BoxLayout(gameInfo, BoxLayout.Y_AXIS));
        JLabel gt = new JLabel("<html><h1>Descent 2e</h1></html>");
        gt.setFont(titleFont);
        gt.setForeground(foregroundColor);
        gameInfo.add(gt);

        updateGameStateInfo(gameState);

        gameInfo.add(currentQuestLabel);
        gameInfo.add(gameStatus);
//        gameInfo.add(playerStatus);
//        gameInfo.add(playerScores);
        gameInfo.add(gamePhase);
        gameInfo.add(turnOwner);
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);
        gameInfo.add(actingFigureLabel);
        gameStatus.setForeground(foregroundColor);
        gamePhase.setForeground(foregroundColor);
        turnOwner.setForeground(foregroundColor);
        turn.setForeground(foregroundColor);
        currentPlayer.setForeground(foregroundColor);
        actingFigureLabel.setForeground(foregroundColor);
        currentQuestLabel.setForeground(foregroundColor);

        gameInfo.setPreferredSize(new Dimension(width / 2 - 10, height));
        gameInfo.setMaximumSize(new Dimension(width / 2 - 10, height));

        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
        wrapper.add(Box.createRigidArea(new Dimension(10, 0)));
        wrapper.add(gameInfo);

        JPanel historyWrapper = new JPanel();
        historyWrapper.setOpaque(false);
        historyWrapper.setLayout(new BoxLayout(historyWrapper, BoxLayout.Y_AXIS));
        JLabel historyTitle = new JLabel("Action history:");
        historyTitle.setForeground(foregroundColor);
        historyWrapper.add(historyTitle);
        historyInfo.setPreferredSize(new Dimension(width / 2 - 10, height - 10));
        historyInfo.setForeground(foregroundColor);
        historyInfo.setOpaque(false);
        historyContainer = new JScrollPane(historyInfo);
        historyContainer.setOpaque(false);
        historyContainer.getViewport().setOpaque(false);
        historyContainer.setMaximumSize(new Dimension(width / 2 - 25, height - 25));
        historyWrapper.add(historyContainer);
        wrapper.add(historyWrapper);
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
        currentQuestLabel.setText("Quest: " + dgs.getCurrentQuest().getName());
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

    protected JComponent createActionPanel(Collection[] highlights, int width, int height, boolean boxLayout,
                                           Consumer<ActionButton> onActionSelected,
                                           Consumer<ActionButton> onMouseEnter,
                                           Consumer<ActionButton> onMouseExit) {
        JPanel actionPanel = new JPanel() {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(width, super.getMaximumSize().height);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(width, super.getPreferredSize().height);
            }
        };
        actionPanel.setOpaque(false);

        actionButtons = new ActionButton[maxActionSpace];
        for (int i = 0; i < maxActionSpace; i++) {
            ActionButton ab = new ActionButton(ac, highlights, onActionSelected, onMouseEnter, onMouseExit);
            actionButtons[i] = ab;
            actionButtons[i].setVisible(false);
            actionPanel.add(actionButtons[i]);
        }
        for (ActionButton actionButton : actionButtons) {
            actionButton.informAllActionButtons(actionButtons);
        }

        JScrollPane pane = new JScrollPane(actionPanel);
        pane.setOpaque(false);
        pane.getViewport().setOpaque(false);
        pane.setPreferredSize(new Dimension(width, height));
        pane.setMaximumSize(new Dimension(width, height));
        if (boxLayout) {
            pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        }
        return pane;
    }

}
