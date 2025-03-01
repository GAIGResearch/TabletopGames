package games.coltexpress.gui;

import gui.AbstractGUIManager;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.interfaces.IGamePhase;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressParameters;
import games.coltexpress.components.Compartment;
import gui.IScreenHighlight;
import gui.GamePanel;
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
                JPanel rules = new JPanel();
                pane.add("Main", main);
                pane.add("Rules", rules);
                JLabel ruleText = new JLabel(getRuleText());
                rules.add(ruleText);

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
                ruleText.setPreferredSize(new Dimension(width*2/3+60, height*2/3+100));

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
                    String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
                    String agentName = split[split.length - 1];

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
                JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, false, true, null, null, null);
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

    protected JComponent createActionPanel(IScreenHighlight[] highlights, int width, int height, boolean boxLayout) {
        JPanel actionPanel = new JPanel();
        actionPanel.setOpaque(false);
        if (boxLayout) {
            actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        }

        actionButtons = new ActionButton[maxActionSpace];
        for (int i = 0; i < maxActionSpace; i++) {
            ActionButton ab = new ActionButton(ac, highlights);
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
        if (boxLayout) {
            pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        }
        return pane;
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
                    JOptionPane.showMessageDialog(parent, "Planning phase over, execute actions!");
                } else {
                    JOptionPane.showMessageDialog(parent, "New round! Time to plan actions!");
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
            int activePlayer = player != null? (gameState.getCoreGameParameters().alwaysDisplayCurrentPlayer ||
                    gameState.getCoreGameParameters().alwaysDisplayFullObservable? player.getPlayerID():
                    humanPlayerIds.contains(player.getPlayerID())? player.getPlayerID():-1) : -1;
            plannedActions.informActivePlayer(activePlayer);

            // Show planned actions from the first played
            plannedActions.setFirstOnTop(gameState.getGamePhase() == ExecuteActions);

            // Update train view
            trainView.update(cegs);
            roundView.update(cegs);

        }
    }

    private String getRuleText() {
        String rules = "<html><center><h1>Colt Express</h1></center><br/><hr><br/>";
        rules += "<p>You are part of a group of bandits aiming to become the richest in the Old West. Your goal is to earn" +
                " as much money as possible by collecting loot bags (of variable worth), jewels (worth $500 each) and " +
                "stronghold boxes (worth $1000) each.</p><br/>";
        rules += "<p>The game is played over several rounds, each with 2 phases: planning and then executing actions.</p><br/>";
        rules += "<p>In the <b>PLANNING</b> phase (Schemin'), players take turns playing cards from their hands (or drawing up to 3 at a time from their deck)," +
                " according to the turn rules in each round, which can be: playing the card normally, playing it face down, " +
                "double turn (each player takes 2 actions in a row), or reversed turn (player order is reversed). All cards " +
                "played are stacked on top of each other.</p><br/>";
        rules += "<p><b>CARD</b> actions are fixed for all players and they are: " +
                "<ul><li>move left/right: distance 1 inside the train, and up to 3 on top of the train.</li>" +
                "<li>move up/down</li>" +
                "<li>collect loot: no effect if there is no loot in player's location, the player chooses later what type of loot they collect if more than 1 available in their location</li>" +
                "<li>move Marshal: The Marshal only moves inside the train, 1  car at a time, and shoots neutral bullets to all players it encounters, forcing them to move to the top of the train.</li>" +
                "<li>shoot: Shooting adds a bullet card into the target's deck; a player has 6 bullet cards to spend, shooting more has no effect;  the player who shot most bullets earns 1000 bonus points at the end (if tied, all tied receive the bonus).</li>" +
                "<li>punch: Punching causes the target player to drop loot (type being the puncher's choice) and moves target player 1 car horizontally.</li>" +
                "</ul></p><br/>";
        rules += "<p>In the <b>ACTION EXECUTING</b> phase (Stealin'), the actions on the cards played in the first phase are " +
                "actually executed, in the order they were played. Invalid actions have no effect and are skipped.</p><br/>";
        rules += "<p>There are some rounds which have special events at the end, hover mouse over the round cards to see details. " +
                "The characters have special powers, hover mouse over the player's area to find out what they do.</p><br/>";
        rules += "<p>WIN: The player with most money (points) at the end wins. If tied, the player with least bullets received wins (if still tied, all tied players win).</p>";
        rules += "<hr><p><b>INTERFACE: </b> Find actions available at any time at the bottom of the screen. If icons/text " +
                "on the train and/or round cards are too small, you can zoom and pan around with the middle mouse button (wheel).</p>";
        rules += "</html>";
        return rules;
    }
}
