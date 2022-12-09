package games.sirius;

import core.AbstractGameState;
import core.CoreConstants;
import core.turnorders.TurnOrder;
import games.sirius.SiriusConstants.SiriusPhase;

import java.util.Arrays;

import static games.sirius.SiriusConstants.MoonType.*;
import static games.sirius.SiriusConstants.SiriusCardType.FAVOUR;
import static games.sirius.SiriusConstants.SiriusPhase.Move;

public class SiriusTurnOrder extends TurnOrder {

    int[] playerByRank; // this is indexed by rank and holds the current player at that rank
    int[] nextPlayer; // this is indexed by player and indicated the next player. It is updated at the start of each raound.
    // We have different rules for different phases
    // During move selection we are formally simultaneous  (moveSelected)
    // While for everything else we go strictly in order of current ranking (nextPlayer)

    public SiriusTurnOrder(int nPlayers) {
        super(nPlayers);
        nextPlayer = new int[nPlayers];
        playerByRank = new int[nPlayers + 1];
        Arrays.setAll(playerByRank, i -> i - 1);
        updatePlayerOrder();
    }

    @Override
    public int nextPlayer(AbstractGameState gs) {
        SiriusGameState state = (SiriusGameState) gs;
        SiriusPhase phase = (SiriusPhase) state.getGamePhase();
        switch (phase) {
            case Move:
                // In this case move selection is simultaneous
                // so the next player is the first one who has not selected a move (and is not us)
                for (int i = 0; i < state.moveSelected.length; i++)
                    if (i != turnOwner && state.moveSelected[i] == -1) return i;
                // else we change phase, and the next player is the firstPlayer
                return getPlayerAtRank(1);
            case Favour:
                // if there is no next player (i.e. -1), then the next is 0 for Move
                return nextPlayer[getCurrentPlayer(state)] > -1 ? nextPlayer[getCurrentPlayer(state)] : 0;
            case Draw:
                // if there is no next player (i.e. -1), then the next is the current last player for Favours
                return nextPlayer[getCurrentPlayer(state)] > -1 ? nextPlayer[getCurrentPlayer(state)] : getPlayerAtRank(2);
            default:
                throw new AssertionError("Unknown Phase " + phase);
        }
    }

    @Override
    public void endPlayerTurn(AbstractGameState gs) {
        SiriusGameState state = (SiriusGameState) gs;
        SiriusPhase phase = (SiriusPhase) state.getGamePhase();
        listeners.forEach(l -> l.onEvent(CoreConstants.GameEvents.TURN_OVER, state, null));
        state.getPlayerTimer()[getCurrentPlayer(state)].incrementTurn();

        boolean firstTurnOfPhase = false;
        switch (phase) {
            case Move:
                if (state.allMovesSelected()) {
                    state.applyChosenMoves();
                    state.setGamePhase(SiriusPhase.Draw);
                    firstTurnOfPhase = true;
                }
                break;
            case Draw:
                if (allActionsComplete()) {
                    endRound(state);
                    state.setGamePhase(SiriusPhase.Favour);
                    firstTurnOfPhase = true;
                }
                break;
            case Favour:
                if (allActionsComplete()) {
                    Arrays.fill(state.moveSelected, -1);
                    state.setGamePhase(Move);
                    firstTurnOfPhase = true;
                    updatePlayerOrder();
                }
        }
        turnCounter++;
        if (firstTurnOfPhase) {
            if (state.getGamePhase() == Move)
                turnOwner = 0;
            else
                turnOwner = playerByRank[1];
        } else
            turnOwner = nextPlayer(state);

        // This next bit ensures that the player knows the cards they can select from
        // This has to be done before computeAvailableActions as this latter uses a re-determinised
        // state, meaning that the actions may not be possible in the actual game state
        if (state.getGamePhase() == SiriusPhase.Draw) {
            Moon nextMoon = state.getMoon(state.getLocationIndex(turnOwner));
            if (nextMoon.moonType == MINING || nextMoon.moonType == PROCESSING) {
                nextMoon.lookAtDeck(turnOwner);
            }
        }
    }

    protected boolean allActionsComplete() {
        return nextPlayer[turnOwner] == -1;  // the current player is last to move
    }

    // returns the current rank of the player for determining move order (1 is first, and so on)
    public int getRank(int player) {
        for (int r = 1; r <= nPlayers; r++) {
            if (playerByRank[r] == player)
                return r;
        }
        throw new AssertionError("Should be unreachable");
    }

    public void setRank(int player, int rank) {
        int oldRank = getRank(player);
        int inc = oldRank < rank ? 1 : -1;
        int diff = Math.abs(oldRank - rank);
        // we shuffle all the ranks up and move player to rank
        // We have to do this step last so that we have already shuffled out the previous occupant
        for (int i = 0; i <= diff; i++) {
            if (i == diff) {
                playerByRank[rank] = player;
            } else {
                playerByRank[i * inc + oldRank] = playerByRank[(i + 1) * inc + oldRank];
            }
        }
    }

    public void updatePlayerOrder() {
        // we set up nextPlayer to point to the next player
        for (int r = 1; r < nPlayers; r++) {
            nextPlayer[playerByRank[r]] = playerByRank[r + 1];
        }
        nextPlayer[playerByRank[nPlayers]] = -1;
    }

    public int getPlayerAtRank(int rank) {
        return playerByRank[rank];
    }

    @Override
    public void endRound(AbstractGameState gs) {
        SiriusGameState state = (SiriusGameState) gs;
        SiriusParameters params = (SiriusParameters) state.getGameParameters();
        state.getPlayerTimer()[getCurrentPlayer(state)].incrementRound();
        listeners.forEach(l -> l.onEvent(CoreConstants.GameEvents.ROUND_OVER, state, null));

        // add cards
        for (Moon moon : state.getAllMoons()) {
            if (moon.getMoonType() == MINING) {
                int drawLimit = moon.getDeckSize() == 0 ? params.cardsPerEmptyMoon : params.cardsPerNonEmptyMoon;
                for (int i = 0; i < drawLimit; i++) {
                    if (state.ammoniaDeck.getSize() > 0)
                        moon.addCard(state.ammoniaDeck.draw());
                }
                if (moon.getCartelOwner() > -1 && state.ammoniaDeck.getSize() > 0)
                    state.addCardToHand(moon.getCartelOwner(), state.ammoniaDeck.draw());
            }
            if (moon.getMoonType() == PROCESSING) {
                int drawLimit = moon.getDeckSize() == 0 ? params.cardsPerEmptyMoon : params.cardsPerNonEmptyMoon;
                for (int i = 0; i < drawLimit; i++) {
                    if (state.contrabandDeck.getSize() > 0)
                        moon.addCard(state.contrabandDeck.draw());
                }
                if (moon.getCartelOwner() > -1 && state.contrabandDeck.getSize() > 0)
                    state.addCardToHand(moon.getCartelOwner(), state.contrabandDeck.draw());
            }
            if (moon.getMoonType() == METROPOLIS) {
                int drawLimit = state.getNPlayers() - moon.getDeckSize();
                for (int i = 0; i < drawLimit; i++) {
                    moon.addCard(new SiriusCard("Favour", FAVOUR, 1));
                }
                if (moon.getCartelOwner() > -1)
                    state.addCardToHand(moon.getCartelOwner(), new SiriusCard("Favour", FAVOUR, 1));
            }
        }
        // move first rank player to last, and shuffle the others
        setRank(playerByRank[1], nPlayers);
        updatePlayerOrder();
        roundCounter++;
    }


    @Override
    protected void _reset() {

    }

    @Override
    protected TurnOrder _copy() {
        SiriusTurnOrder retValue = new SiriusTurnOrder(nPlayers);
        retValue.nextPlayer = nextPlayer.clone();
        retValue.playerByRank = nextPlayer.clone();
        return retValue;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 31 * Arrays.hashCode(nextPlayer) + 31 * 31 * Arrays.hashCode(playerByRank);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SiriusTurnOrder) {
            SiriusTurnOrder other = (SiriusTurnOrder) obj;
            return Arrays.equals(nextPlayer, other.nextPlayer) && Arrays.equals(playerByRank, other.playerByRank)
                    && super.equals(obj);
        }
        return false;
    }
}
