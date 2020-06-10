package games.coltexpress;

import core.AbstractGameParameters;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import core.interfaces.IGamePhase;
import core.interfaces.IPrintable;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.cards.RoundCard;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;
import core.components.PartialObservableDeck;
import games.coltexpress.ColtExpressTypes.*;

import java.util.*;

public class ColtExpressGameState extends AbstractGameState implements IPrintable {

    // Colt express adds 4 game phases
    public enum ColtExpressGamePhase implements IGamePhase {
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
    int playerPlayingBelle;

    // The card stack built by players each round
    PartialObservableDeck<ColtExpressCard> plannedActions;
    // The train to loot
    LinkedList<Compartment> trainCompartments;
    // The round cards
    List<RoundCard> rounds;

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
        components.addAll(playerLoot);
        components.addAll(rounds);
        return components;
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        ColtExpressGameState copy = new ColtExpressGameState(gameParameters, getNPlayers());
        copy.playerHandCards = new ArrayList<>();
        for (PartialObservableDeck<ColtExpressCard> d: playerHandCards) {
            copy.playerHandCards.add(d.copy());
        }
        copy.playerDecks = new ArrayList<>();
        for (PartialObservableDeck<ColtExpressCard> d: playerDecks) {
            copy.playerDecks.add(d.copy());
        }
        copy.playerLoot = new ArrayList<>();
        for (PartialObservableDeck<Loot> d: playerLoot) {
            copy.playerLoot.add(d.copy());
        }
        copy.trainCompartments = new LinkedList<>();
        for (Compartment d: trainCompartments) {
            copy.trainCompartments.add((Compartment) d.copy());
        }
        copy.bulletsLeft = bulletsLeft.clone();
        copy.playerCharacters = new HashMap<>(playerCharacters);
        copy.playerPlayingBelle = playerPlayingBelle;
        copy.plannedActions = plannedActions.copy();
        copy.rounds = new ArrayList<>();
        for (RoundCard c: rounds) {
            copy.rounds.add((RoundCard) c.copy());
        }
        return copy;
    }

    @Override
    protected double _getScore(int playerId) {
        return 0;// TODO
    }

    @Override
    protected void _reset() {
        playerHandCards = new ArrayList<>();
        playerDecks = new ArrayList<>();
        playerLoot = new ArrayList<>();
        bulletsLeft = new int[getNPlayers()];
        playerCharacters = new HashMap<>();
        playerPlayingBelle = -1;
        plannedActions = null;
        trainCompartments = new LinkedList<>();
        rounds = new ArrayList<>();
        gamePhase = ColtExpressGamePhase.PlanActions;
    }

    public ColtExpressGameState(AbstractGameParameters gameParameters, int nPlayers) {
        super(gameParameters, new ColtExpressTurnOrder(nPlayers, (ColtExpressParameters) gameParameters));
        gamePhase = ColtExpressGamePhase.PlanActions;
        trainCompartments = new LinkedList<>();
        playerPlayingBelle = -1;
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

    public List<RoundCard> getRounds() {
        return rounds;
    }

    @Override
    public void printToConsole() {
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
        int i = 0;
        for (RoundCard round : rounds){
            if (i == ((ColtExpressTurnOrder)turnOrder).getCurrentRoundCardIndex()) {
                System.out.print("->");
            }
            System.out.print(round.toString());
            System.out.print(", ");
            i++;
        }

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

    /**
     * Helper getter methods for round card composition.
     */

    RoundCard getRandomEndRoundCard(ColtExpressParameters cep) {
        int nEndCards = cep.endRoundCards.length;
        int choice = new Random(cep.getGameSeed()).nextInt(nEndCards);
        return getEndRoundCard(cep, choice);
    }

    RoundCard getEndRoundCard(ColtExpressParameters cep, int idx) {
        if (idx >= 0 && idx < cep.endRoundCards.length) {
            RoundCard.TurnType[] turnTypes = cep.endRoundCards[idx].getTurnTypeSequence();
            AbstractAction event = cep.endRoundCards[idx].getEndCardEvent();
            return new RoundCard(cep.endRoundCards[idx].name(), turnTypes, event);
        }
        return null;
    }

    RoundCard getRoundCard(ColtExpressParameters cep, int idx, int nPlayers) {
        if (idx >= 0 && idx < cep.roundCards.length) {
            RoundCard.TurnType[] turnTypes = cep.roundCards[idx].getTurnTypeSequence(nPlayers);
            AbstractAction event = cep.roundCards[idx].getEndCardEvent();
            return new RoundCard(cep.roundCards[idx].name(), turnTypes, event);
        }
        return null;
    }

}
