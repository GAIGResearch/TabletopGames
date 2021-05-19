package games.coltexpress.gui;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.interfaces.IGamePhase;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressParameters;
import games.coltexpress.components.Compartment;
import players.human.ActionController;
import players.human.HumanGUIPlayer;
import utilities.ImageIO;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Collection;
import java.util.List;

import static core.CoreConstants.ALWAYS_DISPLAY_CURRENT_PLAYER;
import static core.CoreConstants.ALWAYS_DISPLAY_FULL_OBSERVABLE;
import static games.coltexpress.ColtExpressGameState.ColtExpressGamePhase.ExecuteActions;

public class ColtExpressGUI extends AbstractGUI {
    // Settings for display area sizes
    final static int playerAreaWidth = 400;
    final static int playerAreaWidthScroll = 290;
    final static int playerAreaHeight = 150;
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
    // ID of human player
    int humanID;
    // Border highlight of active player
    Border highlightActive = BorderFactory.createLineBorder(new Color(220, 169, 11), 3);
    Border[] playerViewBorders;

    public ColtExpressGUI(Game game, ActionController ac, int humanID) {
        super(ac, 25);
        this.humanID = humanID;

        if (game != null) {
            AbstractGameState gameState = game.getGameState();
            if (gameState != null) {

                JTabbedPane pane = new JTabbedPane();
                UIManager.put("TabbedPane.contentOpaque", false);
                JPanel main = new JPanel();
                main.setOpaque(false);
                main.setLayout(new BorderLayout());
                JPanel rules = new JPanel();
                pane.add("Main", main);
                pane.add("Rules", rules);

                ColtExpressGameState cegs = (ColtExpressGameState) gameState;
                ColtExpressParameters cep = (ColtExpressParameters) gameState.getGameParameters();

                List<Compartment> train = ((ColtExpressGameState) gameState).getTrainCompartments();
                trainView = new ColtExpressTrainView(train, cep.getDataPath(), cegs.getPlayerCharacters());
                trainView.setOpaque(false);
                plannedActions = new ColtExpressDeckView(cegs.getPlannedActions(), true, cep.getDataPath(), cegs.getPlayerCharacters());
                plannedActions.setOpaque(false);
                roundView = new ColtExpressRoundView(train, cep.getDataPath(), cegs.getPlayerCharacters());
                roundView.setOpaque(false);

                activePlayer = gameState.getCurrentPlayer();
                int nPlayers = gameState.getNPlayers();
                this.width = trainCarWidth*3/2*(train.size()+1) + playerAreaWidth;
                this.height = playerAreaHeight * (nPlayers+1) + defaultInfoPanelHeight + defaultActionPanelHeight;

                ScaledImage backgroundImage = new ScaledImage(ImageIO.GetInstance().getImage("data/coltexpress/bg.jpg"), width, height, this);
                setContentPane(backgroundImage);

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
                int next = 0;
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
                JComponent actionPanel = createActionPanel(new Collection[0], width, defaultActionPanelHeight, false);
                actionPanel.setOpaque(false);


                main.add(infoPanel, BorderLayout.NORTH);
                main.add(mainGameArea, BorderLayout.CENTER);
                main.add(actionPanel, BorderLayout.SOUTH);

                getContentPane().add(pane, BorderLayout.CENTER);
            }
        }

        setFrameProperties();
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
        gameInfo.add(turnOwner);
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
        historyContainer.getViewport().setOpaque(false);
        historyContainer.setPreferredSize(new Dimension(width/2 - 25, height));
        wrapper.add(historyContainer);
        return wrapper;
    }

    @Override
    protected JComponent createActionPanel(Collection[] highlights, int width, int height, boolean boxLayout) {
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
                    JOptionPane.showMessageDialog(this, "Planning phase over, execute actions!");
                } else {
                    JOptionPane.showMessageDialog(this, "New round! Time to plan actions!");
                }
            }
            currentGamePhase = gameState.getGamePhase();

            // Update decks and visibility
            ColtExpressGameState cegs = (ColtExpressGameState)gameState;
            for (int i = 0; i < gameState.getNPlayers(); i++) {
                playerHands[i].update((ColtExpressGameState) gameState, humanID);

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
            int activePlayer = (ALWAYS_DISPLAY_CURRENT_PLAYER || ALWAYS_DISPLAY_FULL_OBSERVABLE? player.getPlayerID(): player.getPlayerID()==humanID? player.getPlayerID():-1);
            plannedActions.informActivePlayer(player.getPlayerID());

            // Show planned actions from the first played
            if (gameState.getGamePhase() == ExecuteActions) {
                plannedActions.setFirstOnTop(true);
            } else {
                plannedActions.setFirstOnTop(false);
            }

            // Update train view
            trainView.update(cegs);
            roundView.update(cegs);

            // Update actions
            if (player instanceof HumanGUIPlayer) {
                updateActionButtons(player, gameState);
            }
        }
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public static class ScaledImage extends JPanel {
        Image img;
        int w, h;
        JFrame frame;

        public ScaledImage(Image img, int w, int h, JFrame frame) {
            this.img = img;
            this.w = w;
            this.h = h;
            this.frame = frame;
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setComposite(AlphaComposite.SrcOver.derive(0.3f));

            Rectangle r = frame.getBounds();
            h = r.height;
            w = r.width;

            int picW = img.getWidth(null);
            int picH = img.getHeight(null);
            double scale = w*1.0/picW;
            double s2 = h*1.0/picH;
            if (s2 > scale) scale = s2;
            g2d.drawImage(img, 0, 0, (int)(picW * scale), (int)(picH * scale), null);
            g2d.dispose();
        }
    }
}
