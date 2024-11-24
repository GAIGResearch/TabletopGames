package games.seasaltpaper.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;
import utilities.ImageIO;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Set;

public class SSPGUIManager extends AbstractGUIManager {

    final static int cardWidth = 120;
    final static int cardHeight = 150;
    public SSPGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
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
//        mainGameArea.add(drawDiscardPanel, BorderLayout.CENTER);

        JLabel testCard = new JLabel();
        String dataPath = "data/seasaltpaper/";
        Image iconSuite = ImageIO.GetInstance().getImage(dataPath + "boat.png");

        BufferedImage cardFront = new BufferedImage(120, 150, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = cardFront.createGraphics();
        g.setColor(new Color(231, 82, 82));
        g.fillRect(0, 0, cardFront.getWidth(), cardFront.getHeight());
        g.drawImage(iconSuite.getScaledInstance(60, 75, Image.SCALE_DEFAULT), 30, 30, null);
//        ImageIcon icon = new ImageIcon(iconSuite.getScaledInstance(60, 75, Image.SCALE_DEFAULT));
        g.setColor(Color.BLACK);
        g.setFont(new Font( "SansSerif", Font.BOLD, 12 ));
        g.drawString("[1, 3, 5, 7]", 5, 15);
        g.dispose();
        ImageIcon icon = new ImageIcon(cardFront);
        testCard.setIcon(icon);
        mainGameArea.add(testCard, BorderLayout.CENTER);

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
