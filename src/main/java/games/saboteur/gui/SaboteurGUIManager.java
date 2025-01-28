package games.saboteur.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.saboteur.SaboteurForwardModel;
import games.saboteur.SaboteurGameParameters;
import games.saboteur.SaboteurGameState;
import games.saboteur.actions.PlacePathCard;
import games.saboteur.actions.PlayMapCard;
import games.saboteur.actions.PlayRockFallCard;
import games.saboteur.actions.PlayToolCard;
import games.saboteur.components.ActionCard;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;
import utilities.ImageIO;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

public class SaboteurGUIManager extends AbstractGUIManager {
    //all subjected to change
    final static int playerAreaWidth = 200;
    final static int playerAreaHeight = 100;
    final static int boardSize = 500;

    SaboteurGameState gs;
    SaboteurForwardModel fm;
    SaboteurGameParameters params;

    // Currently active player
    int activePlayer = -1;
    int highlightPlayerIdx = 0;

    //Other elements
    SaboteurPlayerView[] playerHands;

    // Border highlight of active player
    Border highlightActive = BorderFactory.createLineBorder(new Color(220, 27, 67), 3);
    Border[] playerViewBorders, playerViewBordersHighlight;

    JLabel cardsInDeck;

    public SaboteurGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanID)
    {
        super(parent, game, ac, humanID);

        UIManager.put("TabbedPane.contentOpaque", false);
        UIManager.put("TabbedPane.opaque", false);
        UIManager.put("TabbedPane.tabsOpaque", false);

        if (game != null) {
            cardsInDeck = new JLabel("");

            AbstractGameState gameState = game.getGameState();
            fm = (SaboteurForwardModel) game.getForwardModel();

            if (gameState != null) {
                gs = (SaboteurGameState)gameState;
                params = (SaboteurGameParameters) gs.getGameParameters();
                JTabbedPane pane = new JTabbedPane();
                JPanel main = new JPanel();
                main.setOpaque(false);
                main.setLayout(new BorderLayout());
                JPanel rules = new JPanel();
                pane.add("Main", main);
                pane.add("Rules", rules);
                JLabel ruleText = new JLabel(getRuleText());
                rules.add(ruleText);

                // Initialise active player
                activePlayer = gameState.getCurrentPlayer();

                // Find required size of window
                int nPlayers = gameState.getNPlayers();
                int nHorizAreas = 4;
                double nVertAreas = Math.floor(nPlayers / 4.);
                this.width = playerAreaWidth * nHorizAreas;
                this.height = (int) (playerAreaHeight * nVertAreas) + boardSize;
                ruleText.setPreferredSize(new Dimension(width + 50, height + defaultInfoPanelHeight));

                parent.setBackground(ImageIO.GetInstance().getImage("data/loveletter/bg.png"));

                // Create main game area that will hold all game views
                playerHands = new SaboteurPlayerView[nPlayers];
                playerViewBorders = new Border[nPlayers];
                playerViewBordersHighlight = new Border[nPlayers];
                JPanel mainGameArea = new JPanel();
                mainGameArea.setLayout(new BoxLayout(mainGameArea, BoxLayout.Y_AXIS));
                mainGameArea.setOpaque(false);

                // Player hands go on the edges
                JPanel top = new JPanel();
                JPanel bottom = new JPanel();
                top.setLayout(new BoxLayout(top, BoxLayout.X_AXIS));
                bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
                for (int i = 0; i < nPlayers; i++) {
                    SaboteurPlayerView playerHand = new SaboteurPlayerView(this, gs, i, humanID);

                    // Get agent name
                    String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
                    String agentName = split[split.length - 1];

                    // Create border, layouts and keep track of this view
                    TitledBorder title = BorderFactory.createTitledBorder(
                            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + agentName + "]",
                            TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
                    playerViewBorders[i] = title;
                    playerViewBordersHighlight[i] = BorderFactory.createCompoundBorder(highlightActive, playerViewBorders[i]);
                    playerHand.setBorder(title);

                    JPanel where;
                    if (i / 4 < 1) where = top; else where = bottom;

                    where.setOpaque(false);
                    where.add(playerHand);
                    playerHands[i] = playerHand;
                    int p = i;
                    playerHands[i].addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            highlightPlayerIdx = p;
                        }
                    });
                }

                mainGameArea.add(top);
                mainGameArea.add(new SaboteurBoardView(this, gs));
                mainGameArea.add(bottom);

                // Add GUI listener
//                game.addListener(new LLGUIListener(fm, parent, playerHands));

                // Top area will show state information
                JPanel infoPanel = createGameStateInfoPanel("Saboteur", gameState, width, defaultInfoPanelHeight);
                infoPanel.setOpaque(false);
                // Bottom area will show actions available
                JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, false, false, this::onActionSelected, this::onMouseEnter, this::onMouseExit);
                actionPanel.setOpaque(false);

                main.add(infoPanel, BorderLayout.NORTH);
                main.add(mainGameArea, BorderLayout.CENTER);
                main.add(actionPanel, BorderLayout.SOUTH);

                parent.setLayout(new BorderLayout());
                parent.add(pane, BorderLayout.CENTER);
                parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + defaultCardHeight + 20));
                parent.revalidate();
                parent.setVisible(true);
                parent.repaint();
            }
        }
    }

    protected void onActionSelected(ActionButton actionButton) {
        gridHighlight = null;
        componentIDHighlight = -1;
        cardIdxHighlight = -1;
        actionCardHighlight = null;
    }

    Point gridHighlight = null;
    int componentIDHighlight = -1;
    int cardIdxHighlight = -1;
    ActionCard.ActionCardType actionCardHighlight = null;

    protected void onMouseEnter(ActionButton actionButton) {
        AbstractAction action = actionButton.getButtonAction();
        if (action instanceof PlacePathCard ppc) {
            gridHighlight = new Point(ppc.getX(), ppc.getY());
            componentIDHighlight = ppc.getValueID();
        } else if (action instanceof PlayMapCard pmc) {
            gridHighlight = new Point(pmc.getPosition().getX(), pmc.getPosition().getY());
            actionCardHighlight = ActionCard.ActionCardType.Map;
        } else if (action instanceof PlayRockFallCard) {
            actionCardHighlight = ActionCard.ActionCardType.RockFall;
        } else if (action instanceof PlayToolCard ptc) {
            cardIdxHighlight = ptc.getCardIdx();
        }
    }

    protected void onMouseExit(ActionButton actionButton) {
        gridHighlight = null;
        componentIDHighlight = -1;
        cardIdxHighlight = -1;
        actionCardHighlight = null;
    }

    @Override
    public int getMaxActionSpace() {
        return 1000;
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
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);
        gameInfo.add(cardsInDeck);

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
        historyContainer.getViewport().setBackground(new Color(229, 218, 209, 255));
        historyContainer.setPreferredSize(new Dimension(width/2 - 25, height));
        wrapper.add(historyContainer);
        return wrapper;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            cardsInDeck.setText("Cards in deck: " + ((SaboteurGameState)gameState).getDrawDeck().getSize());

            // Update active player highlight
            if (gameState.getCurrentPlayer() != activePlayer) {
                playerHands[activePlayer].setCardHighlight(-1);
                activePlayer = gameState.getCurrentPlayer();
            }

            // Update decks and visibility
            for (int i = 0; i < gameState.getNPlayers(); i++) {
                boolean front = i == gameState.getCurrentPlayer() && gameState.getCoreGameParameters().alwaysDisplayCurrentPlayer
                        || humanPlayerIds.contains(i)
                        || gameState.getCoreGameParameters().alwaysDisplayFullObservable;
                playerHands[i].update(front);

                // Highlight active player
                if (i == gameState.getCurrentPlayer()) {
                    playerHands[i].setBorder(playerViewBordersHighlight[i]);
                } else {
                    playerHands[i].setBorder(playerViewBorders[i]);
                }
            }
        }
    }


    private String getRuleText() {
        String rules = "<html><center><h1>Saboteur</h1></center><br/><hr><br/>";
        rules += "<p>Rules.</p><br/>";
        rules += "<hr><p><b>INTERFACE: </b> Find actions available at any time at the bottom of the screen. </p>";
        rules += "</html>";
        return rules;
    }
}
