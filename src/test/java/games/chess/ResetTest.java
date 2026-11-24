package games.chess;

import core.AbstractPlayer;
import core.Game;
import games.chess.components.ChessPiece;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A Game object reused for several games (as in every tournament) must start each game from a clean board,
 * with no pieces left over from the previous game.
 */
public class ResetTest {

    @Test
    public void boardIsClearedBetweenGames() {
        ChessParameters params = new ChessParameters();
        params.setParameterValue("drawByRepetition", 3);
        Game game = params.instantiate();
        List<AbstractPlayer> players = List.of(new RandomPlayer(new Random(42)), new RandomPlayer(new Random(43)));

        for (int i = 0; i < 5; i++) {
            game.reset(players, 100L + i);
            ChessGameState state = (ChessGameState) game.getGameState();

            Set<ChessPiece> live = Collections.newSetFromMap(new IdentityHashMap<>());
            live.addAll(state.getPlayerPieces(0));
            live.addAll(state.getPlayerPieces(1));
            assertEquals("pieces in lists, game " + i, 32, live.size());

            int onBoard = 0;
            ChessPiece[][] board = state.getBoard().getBoard();
            for (int x = 0; x < 8; x++)
                for (int y = 0; y < 8; y++)
                    if (board[x][y] != null) {
                        onBoard++;
                        assertTrue("piece on board not in a piece list at " + x + "," + y + ", game " + i,
                                live.contains(board[x][y]));
                    }
            assertEquals("pieces on board, game " + i, 32, onBoard);
            assertEquals("white actions at start, game " + i, 20,
                    game.getForwardModel().computeAvailableActions(state).size());
            // only the starting position has been seen
            assertEquals("repetition counts, game " + i, List.of(1), new ArrayList<>(state.gameStateCounts.values()));

            game.run();
        }
    }
}
