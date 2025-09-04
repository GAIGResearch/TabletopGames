package games.gofish.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.gofish.GoFishParameters;
import games.gofish.GoFishGameState;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.Set;

public class GoFishGUIManager extends AbstractGUIManager {
    public static final int playerAreaWidth = 300;
    public static final int playerAreaHeight = 130;
    public static final int cardWidth = 80;
    public static final int cardHeight = 110;

    GoFishPlayerView[] playerHands;
    GoFishDeckView drawPile;
    int activePlayer = -1;
    Border highlightActive = BorderFactory.createLineBorder(new Color(0, 120, 200), 3);
    Border[] playerBorders;

    public GoFishGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanID) {
        super(parent, game, ac, humanID);

        AbstractGameState gs = game.getGameState();
        GoFishGameState state = (GoFishGameState) gs;
        int nPlayers = state.getNPlayers();
        activePlayer = state.getCurrentPlayer();

        this.width = playerAreaWidth * 3;
        this.height = playerAreaHeight * 3;

        JPanel mainGameArea = new JPanel(new BorderLayout());
        playerHands = new GoFishPlayerView[nPlayers];
        playerBorders = new Border[nPlayers];

        String[] positions = {BorderLayout.NORTH, BorderLayout.EAST, BorderLayout.SOUTH, BorderLayout.WEST};
        JPanel[] sides = {new JPanel(), new JPanel(), new JPanel(), new JPanel()};
        int next = 0;

        for (int i = 0; i < nPlayers; i++) {
            GoFishPlayerView pv = new GoFishPlayerView(state.getPlayerHands().get(i), i, humanID);
            String agentName = game.getPlayers().get(i).getClass().getSimpleName();
            TitledBorder border = BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(), "Player " + i + " [" + agentName + "]"
            );
            playerBorders[i] = border;
            pv.setBorder(border);
            playerHands[i] = pv;

            sides[next].setLayout(new GridBagLayout());
            sides[next].add(pv);
            next = (next + 1) % 4;
        }

        for (int i = 0; i < sides.length; i++) {
            mainGameArea.add(sides[i], positions[i]);
        }

        // Create draw pile using TAG's default card system
        drawPile = new GoFishDeckView(-1, state.getDrawDeck(), false, null,
                new Rectangle(0, 0, cardWidth, cardHeight));
        JPanel center = new JPanel(new GridBagLayout());
        center.add(drawPile);
        mainGameArea.add(center, BorderLayout.CENTER);

        JPanel infoPanel = createGameStateInfoPanel("Go Fish", gs, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, false, true, null, null, null);

        parent.setLayout(new BorderLayout());
        parent.add(infoPanel, BorderLayout.NORTH);
        parent.add(mainGameArea, BorderLayout.CENTER);
        parent.add(actionPanel, BorderLayout.SOUTH);
        parent.revalidate();
    }

    @Override
    public void _update(AbstractPlayer player, AbstractGameState gameState) {
        GoFishGameState state = (GoFishGameState) gameState;
        for (int i = 0; i < state.getNPlayers(); i++) {
            playerHands[i].update(state);
            boolean isVisible = i == gameState.getCurrentPlayer()
                    || humanPlayerIds.contains(i)
                    || gameState.getCoreGameParameters().alwaysDisplayFullObservable;
            playerHands[i].playerHandView.setFront(isVisible);

            if (i == gameState.getCurrentPlayer()) {
                Border compound = BorderFactory.createCompoundBorder(highlightActive, playerBorders[i]);
                playerHands[i].setBorder(compound);
            } else {
                playerHands[i].setBorder(playerBorders[i]);
            }
        }
        drawPile.updateComponent(state.getDrawDeck());
    }

    @Override
    public int getMaxActionSpace() {
        return 12;
    }
}