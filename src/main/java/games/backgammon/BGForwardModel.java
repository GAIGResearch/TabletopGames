package games.backgammon;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.*;
import gametemplate.actions.GTAction;

import java.util.*;

import static games.backgammon.BGParameters.EntryRule.Bar;
import static games.backgammon.BGParameters.EntryRule.Home;

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
            tokensAt(gameState, 24, bgp);
        }
        for (int i = 0; i < bgp.startingAt13; i++) {
            tokensAt(gameState, 13, bgp);
        }
        for (int i = 0; i < bgp.startingAt8; i++) {
            tokensAt(gameState, 8, bgp);
        }
        for (int i = 0; i < bgp.startingAt6; i++) {
            tokensAt(gameState, 6, bgp);
        }
        for (int i = 0; i < bgp.startingAtBar; i++) {
            tokensAt(gameState, 0, bgp);
        }

        // The convention is that all player pieces enter the board at the most distant point, and move towards the home board.
        // (if route == Counter)
        // Hence a piece enters at 24 when coming from the bar, and moves to 23, 22, ..., 1, 0 (bearing off).
        // Players move 'naturally' on the counters' list so that they move from index 0 through to the end
        // Hence, playerTrackMapping is {24...1} for white, and {1...24} for black.
        gameState.playerTrackMapping = bgp.route == BGParameters.Route.CommonHalfA ?
                new int[2][bgp.boardSize - bgp.homeBoardSize] :
                new int[2][bgp.boardSize];
        for (int i = 0; i < bgp.boardSize; i++) {
            switch (bgp.route) {
                case Counter -> {
                    gameState.playerTrackMapping[0][i] = bgp.boardSize - i;
                    gameState.playerTrackMapping[1][i] = i + 1;
                }
                case Common -> {  // both players race in the same direction
                    gameState.playerTrackMapping[0][i] = bgp.boardSize - i;
                    gameState.playerTrackMapping[1][i] = bgp.boardSize - i;
                }
                case CommonHalfA -> {
                    // in this case we use a different first 6 spaces for each player
                    gameState.playerTrackMapping[0][i] = bgp.boardSize - i - bgp.homeBoardSize;
                    gameState.playerTrackMapping[1][i] = bgp.boardSize - i - bgp.homeBoardSize;
                }
            }

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

    private void tokensAt(BGGameState state, int space, BGParameters params) {
        BGParameters.Route route = params.route;
        int boardSize = params.boardSize;
        Token whiteToken = new Token("White");
        whiteToken.setOwnerId(0);
        Token blackToken = new Token("Black");
        blackToken.setOwnerId(1);
        state.counters.get(space).add(whiteToken);
        switch (route) {
            case Counter -> state.counters.get(boardSize - space + 1).add(blackToken);
            case Common, CommonHalfA -> state.counters.get(space).add(blackToken);
        }
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

        // Moves off the bar first
        if (bgs.getPiecesOnBar(playerId) > 0) {
            for (int i : diceAvailable) {
                int physicalIndex = bgs.getPhysicalSpace(playerId, i - 1);
                if (bgs.getPiecesOnPoint(1 - playerId, physicalIndex) < 2) {
                    // we can move to this point
                    actions.add(new MovePiece(0, physicalIndex));
                }
            }
            if (bgp.entryRule == Home) {
                // player can move these as long as they stay within the entry board
                // (or as per previous chunk of code, you can move from the bar onto the board)
                for (int pos = 0; pos < bgp.entryBoardSize; pos++) {
                    int physicalIndex = bgs.getPhysicalSpace(playerId, pos);
                    if (bgs.getPiecesOnPoint(playerId, physicalIndex) > 0) {
                        // we can move this piece
                        for (int die : diceAvailable) {
                            int targetIndex = pos + die;
                            if (targetIndex < bgp.entryBoardSize) {
                                int physicalTargetIndex = bgs.getPhysicalSpace(playerId, targetIndex);
                                if (bgs.getPiecesOnPoint(1 - playerId, physicalTargetIndex) < 2) {
                                    // we can move to this point
                                    actions.add(new MovePiece(physicalIndex, physicalTargetIndex));
                                }
                            }
                        }
                    }
                }
            }
            if (bgp.entryRule == Bar || bgp.entryRule == Home) {
                // we cannot consider other actions until we have moved all pieces from the bar
                if (actions.isEmpty())
                    actions.add(new DoNothing());
                return actions;
            }
            // else we can continue and add other actions
        }

        if (bgs.allPiecesOnHomeBoard(playerId)) {
            // now we can bear off, so add these possibilities to the actions list
            int maxDieValue = Arrays.stream(diceAvailable).max().orElse(0);
            for (int i = 0; i < maxDieValue; i++) {
                int physicalIndex = bgs.getPhysicalSpace(playerId, boardSize - i - 1);
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
