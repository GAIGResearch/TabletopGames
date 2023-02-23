package games.loveletter.gui;

import core.CoreConstants;
import evaluation.listeners.GameListener;
import evaluation.metrics.Event;
import games.loveletter.LoveLetterForwardModel;
import games.loveletter.LoveLetterGameState;
import gui.GamePanel;

import javax.swing.*;
import java.util.Set;

public class LLGUIListener extends GameListener {
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

            // Get winners
            int playersAlive = 0;
            int soleWinner = -1;
            for (int i = 0; i < event.state.getNPlayers(); i++) {
                if (llgs.getPlayerResults()[i] != CoreConstants.GameResult.LOSE && llgs.getPlayerHandCards().get(i).getSize() > 0) {
                    playersAlive += 1;
                    soleWinner = i;
                }
            }
            Set<Integer> winners = fm.getWinners(llgs, playersAlive, soleWinner);

            // Show all hands
            for (int i = 0; i < llgs.getNPlayers(); i++) {
                playerHands[i].update(llgs, true);
            }
            // Repaint
            parent.repaint();

            // Message for pause and clarity
            JOptionPane.showMessageDialog(parent, "Round over! Winners: " + winners.toString() + ". Next round begins!");
        }
    }
}
