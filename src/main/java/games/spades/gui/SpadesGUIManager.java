package games.spades.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.spades.SpadesGameState;
import games.spades.SpadesParameters;
import games.spades.actions.Bid;
import games.tricktaking.PlayCard;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import gui.views.RulesView;
import players.human.ActionController;
import players.human.HumanGUIPlayer;
import utilities.ImageIO;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Set;

/**
 * GUI Manager for Spades game providing a complete graphical interface.
 */
public class SpadesGUIManager extends AbstractGUIManager {
    
    // Layout constants
    private static final int WINDOW_WIDTH = 1000;
    private static final int WINDOW_HEIGHT = 700;
    
    // GUI components
    private SpadesPlayerView[] playerViews;
    private SpadesTrickView trickView;
    private SpadesScoreView scoreView;
    private SpadesGameState gameState;
    
    // Player highlighting
    private int activePlayer = -1;
    private Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);
    private Border[] playerBorders;
    
    public SpadesGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanID) {
        super(parent, game, ac, humanID);
        
        if (game != null && game.getGameState() instanceof SpadesGameState) {
            this.gameState = (SpadesGameState) game.getGameState();
            setupGUI();
        }
    }
    
    private void setupGUI() {
        if (parent == null) return;
        
        // Set background
        parent.setBackground(ImageIO.GetInstance().getImage("data/FrenchCards/table-background.jpg"));
        
        // Create tabbed pane for different views
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setOpaque(false);
        
        // Main game panel
        JPanel mainPanel = createMainGamePanel();
        tabbedPane.add("Game", mainPanel);
        
        RulesView.addTabs(tabbedPane, GameType.Spades, gameState.getGameParameters(), WINDOW_HEIGHT * 2/3);
        
        parent.setLayout(new BorderLayout());
        parent.add(tabbedPane, BorderLayout.CENTER);
        parent.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        parent.revalidate();
        parent.repaint();
    }
    
    private JPanel createMainGamePanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setOpaque(false);
        
        // Get game parameters
        SpadesParameters params = (SpadesParameters) gameState.getGameParameters();
        String dataPath = "data/FrenchCards/";

        int nPlayers = gameState.getNPlayers();
        // Initialize player views
        playerViews = new SpadesPlayerView[nPlayers];
        playerBorders = new Border[nPlayers];
        
        // Create center area with trick view and score
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        
        // Trick view in center
        trickView = new SpadesTrickView(dataPath);
        centerPanel.add(trickView, BorderLayout.CENTER);
        
        // Score view on the right
        scoreView = new SpadesScoreView();
        centerPanel.add(scoreView, BorderLayout.EAST);
        
        // Create player areas around the center
        JPanel gameArea = new JPanel(new BorderLayout());
        gameArea.setOpaque(false);
        
        // Create player panels for each position
        for (int i = 0; i < nPlayers; i++) {
            SpadesPlayerView playerView = new SpadesPlayerView(gameState.getPlayerHands().get(i), i, dataPath);
            playerViews[i] = playerView;
            
            // Create border with player info
            String playerName = "Player " + i;
            if (game.getPlayers() != null && i < game.getPlayers().size()) {
                String agentName = game.getPlayers().get(i).toString();
                playerName += " [" + agentName + "]";
            }
            
            TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                playerName,
                TitledBorder.CENTER,
                TitledBorder.BELOW_BOTTOM
            );
            playerBorders[i] = border;
            playerView.setBorder(border);
            
            // Position players around the game area
            String position = switch (i) {
                case 0 -> BorderLayout.SOUTH;  // Bottom
                case 1 -> BorderLayout.WEST;   // Left
                case 2 -> BorderLayout.NORTH;  // Top
                case 3 -> BorderLayout.EAST;   // Right
                default -> BorderLayout.CENTER;
            };

            gameArea.add(playerView, position);
        }
        
        gameArea.add(centerPanel, BorderLayout.CENTER);
        
        // Info panel at top
        JPanel infoPanel = createGameStateInfoPanel("Spades", gameState, WINDOW_WIDTH, defaultInfoPanelHeight);
        
        // Action panel at bottom
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], WINDOW_WIDTH, defaultActionPanelHeight);
        
        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(gameArea, BorderLayout.CENTER);
        mainPanel.add(actionPanel, BorderLayout.SOUTH);
        
        return mainPanel;
    }
    
    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof SpadesGameState)) return;
        
        this.gameState = (SpadesGameState) gameState;
        
        // Update active player highlighting
        int currentPlayer = gameState.getCurrentPlayer();
        if (currentPlayer != activePlayer) {
            // Remove old highlighting
            if (activePlayer >= 0 && activePlayer < playerViews.length) {
                playerViews[activePlayer].setActivePlayer(false);
                playerViews[activePlayer].setBorder(playerBorders[activePlayer]);
            }
            
            // Add new highlighting
            activePlayer = currentPlayer;
            if (activePlayer >= 0 && activePlayer < playerViews.length) {
                playerViews[activePlayer].setActivePlayer(true);
                Border compound = BorderFactory.createCompoundBorder(highlightActive, playerBorders[activePlayer]);
                playerViews[activePlayer].setBorder(compound);
            }
        }
        
        // Update all components
        if (playerViews != null) {
            for (int i = 0; i < playerViews.length && i < this.gameState.getPlayerHands().size(); i++) {
                if (playerViews[i] != null) {
                    playerViews[i].setDeck(this.gameState.getPlayerHands().get(i));
                    // Show cards for human players
                    playerViews[i].setVisible(showHiddenInfo(this.gameState, i));
                }
            }
        }
        
        if (trickView != null) {
            trickView.updateTrick(this.gameState);
        }
        if (scoreView != null) {
            scoreView.updateGameState(this.gameState);
        }
        
        updateGameStateInfo(gameState);
        
        if (parent != null) {
            parent.repaint();
        }
    }
    
    @Override
    protected void updateActionButtons(AbstractPlayer current, AbstractGameState gameState) {
        if (!(current instanceof HumanGUIPlayer) || !(gameState instanceof SpadesGameState)) {
            return;
        }
        
        SpadesGameState spadesState = (SpadesGameState) gameState;
        
        // Clear existing actions
        for (ActionButton button : actionButtons) {
            button.setVisible(false);
        }
        
        // Get available actions
        List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(gameState);
        
        int buttonIndex = 0;
        for (AbstractAction action : actions) {
            if (buttonIndex >= actionButtons.length) break;
            
            ActionButton button = actionButtons[buttonIndex];
            button.setButtonAction(action, gameState);
            
            // Set button text based on action type
            if (action instanceof Bid) {
                Bid bidAction = (Bid) action;
                if (bidAction.bidAmount == 0) {
                    button.setText("Bid Nil");
                } else {
                    button.setText("Bid " + bidAction.bidAmount);
                }
            } else if (action instanceof PlayCard<?> playAction) {
                button.setText("Play " + playAction.card.toString());
            } else {
                button.setText(action.toString());
            }
            
            button.setVisible(true);
            buttonIndex++;
        }
    }
    
    @Override
    public int getMaxActionSpace() {
        return 14; // Max 14 bids (0-13) or 13 cards
    }
} 