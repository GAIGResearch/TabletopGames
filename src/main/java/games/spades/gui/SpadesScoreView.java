package games.spades.gui;

import games.spades.SpadesGameState;

import javax.swing.*;
import java.awt.*;

/**
 * GUI component for displaying scores, bids, and tricks taken in Spades.
 */
public class SpadesScoreView extends JPanel {
    
    private SpadesGameState gameState;
    
    public SpadesScoreView() {
        setOpaque(false);
        setPreferredSize(new Dimension(300, 200));
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw background
        g2d.setColor(new Color(240, 240, 240, 200));
        g2d.fillRoundRect(5, 5, getWidth() - 10, getHeight() - 10, 15, 15);
        g2d.setColor(Color.BLACK);
        g2d.drawRoundRect(5, 5, getWidth() - 10, getHeight() - 10, 15, 15);
        
        if (gameState == null) {
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            g2d.drawString("No game data", 20, 30);
            return;
        }
        
        int y = 25;
        int lineHeight = 18;
        
        // Title
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(Color.BLUE);
        g2d.drawString("SPADES SCORECARD", 15, y);
        y += lineHeight + 5;
        
        // Team scores
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.setColor(Color.BLACK);
        g2d.drawString("TEAM SCORES:", 15, y);
        y += lineHeight;
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        g2d.setColor(new Color(0, 100, 0));
        g2d.drawString("Team 1 (P0 & P2): " + gameState.getTeamScore(0), 20, y);
        y += lineHeight;
        
        g2d.setColor(new Color(100, 0, 0));
        g2d.drawString("Team 2 (P1 & P3): " + gameState.getTeamScore(1), 20, y);
        y += lineHeight + 5;
        
        // Current round bids and tricks
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.setColor(Color.BLACK);
        g2d.drawString("CURRENT ROUND:", 15, y);
        y += lineHeight;
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        
        // Show bids and tricks for each player
        String[] playerNames = {"P0", "P1", "P2", "P3"};
        Color[] playerColors = {
            new Color(0, 100, 0),    // P0 - Green
            new Color(100, 0, 0),    // P1 - Red  
            new Color(0, 100, 0),    // P2 - Green
            new Color(100, 0, 0)     // P3 - Red
        };
        
        for (int i = 0; i < 4; i++) {
            try {
                g2d.setColor(playerColors[i]);
                
                int bid = gameState.getPlayerBid(i);
                int tricks = gameState.getTricksTaken(i);
                
                String bidText = (bid == -1) ? "?" : (bid == 0 ? "Nil" : String.valueOf(bid));
                String status = (bid == -1) ? "Bidding..." : tricks + "/" + bidText;
                
                g2d.drawString(playerNames[i] + ": " + status, 20, y);
                y += lineHeight;
            } catch (Exception e) {
                // Skip this player if there's an error accessing game state
                g2d.drawString(playerNames[i] + ": ?", 20, y);
                y += lineHeight;
            }
        }
        
        y += 5;
        
        // Sandbags
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.setColor(Color.ORANGE);
        g2d.drawString("Sandbags:", 15, y);
        y += 15;
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.setColor(new Color(0, 100, 0));
        g2d.drawString("Team 1: " + gameState.getTeamSandbags(0), 20, y);
        y += 12;
        
        g2d.setColor(new Color(100, 0, 0));
        g2d.drawString("Team 2: " + gameState.getTeamSandbags(1), 20, y);
        y += 12;
        
        // Game phase
        g2d.setFont(new Font("Arial", Font.ITALIC, 10));
        g2d.setColor(Color.GRAY);
        String phase = gameState.getSpadesGamePhase().name();
        g2d.drawString("Phase: " + phase, 15, y);
        
        // Spades broken indicator
        if (gameState.isSpadesBroken()) {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString("♠ SPADES BROKEN ♠", 15, y + 15);
        }
    }
    
    public void updateGameState(SpadesGameState gameState) {
        // Create a defensive copy or reference
        this.gameState = gameState;
        // Use SwingUtilities.invokeLater to ensure painting happens on EDT
        SwingUtilities.invokeLater(this::repaint);
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(250, 200);
    }
} 