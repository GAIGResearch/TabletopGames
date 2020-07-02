package games.coltexpress;

import core.AbstractGameState;
import games.coltexpress.cards.RoundCard;
import core.turnorders.TurnOrder;
import utilities.Utils;

import static utilities.Utils.GameResult.GAME_END;
import static utilities.Utils.GameResult.GAME_ONGOING;
import static core.CoreConstants.VERBOSE;


public class ColtExpressTurnOrder extends TurnOrder {

    private int firstPlayerOfRound;
    ColtExpressParameters gameParameters;

    private int currentRoundIndex;
    private int currentRoundCardIndex;
    private RoundCard.TurnType currentTurnType;
    private int direction;

    private boolean firstAction;
    private boolean evalPhase;

    public ColtExpressTurnOrder(int nPlayers, ColtExpressParameters cep){
        this.gameParameters = cep;
        this.nPlayers = nPlayers;
        this.nMaxRounds = cep.nMaxRounds;
        firstPlayer = 0;
        turnOwner = 0;
        turnCounter = 0;
        roundCounter = 0;
        firstPlayerOfRound = 0;
        currentRoundIndex = 0;
        currentRoundCardIndex = 0;
        direction = 1;
        firstAction = true;
        evalPhase = false;
        setStartingPlayer(0);
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
    }

    @Override
    protected TurnOrder _copy() {
        ColtExpressTurnOrder ceto = new ColtExpressTurnOrder(nPlayers, gameParameters);
        ceto.firstPlayerOfRound = firstPlayerOfRound;
        ceto.gameParameters = (ColtExpressParameters) gameParameters.copy();
        ceto.currentRoundIndex = currentRoundIndex;
        ceto.currentRoundCardIndex = currentRoundCardIndex;
        ceto.currentTurnType = currentTurnType;
        ceto.direction = direction;
        ceto.firstAction = firstAction;
        ceto.evalPhase = evalPhase;
        return ceto;
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
        if (gameState.getGamePhase() == ColtExpressGameState.ColtExpressGamePhase.DraftCharacter) {
            return (nPlayers + turnOwner + direction) % nPlayers;
        } else if (gameState.getGamePhase() == ColtExpressGameState.ColtExpressGamePhase.ExecuteActions) {
            ColtExpressGameState cegs = (ColtExpressGameState) gameState;
            if (cegs.plannedActions.getSize() > 0) {
                return cegs.plannedActions.getComponents().get(0).playerID;
            }
            return (nPlayers + turnOwner + direction) % nPlayers;
        } else {
            if (currentTurnType == RoundCard.TurnType.DoubleTurn) {
                if (firstAction) {
                    firstAction = false;
                    return turnOwner;
                }
            }

            firstAction = true;
            int tmp = (nPlayers + turnOwner + direction) % nPlayers;
            if (tmp == firstPlayerOfRound) {
                currentRoundIndex++;
                if (currentRoundIndex < ((ColtExpressGameState) gameState).getRounds().get(currentRoundCardIndex).getTurnTypes().length)
                    initRound(((ColtExpressGameState) gameState).getRounds().get(currentRoundCardIndex), currentRoundIndex);
                else
                    gameState.setGamePhase(ColtExpressGameState.ColtExpressGamePhase.ExecuteActions);

            }
            return tmp;
        }
    }

    @Override
    public void endRound(AbstractGameState gameState) {
        if (nMaxRounds != -1 && roundCounter == nMaxRounds) gameState.setGameStatus(Utils.GameResult.GAME_END);
        else {
            turnCounter = 0;
            turnOwner = 0;
            int n = 0;
            while (gameState.getPlayerResults()[turnOwner] != GAME_ONGOING) {
                turnOwner = nextPlayer(gameState);
                n++;
                if (n >= nPlayers) {
                    gameState.setGameStatus(GAME_END);
                    break;
                }
            }
        }
    }

    public void endRoundCard(ColtExpressGameState gameState){
        gameState.getRounds().get(currentRoundCardIndex).endRoundCardEvent(gameState);
        currentRoundCardIndex++;
        if (currentRoundCardIndex < gameState.getRounds().getSize()) {
            firstPlayerOfRound = (firstPlayerOfRound + 1) % nPlayers;
            turnCounter = 0;
            turnOwner = firstPlayerOfRound;
            initRound(gameState.getRounds().get(currentRoundCardIndex), 0);
            gameState.setGamePhase(ColtExpressGameState.ColtExpressGamePhase.PlanActions);
        }
    }
}
