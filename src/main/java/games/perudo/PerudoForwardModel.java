package games.perudo;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import games.perudo.actions.Bid;
import games.perudo.actions.Calza;
import games.perudo.actions.Dudo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static games.perudo.PerudoParameters.PACO;

public class PerudoForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        PerudoGameState state = (PerudoGameState) firstState;
        PerudoParameters params = state.getParams();
        int nPlayers = state.getNPlayers();
        state.dice = new int[nPlayers][params.nFaces];
        state.eliminationRound = new int[nPlayers];
        Arrays.fill(state.eliminationRound, -1);
        state.palificoUsed = new boolean[nPlayers];
        state.setPalificoRound(false);
        state.clearBid();
        state.setNextRoundStarter(-1);
        for (int p = 0; p < nPlayers; p++)
            rollDice(state, p, params.nDicePerPlayer);
        state.setFirstPlayer(0);  // player 0 opens the first round
    }

    /**
     * Replaces the player's dice with n newly rolled ones.
     */
    private void rollDice(PerudoGameState state, int player, int n) {
        Random rnd = state.getRnd();
        int[] rolled = new int[state.getParams().nFaces];
        for (int d = 0; d < n; d++)
            rolled[rnd.nextInt(rolled.length)]++;
        state.setDice(player, rolled);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        PerudoGameState state = (PerudoGameState) gameState;
        PerudoParameters params = state.getParams();
        int totalDice = state.getTotalDice();
        // the range of quantities that may be bid on each face (none when from > to)
        int[] from = new int[params.nFaces + 1], to = new int[params.nFaces + 1];
        for (int face = 1; face <= params.nFaces; face++) {
            int lowest = lowestBid(state, face);
            if (lowest < 0) continue;
            int floor = totalDice * params.minBidPercent / 100;
            int band = totalDice * params.maxBidPercent / 100;
            // a Paco is wild, and so worth two dice of another face, as the raises to and from Pacos have it
            if (face == PACO && !state.isPalificoRound()) {
                floor = (floor + 1) / 2;
                band = (band + 1) / 2;
            }
            from[face] = Math.max(lowest, floor);
            // no bid may be for more dice than are on the table
            to[face] = Math.min(totalDice, Math.max(lowest + params.maxRaise, band));
        }
        List<AbstractAction> actions = new ArrayList<>();
        for (int quantity = 1; quantity <= totalDice; quantity++) {
            for (int face = 1; face <= params.nFaces; face++) {
                if (quantity >= from[face] && quantity <= to[face])
                    actions.add(new Bid(quantity, face));
            }
        }
        if (state.hasBid()) {
            actions.add(new Dudo());
            if (state.getParams().calza && !state.isPalificoRound())
                actions.add(new Calza());
        }
        return actions;
    }

    /**
     * The fewest dice that may be bid on the face, or -1 if no bid on it is legal.
     */
    private int lowestBid(PerudoGameState state, int face) {
        // only a Palifico round may open on Pacos
        if (!state.hasBid())
            return face != PACO || state.isPalificoRound() ? 1 : -1;
        int bidQuantity = state.getBidQuantity();
        // in a Palifico round the face is fixed
        if (state.isPalificoRound())
            return face == state.getBidFace() ? bidQuantity + 1 : -1;
        // from Pacos: more Pacos, or more than twice as many dice of another face
        if (state.getBidFace() == PACO)
            return face == PACO ? bidQuantity + 1 : 2 * bidQuantity + 1;
        // to Pacos: at least half as many, rounded up
        if (face == PACO)
            return (bidQuantity + 1) / 2;
        return face > state.getBidFace() ? bidQuantity : bidQuantity + 1;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        PerudoGameState state = (PerudoGameState) currentState;
        if (state.hasBid()) {
            endPlayerTurn(state);
            return;
        }
        // A challenge cleared the bid: players left with no dice are out, then the game ends or a new round starts
        int nPlayers = state.getNPlayers();
        int playersLeft = 0;
        for (int p = 0; p < nPlayers; p++) {
            if (state.getNDice(p) == 0 && state.isNotTerminalForPlayer(p)) {
                state.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, p);
                state.setEliminationRound(p, state.getRoundCounter());
            }
            if (state.isNotTerminalForPlayer(p))
                playersLeft++;
        }
        if (playersLeft == 1) {
            endGame(state);
            return;
        }
        int starter = state.getNextRoundStarter();
        // the round after a player first drops to one die is their Palifico round
        boolean palifico = state.getParams().palifico && state.getNDice(starter) == 1 && !state.hasHadPalifico(starter);
        state.setPalificoRound(palifico);
        if (palifico)
            state.setPalificoUsed(starter, true);
        while (!state.isNotTerminalForPlayer(starter))
            starter = (starter + 1) % nPlayers;
        state.setNextRoundStarter(-1);
        endRound(state, starter);
        for (int p = 0; p < nPlayers; p++) {
            if (state.isNotTerminalForPlayer(p))
                rollDice(state, p, state.getNDice(p));
        }
    }
}
