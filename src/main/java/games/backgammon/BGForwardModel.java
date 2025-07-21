package games.backgammon;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.*;
import gametemplate.actions.GTAction;

import java.util.*;

/**
 * <p>The forward model contains all the game rules and logic. It is mainly responsible for declaring rules for:</p>
 * <ol>
 *     <li>Game setup</li>
 *     <li>Actions available to players in a given game state</li>
 *     <li>Game events or rules applied after a player's action</li>
 *     <li>Game end</li>
 * </ol>
 */
public class BGForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        BGGameState gameState = (BGGameState) firstState;
        BGParameters bgp = (BGParameters) firstState.getGameParameters();

        gameState.counters = new ArrayList<>(bgp.boardSize + 1);
        for (int i = 0; i <= bgp.boardSize; i++) {
            gameState.counters.add(new ArrayList<>());
        }
        // we use the convention that 0 is the 'bar'. Once a piece is borne off it cannot reenter
        // the game, and so is no longer tracked as a Token (just a count of the number borne off)
        // Distribute counters based on starting positions
        for (int i = 0; i < bgp.startingAt24; i++) {
            tokensAt(gameState, 24);
        }
        for (int i = 0; i < bgp.startingAt13; i++) {
            tokensAt(gameState, 13);
        }
        for (int i = 0; i < bgp.startingAt8; i++) {
            tokensAt(gameState, 8);
        }
        for (int i = 0; i < bgp.startingAt6; i++) {
            tokensAt(gameState, 6);
        }
        for (int i = 0; i < bgp.startingAtBar; i++) {
            tokensAt(gameState, 0);
        }

        gameState.playerTrackMapping = new int[2][bgp.boardSize];
        for (int i = 0; i < bgp.boardSize; i++) {
            gameState.playerTrackMapping[1][i] = i + 1;
            gameState.playerTrackMapping[0][i] = bgp.boardSize - i;
        }
        gameState.piecesBorneOff = new int[2];
        gameState.dice = new Dice[bgp.diceNumber];
        for (int i = 0; i < bgp.diceNumber; i++) {
            gameState.dice[i] = new Dice(bgp.diceSides);
        }
        gameState.diceUsed = new boolean[bgp.diceNumber];
        gameState.rollDice();

        gameState.blots = new int[2];
    }

    private void tokensAt(BGGameState state, int space) {
        Token whiteToken = new Token("White");
        whiteToken.setOwnerId(0);
        Token blackToken = new Token("Black");
        blackToken.setOwnerId(1);
        state.counters.get(space).add(whiteToken);
        state.counters.get(24 - space + 1).add(blackToken);
    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     *
     * @return - List of AbstractAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        List<AbstractAction> actions = new ArrayList<>();
        // We create the set of possible actions
        // For each available dice value we consider each point that has player tokens on
        // and add the possible moves to the list of actions
        // (removing any moves that would move to a point occupied by two or more opponent tokens)
        BGGameState bgs = (BGGameState) gameState;
        BGParameters bgp = (BGParameters) gameState.getGameParameters();
        int playerId = bgs.getCurrentPlayer();
        // just look at unique dice values
        int[] diceAvailable = Arrays.stream(bgs.getAvailableDiceValues())
                .distinct()
                .toArray();
        int boardSize = bgp.boardSize;

        if (bgs.getPiecesOnBar(playerId) > 0) {
            // player has pieces on the bar, so they can only move those
            for (int i : diceAvailable) {
                int physicalIndex = bgs.getPhysicalSpace(playerId, i - 1);
                if (bgs.getPiecesOnPoint(1 - playerId, physicalIndex) < 2) {
                    // we can move to this point
                    actions.add(new MovePiece(0, physicalIndex));
                }
            }
            if (actions.isEmpty())
                actions.add(new DoNothing());
            return actions;
        }

        if (bgs.allPiecesOnHomeBoard(playerId)) {
            // now we can bear off, so add these possibilities to the actions list
            int maxDieValue = Arrays.stream(diceAvailable).max().orElse(0);
            for (int i = 0; i < maxDieValue; i++) {
                int physicalIndex = bgs.getPhysicalSpace(playerId, boardSize - i  - 1);
                if (bgs.getPiecesOnPoint(playerId, physicalIndex) > 0) {
                    // we can bear off this piece
                    actions.add(new MovePiece(physicalIndex, -1));
                }
            }
        }

        for (int i : diceAvailable) {
            for (int from = 0; from < boardSize - i; from++) {
                int physicalIndexFrom = bgs.getPhysicalSpace(playerId, from);
                if (bgs.getPiecesOnPoint(playerId, physicalIndexFrom) > 0) {
                    int physicalIndexTo = bgs.getPhysicalSpace(playerId, from + i);
                    if (bgs.getPiecesOnPoint(1 - playerId, physicalIndexTo) < 2) {
                        // we can move to this point
                        actions.add(new MovePiece(physicalIndexFrom, physicalIndexTo));
                    }
                }
            }
        }
        if (actions.isEmpty()) {
            // no moves available, so we can only do nothing
            actions.add(new DoNothing());
        }
        return actions;
    }

    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        // a player's turn ends when they have used all the dice values, or have no valid moves
        BGGameState bgs = (BGGameState) currentState;
        BGParameters bgp = (BGParameters) currentState.getGameParameters();
        // check for game end
        if (bgs.piecesBorneOff[0] == bgp.piecesPerPlayer || bgs.piecesBorneOff[1] == bgp.piecesPerPlayer) {
            endGame(bgs);
        } else {
            int[] diceAvailable = bgs.getAvailableDiceValues();
            if (diceAvailable.length == 0 || computeAvailableActions(currentState).stream().noneMatch(c -> c instanceof MovePiece)) {
                // end of turn: switch player
                bgs.rollDice();
                endPlayerTurn(bgs);  // default is to move to next player
                if (bgs.getCurrentPlayer() == 0) {
                    // if we are back to player 0, we can end the round
                    endRound(bgs);
                }
            }
        }
    }
}
