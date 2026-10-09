package games.coltexpress.gui;

import gui.GUIMessages;
import gui.AbstractGUIManager;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.interfaces.IGamePhase;
import games.GameType;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressParameters;
import games.coltexpress.components.Compartment;
import gui.IScreenHighlight;
import gui.GamePanel;
import gui.views.RulesView;
import players.human.ActionController;
import utilities.ImageIO;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.Set;

import static games.coltexpress.ColtExpressGameState.ColtExpressGamePhase.ExecuteActions;

public class ColtExpressGUIManager extends AbstractGUIManager {
    // Settings for display area sizes
    final static int playerAreaWidth = 470;
    final static int playerAreaWidthScroll = 290;
    final static int playerAreaHeight = 100;
    final static int playerAreaHeightScroll = 150;
    final static int ceCardWidth = 50;
    final static int ceCardHeight = 60;
    final static int roundCardWidth = 100;
    final static int roundCardHeight = 80;
    final static int trainCarWidth = 130;
    final static int trainCarHeight = 80;
    final static int playerSize = 40;
    final static int lootSize = 20;

    // Player views
    ColtExpressPlayerView[] playerHands;
    // Planned actions deck view
    ColtExpressDeckView plannedActions;
    // Main train view
    ColtExpressTrainView trainView;
    ColtExpressRoundView roundView;

    // Currently active player
    int activePlayer = -1;
    // Border highlight of active player
    Border highlightActive = BorderFactory.createLineBorder(new Color(220, 169, 11), 3);
    Border[] playerViewBorders;

    public ColtExpressGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanID) {
        super(parent, game, ac, humanID);

        UIManager.put("TabbedPane.contentOpaque", false);
        UIManager.put("TabbedPane.opaque", false);
        UIManager.put("TabbedPane.tabsOpaque", false);

        if (game != null) {
            AbstractGameState gameState = game.getGameState();
            if (gameState != null) {
                JTabbedPane pane = new JTabbedPane();
                JPanel main = new JPanel();
                main.setOpaque(false);
                main.setLayout(new BorderLayout());
                pane.add("Main", main);

                ColtExpressGameState cegs = (ColtExpressGameState) gameState;
                ColtExpressParameters cep = (ColtExpressParameters) gameState.getGameParameters();

                List<Compartment> train = ((ColtExpressGameState) gameState).getTrainCompartments();
                trainView = new ColtExpressTrainView(train, cep.getDataPath(), cegs.getPlayerCharacters());
                trainView.setOpaque(false);
                plannedActions = new ColtExpressDeckView(cegs.getPlannedActions(), true, cep.getDataPath(), cegs.getPlayerCharacters());
                plannedActions.setOpaque(false);
                roundView = new ColtExpressRoundView(train, cep.nMaxRounds, cep.getDataPath(), cegs.getPlayerCharacters());
                roundView.setOpaque(false);

                activePlayer = gameState.getCurrentPlayer();
                int nPlayers = gameState.getNPlayers();
                this.width = trainCarWidth*3/2*(train.size()+1) + playerAreaWidth;
                this.height = Math.max(playerAreaHeight * (nPlayers+1), trainView.height + ceCardHeight + 50 + roundView.height) + defaultInfoPanelHeight + defaultActionPanelHeight;
                RulesView.addTabs(pane, GameType.ColtExpress, cep, height*2/3+100);

                parent.setBackground(ImageIO.GetInstance().getImage("data/coltexpress/bg.jpg"));

                // Create main game area that will hold all game views
                JPanel mainGameArea = new JPanel();
                mainGameArea.setOpaque(false);
                JPanel playerViews = new JPanel();
                playerViews.setOpaque(false);
                playerViews.setLayout(new BoxLayout(playerViews, BoxLayout.Y_AXIS));

                // Planned actions + train + rounds go in the center
                JPanel centerArea = new JPanel();
                centerArea.setOpaque(false);
                centerArea.setLayout(new BoxLayout(centerArea, BoxLayout.Y_AXIS));
                centerArea.add(trainView);
                centerArea.add(roundView);
                centerArea.add(plannedActions);
                mainGameArea.add(centerArea);
                mainGameArea.add(playerViews);

                // Player hands go on the edges
                playerHands = new ColtExpressPlayerView[nPlayers];
                playerViewBorders = new Border[nPlayers];
                for (int i = 0; i < nPlayers; i++) {
                    ColtExpressPlayerView playerHand = new ColtExpressPlayerView(i, cep.getDataPath(), cegs.getPlayerCharacters());
                    playerHand.setOpaque(false);
                    // Get agent name
                    String agentName = game.getPlayers().get(i).toString();

                    // Create border, layouts and keep track of this view
                    TitledBorder title = BorderFactory.createTitledBorder(
                            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + agentName + "]",
                            TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
                    playerViewBorders[i] = title;
                    playerHand.setBorder(title);

                    playerViews.add(playerHand);
                    playerHands[i] = playerHand;
                }

                // Top area will show state information
                JPanel infoPanel = createGameStateInfoPanel("Colt Express", gameState, width, defaultInfoPanelHeight);
                infoPanel.setOpaque(false);
                // Bottom area will show actions available
                JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, true, null, null, null);
                actionPanel.setOpaque(false);

                main.add(infoPanel, BorderLayout.NORTH);
                main.add(mainGameArea, BorderLayout.CENTER);
                main.add(actionPanel, BorderLayout.SOUTH);

                parent.setLayout(new BorderLayout());
                parent.add(pane, BorderLayout.CENTER);
                parent.setPreferredSize(new Dimension(width, height));
                parent.revalidate();
                parent.setVisible(true);
                parent.repaint();
            }
        }

    }

    @Override
    public int getMaxActionSpace() {
        return 25;
    }

    @Override
    protected JPanel createGameStateInfoPanel(String gameTitle, AbstractGameState gameState, int width, int height) {
        JPanel gameInfo = new JPanel();
        gameInfo.setOpaque(false);
        gameInfo.setLayout(new BoxLayout(gameInfo, BoxLayout.Y_AXIS));
        gameInfo.add(new JLabel("<html><h1>" + gameTitle + "</h1></html>"));

        updateGameStateInfo(gameState);

        gameInfo.add(gameStatus);
        gameInfo.add(playerStatus);
        gameInfo.add(gamePhase);
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);

        gameInfo.setPreferredSize(new Dimension(width/2 - 10, height));

        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new FlowLayout());
        wrapper.add(gameInfo);

        historyInfo.setOpaque(false);
        historyInfo.setPreferredSize(new Dimension(width/2 - 10, height));
        historyContainer = new JScrollPane(historyInfo);
        historyContainer.setOpaque(false);
//        historyContainer.getViewport().setOpaque(false);
        historyContainer.setPreferredSize(new Dimension(width/2 - 25, height));
        wrapper.add(historyContainer);
        return wrapper;
    }

    IGamePhase currentGamePhase;

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            if (gameState.getCurrentPlayer() != activePlayer) {
                activePlayer = gameState.getCurrentPlayer();
            }
            if (currentGamePhase == null || currentGamePhase != gameState.getGamePhase()) {
                if (gameState.getGamePhase() == ExecuteActions) {
                    GUIMessages.show(parent, "Planning phase over, execute actions!");
                } else {
                    GUIMessages.show(parent, "New round! Time to plan actions!");
                }
            }
            currentGamePhase = gameState.getGamePhase();

            // Update decks and visibility
            ColtExpressGameState cegs = (ColtExpressGameState)gameState;
            for (int i = 0; i < gameState.getNPlayers(); i++) {
                playerHands[i].update((ColtExpressGameState) gameState, humanPlayerIds);

                // Highlight active player
                if (i == gameState.getCurrentPlayer()) {
                    Border compound = BorderFactory.createCompoundBorder(
                            highlightActive, playerViewBorders[i]);
                    playerHands[i].setBorder(compound);
                } else {
                    playerHands[i].setBorder(playerViewBorders[i]);
                }
            }
            plannedActions.updateComponent(cegs.getPlannedActions());
            // planned cards are drawn as the viewing (human) player sees them, or all in full-observability mode
            plannedActions.informActivePlayer(viewingPlayer(gameState));
            plannedActions.setFront(gameState.getCoreGameParameters().alwaysDisplayFullObservable);

            // Show planned actions from the first played
            plannedActions.setFirstOnTop(gameState.getGamePhase() == ExecuteActions);

            // Update train view
            trainView.update(cegs);
            roundView.update(cegs);

        }
    }
}
