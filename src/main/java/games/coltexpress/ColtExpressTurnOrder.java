package games.coltexpress;

import core.AbstractGameState;
import core.turnorders.TurnOrder;
import games.coltexpress.cards.roundcards.EndCardMarshallsRevenge;
import games.coltexpress.cards.roundcards.RoundCard;
import games.coltexpress.cards.roundcards.RoundCardBridge;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;

import static utilities.Utils.GameResult.GAME_ONGOING;
import static core.CoreConstants.VERBOSE;


public class ColtExpressTurnOrder extends TurnOrder {

    List<RoundCard> rounds = new ArrayList<>(5);
    private int firstPlayerOfRound = 0;

    private int currentRoundIndex = 0;
    private int currentRoundCardIndex = 0;
    private RoundCard.TurnType currentTurnType;
    private boolean firstAction=true;
    private int direction = 1;

    private boolean evalPhase = false;

    public ColtExpressTurnOrder(int nPlayers){
        super(nPlayers);
        setStartingPlayer(0);
        rounds.add(new RoundCardBridge(nPlayers));
        rounds.add(new RoundCardBridge(nPlayers));
        rounds.add(new RoundCardBridge(nPlayers));
        rounds.add(new RoundCardBridge(nPlayers));
        rounds.add(new EndCardMarshallsRevenge());
    }

    public int getCurrentRoundIndex(){return currentRoundIndex;}
    public int getCurrentRoundCardIndex(){return currentRoundCardIndex;}

    private void initRound(RoundCard round, int turn){
        currentTurnType = round.turnTypes[turn];
        switch (round.turnTypes[turn]){
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
    public void endPlayerTurn(AbstractGameState gameState) {
        turnCounter++;
        turnOwner = nextPlayer(gameState);

        if (VERBOSE)
            System.out.println("Next Player: " + turnOwner);
    }

    @Override
    public int nextPlayer(AbstractGameState gameState) {
        if (((ColtExpressGameState) gameState).getGamePhase() != ColtExpressGameState.ColtExpressGamePhase.PlanActions)
            return (turnOwner+direction) % nPlayers;

        if (currentTurnType== RoundCard.TurnType.DoubleTurn)
        {
            if (firstAction) {
                firstAction = false;
                return turnOwner;
            }
        }

        firstAction = true;
        int tmp = (turnOwner+direction) % nPlayers;
        if (tmp == firstPlayerOfRound){
            currentRoundIndex++;
            if (currentRoundIndex < rounds.get(currentRoundCardIndex).turnTypes.length)
                initRound(rounds.get(currentRoundCardIndex), currentRoundIndex);
            else
                ((ColtExpressGameState) gameState).setGamePhase(ColtExpressGameState.ColtExpressGamePhase.ExecuteActions);
        }
        return tmp;
    }

    @Override
    public void endRound(AbstractGameState gameState) {

        if (nMaxRounds != -1 && roundCounter == nMaxRounds) gameState.setGameStatus(Utils.GameResult.GAME_END);
        else {
            turnCounter = 0;
            turnOwner = 0;
            while (gameState.getPlayerResults()[turnOwner] != GAME_ONGOING) {
                turnOwner = nextPlayer(gameState);
            }
        }
    }

    public void endRoundCard(AbstractGameState gameState){
        rounds.get(currentRoundCardIndex).endRoundCardEvent((ColtExpressGameState) gameState);
        currentRoundCardIndex++;
        if (currentRoundCardIndex < rounds.size()) {
            firstPlayerOfRound = (firstPlayerOfRound + 1) % nPlayers;
            turnCounter = 0;
            turnOwner = firstPlayerOfRound;
            initRound(rounds.get(currentRoundCardIndex), 0);
            ((ColtExpressGameState) gameState).setGamePhase(ColtExpressGameState.ColtExpressGamePhase.PlanActions);
            ((ColtExpressGameState) gameState).distributeCards();
        }
    }

    @Override
    public TurnOrder copy() {
        ColtExpressTurnOrder to = new ColtExpressTurnOrder(nPlayers);
        copyTo(to);
        throw new IllegalArgumentException("ColtExpressTurnOrder.copy not implemented yet");
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("ColtExpressTurnOrder: ");
        int i = 0;
        for (RoundCard round : rounds){
            if (i == currentRoundCardIndex) {
                sb.append("->");
            }
            sb.append(round.toString());
            sb.append(", ");
            i++;
        }

        sb.deleteCharAt(sb.length()-2);
        return sb.toString();
    }

}
