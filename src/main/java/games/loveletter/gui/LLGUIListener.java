package games.loveletter.gui;

import core.CoreConstants;
import core.Game;
import evaluation.listeners.IGameListener;
import evaluation.metrics.Event;
import games.loveletter.LoveLetterForwardModel;
import games.loveletter.LoveLetterGameState;
import gui.GamePanel;

import javax.swing.*;
import java.util.HashSet;
import java.util.Set;

public class LLGUIListener implements IGameListener {
    final LoveLetterForwardModel fm;
    final GamePanel parent;
    final LoveLetterPlayerView[] playerHands;

    public LLGUIListener(LoveLetterForwardModel fm, GamePanel parent, LoveLetterPlayerView[] playerHands) {
        this.fm = fm;
        this.parent = parent;
        this.playerHands = playerHands;
    }

    @Override
    public void onEvent(Event event) {
        if (event.type == Event.GameEvent.ROUND_OVER) {
            // Paint final state of previous round, showing all hands
            LoveLetterGameState llgs = (LoveLetterGameState) event.state;

            Set<Integer> winners = new HashSet<>();

            // Show all hands
            for (int i = 0; i < llgs.getNPlayers(); i++) {
                if (llgs.getPlayerResults()[i] == CoreConstants.GameResult.WIN_GAME || llgs.getPlayerResults()[i] == CoreConstants.GameResult.WIN_ROUND || llgs.getPlayerResults()[i] == CoreConstants.GameResult.DRAW_ROUND)
                    winners.add(i);
                playerHands[i].update(llgs, true);
            }
            // Repaint
            parent.repaint();

            // Message for pause and clarity
            JOptionPane.showMessageDialog(parent, "Round over! Winners: " + winners.toString() + ". Next round begins!");
        }
    }

    @Override
    public void report() {

    }

    @Override
    public void setGame(Game game) {

    }

    @Override
    public Game getGame() {
        return null;
    }
}
