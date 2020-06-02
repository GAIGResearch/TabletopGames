package games.coltexpress;

import core.AbstractGameParameters;
import core.AbstractGameState;
import core.components.Component;
import core.interfaces.IGamePhase;
import core.interfaces.IPrintable;
import core.observations.VectorObservation;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;
import core.components.PartialObservableDeck;
import utilities.Utils;
import games.coltexpress.ColtExpressTypes.*;

import java.util.*;

public class ColtExpressGameState extends AbstractGameState implements IPrintable {

    // Colt express adds 4 game phases
    public enum ColtExpressGamePhase implements IGamePhase {
        DrawCards,
        PlanActions,
        ExecuteActions,
        DraftCharacter
    }

    // Cards in player hands
    List<PartialObservableDeck<ColtExpressCard>> playerHandCards;
    // A deck for each player
    List<PartialObservableDeck<ColtExpressCard>> playerDecks;
    List<PartialObservableDeck<Loot>> playerLoot;
    int[] bulletsLeft;
    // The player characters available
    HashMap<Integer, CharacterType> playerCharacters;
    int playerPlayingBelle = -1;

    // The card stack built by players each round
    PartialObservableDeck<ColtExpressCard> plannedActions;
    // The train to loot
    final LinkedList<Compartment> trainCompartments;

    @Override
    public List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();
        components.add(plannedActions);
        components.addAll(trainCompartments);

        for (Compartment compartment: trainCompartments) {
            components.add(compartment.getLootInside());
            components.add(compartment.getLootOnTop());
        }

        components.addAll(playerHandCards);
        components.addAll(playerDecks);
        return components;
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        // TODO
        return null;
    }

    @Override
    protected VectorObservation _getVectorObservation() {
        return null;// TODO
    }

    @Override
    protected double[] _getDistanceFeatures(int playerId) {
        return new double[0];// TODO
    }

    @Override
    protected HashMap<HashMap<Integer, Double>, Utils.GameResult> _getTerminalFeatures(int playerId) {
        return null;// TODO
    }

    @Override
    protected double _getScore(int playerId) {
        return 0;// TODO
    }

    public ColtExpressGameState(AbstractGameParameters gameParameters, int nPlayers) {
        super(gameParameters, new ColtExpressTurnOrder(nPlayers, (ColtExpressParameters) gameParameters));
        gamePhase = ColtExpressGamePhase.DrawCards;
        trainCompartments = new LinkedList<>();
    }

    public void addLoot(Integer playerID, Loot loot) {
        playerLoot.get(playerID).add(loot);
    }

    public void addNeutralBullet(Integer playerID) {
        addBullet(playerID, -1);
    }

    public void addBullet(Integer playerID, Integer shooterID) {
        this.playerDecks.get(playerID).add(new ColtExpressCard(shooterID, ColtExpressCard.CardType.Bullet));
        if (playerCharacters.containsKey(shooterID))
            bulletsLeft[shooterID]--;
    }

    public LinkedList<Compartment> getTrainCompartments() {
        return trainCompartments;
    }

    public PartialObservableDeck<Loot> getLoot(int playerID){return playerLoot.get(playerID);}

    public PartialObservableDeck<ColtExpressCard> getPlannedActions() {
        return plannedActions;
    }

    public List<PartialObservableDeck<ColtExpressCard>> getPlayerDecks() {
        return playerDecks;
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println("Colt Express Game-State");
        System.out.println("=======================");

        int currentPlayer = turnOrder.getCurrentPlayer(this);

        for (int i = 0; i < getNPlayers(); i++){
            if (currentPlayer == i)
                System.out.print(">>> ");
            System.out.print("Player " + i + " = "+ playerCharacters.get(i).name() + ":  ");
            System.out.print("Hand=");
            System.out.print(playerHandCards.get(i).toString(i));
            System.out.print("; Deck=");
            System.out.print(playerDecks.get(i).toString(i));
            System.out.print("; Loot=");
            System.out.print(playerLoot.get(i).toString(i));
            System.out.println();
        }
        System.out.println();
        System.out.println(printTrain());

        System.out.println();
        System.out.print("Planned Actions: ");
        System.out.println(plannedActions.toString());

        System.out.println();
        System.out.println(turnOrder.toString());

        System.out.println();
        System.out.println("Current GamePhase: " + gamePhase);
    }

    public String printTrain(){
        StringBuilder sb = new StringBuilder();
        sb.append("Train:\n");
        for (Compartment compartment : trainCompartments)
        {
            sb.append(compartment.toString());
            sb.append("\n");
        }
        sb.deleteCharAt(sb.length()-1);

        return sb.toString();
    }
}
