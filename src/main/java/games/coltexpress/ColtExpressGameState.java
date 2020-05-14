package games.coltexpress;

import core.AbstractGameState;
import core.ForwardModel;
import core.actions.IAction;
import core.observations.IObservation;
import core.observations.IPrintable;
import games.coltexpress.cards.CharacterType;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Train;
import core.components.IPartialObservableDeck;
import core.components.PartialObservableDeck;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ColtExpressGameState extends AbstractGameState implements IObservation, IPrintable {

    public enum GamePhase {
        DrawCards,
        PlanActions,
        ExecuteActions,
        DraftCharacter
    }

    private List<PartialObservableDeck<ColtExpressCard>> playerHandCards;
    private List<PartialObservableDeck<ColtExpressCard>> playerDecks;
    private PartialObservableDeck<ColtExpressCard> cardStack;
    private HashMap<Integer, CharacterType> playerCharacters;

    private Train train;

    private GamePhase gamePhase = GamePhase.DrawCards;

    public static boolean PARTIAL_OBSERVABLE = false;

    public GamePhase getGamePhase() {
        return gamePhase;
    }

    public void setGamePhase(GamePhase gamePhase) {
        this.gamePhase = gamePhase;
    }

    public ColtExpressGameState(ColtExpressParameters gameParameters, ForwardModel model, int nPlayers) {
        super(gameParameters, model, nPlayers, new ColtExpressTurnOrder(nPlayers));
        setComponents(gameParameters);
    }

    public void setComponents(ColtExpressParameters gameParameters) {
        train = new Train(getNPlayers());
        playerCharacters = new HashMap<>();

        // give each player a single card
        playerDecks = new ArrayList<>(getNPlayers());
        for (int playerIndex = 0; playerIndex < getNPlayers(); playerIndex++) {
            playerCharacters.put(playerIndex, gameParameters.pickRandomCharacterType());

            boolean[] visibility = new boolean[getNPlayers()];
            Arrays.fill(visibility, !PARTIAL_OBSERVABLE);
            visibility[playerIndex] = true;

            PartialObservableDeck<ColtExpressCard> playerCards =
                    new PartialObservableDeck<>("playerCards"+playerIndex, visibility);
            for (ColtExpressCard.CardType type : gameParameters.cardCounts.keySet()){
                for (int j = 0; j < gameParameters.cardCounts.get(type); j++)
                    playerCards.add(new ColtExpressCard(type));
            }
            playerDecks.add(playerCards);
        }
    }

    private ArrayList<IAction> playerActions(int playerID) {
        ArrayList<IAction> actions = new ArrayList<>();

        // add end turn by drawing a card
        return actions;
    }

    @Override
    public IObservation getObservation(int player) {
        return this;
    }

    @Override
    public void endGame() {
        this.gameStatus = Utils.GameResult.GAME_END;
        Arrays.fill(playerResults, Utils.GameResult.GAME_LOSE);
    }

    private ArrayList<IAction> drawAction(int player){
        ArrayList<IAction> actions = new ArrayList<>();
        return actions;
    }

    public ArrayList<IAction> schemingActions(int player){
        ArrayList<IAction> actions = new ArrayList<>();
        return actions;
    }

    public ArrayList<IAction> stealingActions(int player)
    {
        ArrayList<IAction> actions = new ArrayList<>();
        return actions;
    }

    @Override
    public List<IAction> computeAvailableActions() {

        ArrayList<IAction> actions;
        int player = getTurnOrder().getCurrentPlayer(this);
        switch (gamePhase){
            case DraftCharacter:
                System.out.println("character drafting is not implemented yet");
            case DrawCards:
                actions = drawAction(player);
                break;
            case PlanActions:
                actions = schemingActions(player);
                break;
            case ExecuteActions:
                actions = stealingActions(player);
                break;
            default:
                actions = new ArrayList<>();
                break;
        }

        return actions;
    }

    @Override
    public void setComponents() {

    }

    @Override
    public void printToConsole() {
        System.out.println("Colt Express Game-State");
        System.out.println("======================");

        int currentPlayer = turnOrder.getCurrentPlayer(this);

        for (int i = 0; i < getNPlayers(); i++){
            if (currentPlayer == i)
                System.out.print(">>> ");
            System.out.print("Player " + i + " = "+ playerCharacters.get(i).name() + ":  ");
            printDeck(playerDecks.get(i));
            System.out.println();
        }

        System.out.println("Current GamePhase: " + gamePhase);
    }

    public void printDeck(IPartialObservableDeck<ColtExpressCard> deck){
        StringBuilder sb = new StringBuilder();
        for (ColtExpressCard card : deck.getVisibleCards(turnOrder.getCurrentPlayer(this))){
            if (card == null)
                sb.append("UNKNOWN");
            else
                sb.append(card.cardType.toString());
            sb.append(",");
        }
        if (sb.length() > 0) sb.deleteCharAt(sb.length()-1);
        System.out.print(sb.toString());
    }

}
