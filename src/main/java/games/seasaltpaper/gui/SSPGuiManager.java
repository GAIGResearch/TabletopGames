package games.seasaltpaper.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class SSPGuiManager extends AbstractGUIManager {
    public SSPGuiManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);

        this.width = 1000;
        this.height = 1000;

        AbstractGameState gameState = game.getGameState();

        JPanel mainGameArea = new JPanel();
        mainGameArea.setLayout(new BorderLayout());

        String[] locations = new String[]{BorderLayout.NORTH, BorderLayout.EAST, BorderLayout.SOUTH, BorderLayout.WEST};
        JPanel[] sides = new JPanel[]{new JPanel(), new JPanel(), new JPanel(), new JPanel()};

        //TODO: Make this generic for number of players
        for (int i=0; i<4; i++) {
            sides[i].setLayout(new BorderLayout());

            JButton playerLabel = new JButton("Player " + i);
            sides[i].add(playerLabel, BorderLayout.SOUTH);

            // TODO: This is where PlayerView goes
            JPanel playerHand = new JPanel();
            playerHand.setLayout(new FlowLayout());
            JButton handLabel = new JButton("Player Hand");
            JButton playedLabel = new JButton("Played cards");
            playerHand.add(handLabel);
            playerHand.add(playedLabel);
            sides[i].add(playerHand, BorderLayout.CENTER);

            mainGameArea.add(sides[i], locations[i]);
        }
        // TODO: this is where DeckView for draw pile and discard piles go
        JPanel drawDiscardPanel = new JPanel();
        drawDiscardPanel.setLayout(new FlowLayout());
        JButton drawPile = new JButton("Draw Pile");
        // TODO: make this generic for multiple number of discard piles (?)
        JButton discardPile1 = new JButton ("Discard Pile 1");
        JButton discardPile2 = new JButton ("Discard Pile 2");
        drawDiscardPanel.add(drawPile);
        drawDiscardPanel.add(discardPile1);
        drawDiscardPanel.add(discardPile2);
        mainGameArea.add(drawDiscardPanel, BorderLayout.CENTER);

        JPanel infoPanel = createGameStateInfoPanel("Sea Salt and Paper", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, false, true, null, null, null);

        parent.setLayout(new BorderLayout());
        parent.add(mainGameArea, BorderLayout.CENTER);
        parent.add(infoPanel, BorderLayout.NORTH);
        parent.add(actionPanel, BorderLayout.SOUTH);
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();

//        System.out.println("AAAAAAA GUI AAAAAAAA");

    }

    @Override
    public int getMaxActionSpace() {
        return 10;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {

    }
}
