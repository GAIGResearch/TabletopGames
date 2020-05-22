package games.coltexpress;

import core.AbstractGameState;
import core.ForwardModel;
import core.gamephase.GamePhase;
import core.actions.IAction;
import core.observations.IObservation;
import core.observations.IPrintable;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Train;
import core.components.PartialObservableDeck;
import utilities.Utils;
import games.coltexpress.ColtExpressParameters.CharacterType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ColtExpressGameState extends AbstractGameState implements IObservation, IPrintable {

    // Colt express adds 4 game phases
    public enum ColtExpressGamePhase implements GamePhase {
        DrawCards,
        PlanActions,
        ExecuteActions,
        DraftCharacter
    }

    // Cards in player hands
    List<PartialObservableDeck<ColtExpressCard>> playerHandCards;
    // A deck for each player
    List<PartialObservableDeck<ColtExpressCard>> playerDecks;
    // The card stack built by players each round
    PartialObservableDeck<ColtExpressCard> cardStack;
    // The player characters available
    HashMap<Integer, CharacterType> playerCharacters;
    // The train to loot
    Train train;

    public ColtExpressGameState(ColtExpressParameters gameParameters, ForwardModel model, int nPlayers) {
        super(gameParameters, model, new ColtExpressTurnOrder(nPlayers));
        gamePhase = ColtExpressGamePhase.DrawCards;
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

    @Override
    public List<IAction> computeAvailableActions() {

        ArrayList<IAction> actions;
        if (ColtExpressGamePhase.DraftCharacter.equals(gamePhase)) {
            System.out.println("character drafting is not implemented yet");
            actions = drawAction();
        } else if (ColtExpressGamePhase.DrawCards.equals(gamePhase)) {
            actions = drawAction();
        } else if (ColtExpressGamePhase.PlanActions.equals(gamePhase)) {
            actions = schemingActions();
        } else if (ColtExpressGamePhase.ExecuteActions.equals(gamePhase)) {
            actions = stealingActions();
        } else {
            actions = new ArrayList<>();
        }

        return actions;
    }
    private ArrayList<IAction> drawAction(){
        ArrayList<IAction> actions = new ArrayList<>();
        return actions;
    }

    public ArrayList<IAction> schemingActions(){
        ArrayList<IAction> actions = new ArrayList<>();
        return actions;
    }

    public ArrayList<IAction> stealingActions()
    {
        ArrayList<IAction> actions = new ArrayList<>();
        return actions;
    }

//    private ArrayList<IAction> playerActions(int playerID) {
//        ArrayList<IAction> actions = new ArrayList<>();
//
//        // add end turn by drawing a card
//        return actions;
//    }

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

    public void printDeck(PartialObservableDeck<ColtExpressCard> deck){
        StringBuilder sb = new StringBuilder();
        for (ColtExpressCard card : deck.getVisibleComponents(turnOrder.getCurrentPlayer(this))){
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
