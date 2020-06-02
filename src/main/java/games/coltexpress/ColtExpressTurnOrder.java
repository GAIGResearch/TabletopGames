package games.coltexpress;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.coltexpress.cards.RoundCard;
import core.turnorders.TurnOrder;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static utilities.Utils.GameResult.GAME_ONGOING;
import static core.CoreConstants.VERBOSE;


public class ColtExpressTurnOrder extends TurnOrder {

    List<RoundCard> rounds;
    private int firstPlayerOfRound;
    private RoundCard.TurnType firstTurnType;

    private int currentRoundIndex;
    private int currentRoundCardIndex;
    private RoundCard.TurnType currentTurnType;
    private int direction;

    private boolean firstAction;
    private boolean evalPhase;

    public ColtExpressTurnOrder(int nPlayers, ColtExpressParameters cep){
        this.nPlayers = nPlayers;
        this.nMaxRounds = cep.nMaxRounds;
        firstPlayer = 0;
        turnOwner = 0;
        turnCounter = 0;
        roundCounter = 0;
        setStartingPlayer(0);
        firstPlayerOfRound = 0;
        currentRoundIndex = 0;
        currentRoundCardIndex = 0;
        direction = 1;
        firstAction = true;
        evalPhase = false;
        setupRounds(nPlayers, nMaxRounds, cep);
    }

    private ColtExpressTurnOrder(int nPlayers, int nMaxRounds) {
        super(nPlayers, nMaxRounds);
    }

    @Override
    protected void _reset() {
        firstPlayer = 0;
        turnOwner = 0;
        turnCounter = 0;
        roundCounter = 0;
        currentRoundCardIndex = 0;
        currentRoundIndex = 0;
        direction = 1;
        firstAction = true;
        evalPhase = false;
        currentTurnType = rounds.get(0).getTurnTypes()[0];
    }

    @Override
    protected TurnOrder _copy() {
        return null;
    }

    private void setupRounds(int nPlayers, int nMaxRounds, ColtExpressParameters cep){
        rounds = new ArrayList<>(nMaxRounds);

        // Add random round cards
        ArrayList<Integer> availableRounds = new ArrayList<>();
        for (int i = 0; i < cep.roundCards.length; i++) {
            availableRounds.add(i);
        }
        Random r = new Random(cep.getGameSeed());
        for (int i = 0; i < nMaxRounds-1; i++) {
            int choice = r.nextInt(availableRounds.size());
            rounds.add(getRoundCard(cep, choice, nPlayers));
            availableRounds.remove(Integer.valueOf(choice));
        }

        // Add 1 random end round card
        rounds.add(getRandomEndRoundCard(cep));
    }

    public int getCurrentRoundIndex(){return currentRoundIndex;}
    public int getCurrentRoundCardIndex(){return currentRoundCardIndex;}

    private void initRound(RoundCard round, int turn){
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
    public void endPlayerTurn(AbstractGameState gameState) {
        turnCounter++;
        turnOwner = nextPlayer(gameState);

        if (VERBOSE)
            System.out.println("Next Player: " + turnOwner);
    }

    @Override
    public int nextPlayer(AbstractGameState gameState) {
        if (gameState.getGamePhase() != ColtExpressGameState.ColtExpressGamePhase.PlanActions)
            return (turnOwner+direction) % nPlayers;

        if (currentTurnType == RoundCard.TurnType.DoubleTurn)
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
            if (currentRoundIndex < rounds.get(currentRoundCardIndex).getTurnTypes().length)
                initRound(rounds.get(currentRoundCardIndex), currentRoundIndex);
            else
                gameState.setGamePhase(ColtExpressGameState.ColtExpressGamePhase.ExecuteActions);

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
            gameState.setGamePhase(ColtExpressGameState.ColtExpressGamePhase.PlanActions);
        }
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


    /**
     * Helper getter methods for round card composition.
     */

    public RoundCard getRandomEndRoundCard(ColtExpressParameters cep) {
        int nEndCards = cep.endRoundCards.length;
        int choice = new Random(cep.getGameSeed()).nextInt(nEndCards);
        return getEndRoundCard(cep, choice);
    }

    public RoundCard getEndRoundCard(ColtExpressParameters cep, String key) {
        int idx = -1;
        int nEndCards = cep.endRoundCards.length;
        for (int i = 0; i < nEndCards; i++) {
            if (cep.endRoundCards[i].getKey().equals(key)) {
                idx = i;
                break;
            }
        }
        return getEndRoundCard(cep, idx);
    }

    public RoundCard getEndRoundCard(ColtExpressParameters cep, int idx) {
        if (idx >= 0 && idx < cep.endRoundCards.length) {
            RoundCard.TurnType[] turnTypes = cep.endRoundCards[idx].getTurnTypeSequence();
            AbstractAction event = cep.endRoundCards[idx].getEndCardEvent();
            return new RoundCard(cep.endRoundCards[idx].name(), turnTypes, event);
        }
        return null;
    }


    public RoundCard getRandomRoundCard(ColtExpressParameters cep, int nPlayers, int seed) {
        int nEndCards = cep.roundCards.length;
        int choice = new Random(seed).nextInt(nEndCards);
        return getRoundCard(cep, choice, nPlayers);
    }

    public RoundCard getRoundCard(ColtExpressParameters cep, String key, int nPlayers) {
        int idx = -1;
        int nEndCards = cep.roundCards.length;
        for (int i = 0; i < nEndCards; i++) {
            if (cep.roundCards[i].getKey().equals(key)) {
                idx = i;
                break;
            }
        }
        return getRoundCard(cep, idx, nPlayers);
    }

    public RoundCard getRoundCard(ColtExpressParameters cep, int idx, int nPlayers) {
        if (idx >= 0 && idx < cep.roundCards.length) {
            RoundCard.TurnType[] turnTypes = cep.roundCards[idx].getTurnTypeSequence(nPlayers);
            AbstractAction event = cep.roundCards[idx].getEndCardEvent();
            return new RoundCard(cep.roundCards[idx].name(), turnTypes, event);
        }
        return null;
    }

}
