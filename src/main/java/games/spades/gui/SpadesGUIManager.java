package games.spades.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.spades.SpadesGameState;
import games.spades.SpadesParameters;
import games.spades.actions.Bid;
import games.spades.actions.PlayCard;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
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
        
        // Rules panel
        JPanel rulesPanel = createRulesPanel();
        tabbedPane.add("Rules", rulesPanel);
        
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
        
        // Initialize player views
        playerViews = new SpadesPlayerView[4];
        playerBorders = new Border[4];
        
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
        for (int i = 0; i < 4; i++) {
            SpadesPlayerView playerView = new SpadesPlayerView(gameState.getPlayerHands().get(i), i, dataPath);
            playerViews[i] = playerView;
            
            // Create border with player info
            String playerName = "Player " + i;
            if (game.getPlayers() != null && i < game.getPlayers().size()) {
                String[] agentParts = game.getPlayers().get(i).getClass().getSimpleName().split("\\.");
                String agentName = agentParts[agentParts.length - 1];
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
            String position;
            switch (i) {
                case 0: position = BorderLayout.SOUTH; break;  // Bottom
                case 1: position = BorderLayout.WEST; break;   // Left
                case 2: position = BorderLayout.NORTH; break;  // Top
                case 3: position = BorderLayout.EAST; break;   // Right
                default: position = BorderLayout.CENTER; break;
            }
            
            gameArea.add(playerView, position);
        }
        
        gameArea.add(centerPanel, BorderLayout.CENTER);
        
        // Info panel at top
        JPanel infoPanel = createGameStateInfoPanel("Spades", gameState, WINDOW_WIDTH, defaultInfoPanelHeight);
        
        // Action panel at bottom
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], WINDOW_WIDTH, defaultActionPanelHeight, false);
        
        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(gameArea, BorderLayout.CENTER);
        mainPanel.add(actionPanel, BorderLayout.SOUTH);
        
        return mainPanel;
    }
    
    private JPanel createRulesPanel() {
        JPanel rulesPanel = new JPanel();
        rulesPanel.setBackground(new Color(43, 108, 25, 111));
        
        JLabel rulesLabel = new JLabel(getRulesText());
        rulesLabel.setVerticalAlignment(SwingConstants.TOP);
        
        JScrollPane scrollPane = new JScrollPane(rulesLabel);
        scrollPane.setPreferredSize(new Dimension(WINDOW_WIDTH * 2/3, WINDOW_HEIGHT * 2/3));
        
        rulesPanel.add(scrollPane);
        return rulesPanel;
    }
    
    private String getRulesText() {
        return "<html><center><h1>Spades</h1></center><br/><hr><br/>" +
               "<p>Spades is a trick-taking card game for 4 players in 2 partnerships.</p>" +
               "<ul>" +
               "<li><b>Teams:</b> Players 0 & 2 vs Players 1 & 3</li>" +
               "<li><b>Goal:</b> First team to reach 500 points wins</li>" +
               "<li><b>Trump:</b> Spades are always trump cards</li>" +
               "</ul>" +
               "<h3>Bidding Phase:</h3>" +
               "<ul>" +
               "<li>Each player bids the number of tricks they expect to win (0-13)</li>" +
               "<li>Bid of 0 is called 'Nil' - attempting to win no tricks</li>" +
               "<li>All players must bid before playing begins</li>" +
               "</ul>" +
               "<h3>Playing Phase:</h3>" +
               "<ul>" +
               "<li>Players must follow suit if possible</li>" +
               "<li>Spades beat all other suits (trump)</li>" +
               "<li>Cannot lead spades until 'broken' (someone plays a spade)</li>" +
               "<li>Exception: Can lead spades if only spades remaining</li>" +
               "</ul>" +
               "<h3>Scoring:</h3>" +
               "<ul>" +
               "<li><b>Made bid:</b> 10 points per bid trick + 1 per overtrick</li>" +
               "<li><b>Failed bid:</b> Lose 10 points per bid trick</li>" +
               "<li><b>Sandbags:</b> Every 10 overtricks = 100 point penalty</li>" +
               "<li><b>Nil:</b> +100 if successful, -100 if failed</li>" +
               "</ul>" +
               "<hr><p><b>INTERFACE:</b> Click cards to play them. Use action buttons to bid.</p>" +
               "</html>";
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
                    playerViews[i].setVisible(humanPlayerIds.contains(i) || i == 0); // Always show player 0 for demo
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
            } else if (action instanceof PlayCard) {
                PlayCard playAction = (PlayCard) action;
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