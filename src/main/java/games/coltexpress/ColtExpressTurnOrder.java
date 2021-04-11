package games.coltexpress;

import core.AbstractGameState;
import core.CoreConstants;
import games.coltexpress.cards.RoundCard;
import core.turnorders.TurnOrder;

import java.util.Arrays;
import java.util.Objects;

import static utilities.Utils.GameResult.GAME_END;
import static utilities.Utils.GameResult.GAME_ONGOING;


public class ColtExpressTurnOrder extends TurnOrder {

    private int firstPlayerOfRound;  // First player of round moves clockwise for each round in the game
    private RoundCard.TurnType currentTurnType;  // Current type of turn
    private int direction;  // Direction of play, 1 is clockwise, -1 is anticlockwise
    private boolean firstAction;  // In double turns, allows players to take two turns before changing turn owner
    private int fullPlayerTurnCounter;  // Extra counter for how many turns in a round were played (full turn by all players)

    public ColtExpressTurnOrder(int nPlayers, int nMaxRounds){
        super(nPlayers, nMaxRounds);
        firstPlayer = 0;
        turnOwner = 0;
        fullPlayerTurnCounter = 0;
        turnCounter = 0;
        roundCounter = 0;
        firstPlayerOfRound = 0;
        direction = 1;
        firstAction = true;
        setStartingPlayer(0);
    }

    @Override
    protected void _reset() {
        firstPlayer = 0;
        turnOwner = 0;
        turnCounter = 0;
        roundCounter = 0;
        fullPlayerTurnCounter = 0;
        direction = 1;
        firstAction = true;
    }

    @Override
    protected TurnOrder _copy() {
        ColtExpressTurnOrder ceto = new ColtExpressTurnOrder(nPlayers, nMaxRounds);
        ceto.firstPlayerOfRound = firstPlayerOfRound;
        ceto.currentTurnType = currentTurnType;
        ceto.direction = direction;
        ceto.firstAction = firstAction;
        ceto.fullPlayerTurnCounter = fullPlayerTurnCounter;
        return ceto;
    }

    /**
     * Initializes current turn type and direction of play.
     * @param round - round card.
     * @param turn - turn index (of turn type array in round card).
     */
    private void initTurn(RoundCard round, int turn, ColtExpressGameState state){
        boolean[] allTrue = new boolean[nPlayers];
        Arrays.fill(allTrue, true);
        state.rounds.setVisibilityOfComponent(roundCounter, allTrue);
        currentTurnType = round.getTurnTypes()[turn];
        switch (round.getTurnTypes()[turn]){
            case NormalTurn:
            case DoubleTurn:
            case HiddenTurn:
                direction = 1;
                break;
            case ReverseTurn:
                direction = -1;
                break;
            default:
                throw new IllegalArgumentException("unknown turn type " + currentTurnType.toString());
        }
    }

    public boolean isHiddenTurn(){
        return currentTurnType == RoundCard.TurnType.HiddenTurn;
    }
    public RoundCard.TurnType getCurrentTurnType(){
        return currentTurnType;
    }

    @Override
    public int nextPlayer(AbstractGameState gameState) {
        if (gameState.getGamePhase() == ColtExpressGameState.ColtExpressGamePhase.DraftCharacter) {
            // Return next player
            return (nPlayers + turnOwner + direction) % nPlayers;
        } else if (gameState.getGamePhase() == ColtExpressGameState.ColtExpressGamePhase.ExecuteActions) {
            // Return ID of player on the next card in the planned actions deck
            ColtExpressGameState cegs = (ColtExpressGameState) gameState;
            if (cegs.plannedActions.getSize() > 0) {
                int idx = cegs.plannedActions.getSize()-1;
                int id = cegs.plannedActions.get(idx).playerID;

                // ID could be -1 if bullets introduced in the deck (e.g. by GS copy with PO), try to find the next one
                // and remove the illegal card from the deck
                while (id == -1 && idx > 0) {
                    cegs.plannedActions.remove(idx);
                    idx--;
                    id = cegs.plannedActions.get(idx).playerID;
                    if (id != -1) return id;
                }
                // If no legal cards left, return next player
                return (nPlayers + turnOwner + direction) % nPlayers;
            }
            // Return next player if no cards in deck
            return (nPlayers + turnOwner + direction) % nPlayers;
        } else {
            // Return next player in the round, double up if a double turn
            if (currentTurnType == RoundCard.TurnType.DoubleTurn) {
                if (firstAction) {
                    firstAction = false;
                    return turnOwner;
                }
            }

            firstAction = true;
            return (nPlayers + turnOwner + direction) % nPlayers;
        }
    }

    /**
     * In Colt Express, this round is equivalent to a turn played by all players (one turn type in round card).
     * @param gameState - current game state.
     */
    @Override
    public void endRound(AbstractGameState gameState) {
        if (gameState.getGameStatus() != GAME_ONGOING) return;

        gameState.getPlayerTimer()[getCurrentPlayer(gameState)].incrementRound();

        listeners.forEach(l -> l.onEvent(CoreConstants.GameEvents.ROUND_OVER, gameState, null));

        turnCounter = 0;
        fullPlayerTurnCounter++;
        moveToNextPlayer(gameState, nextPlayer(gameState));

        ColtExpressGameState cegs = (ColtExpressGameState) gameState;
        RoundCard currentRoundCard = cegs.getRounds().get(roundCounter);
        if (fullPlayerTurnCounter < currentRoundCard.getTurnTypes().length) {
            // Initialize next full player turn
            initTurn(currentRoundCard, fullPlayerTurnCounter, cegs);
        } else {
            // All turns in this round played, execute the actions
            gameState.setGamePhase(ColtExpressGameState.ColtExpressGamePhase.ExecuteActions);
        }
    }

    /**
     * Ends the round with the corresponding end event.
     * @param gameState - current game state.
     */
    public void endRoundCard(ColtExpressGameState gameState){
        // End card event
        gameState.getRounds().get(roundCounter).endRoundCardEvent(gameState);
        // Move to next round
        roundCounter++;
        if (roundCounter < gameState.getRounds().getSize()) {
            firstPlayerOfRound = (firstPlayerOfRound + 1) % nPlayers;
            turnCounter = 0;
            fullPlayerTurnCounter = 0;
            turnOwner = firstPlayerOfRound;
            initTurn(gameState.getRounds().get(roundCounter), 0, gameState);
            gameState.setGamePhase(ColtExpressGameState.ColtExpressGamePhase.PlanActions);
            gameState.setGameStatus(GAME_ONGOING);
        } else {
            gameState.setGameStatus(GAME_END);
        }
    }

    public int getFullPlayerTurnCounter() {
        return fullPlayerTurnCounter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColtExpressTurnOrder)) return false;
        if (!super.equals(o)) return false;
        ColtExpressTurnOrder that = (ColtExpressTurnOrder) o;
        return firstPlayerOfRound == that.firstPlayerOfRound &&
                direction == that.direction &&
                firstAction == that.firstAction &&
                fullPlayerTurnCounter == that.fullPlayerTurnCounter &&
                currentTurnType == that.currentTurnType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), firstPlayerOfRound, currentTurnType, direction, firstAction, fullPlayerTurnCounter);
    }
}
