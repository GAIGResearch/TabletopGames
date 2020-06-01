package games.coltexpress;

import core.AbstractGameState;
import core.interfaces.IGamePhase;
import core.interfaces.IPrintable;
import core.observations.VectorObservation;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;
import games.coltexpress.components.Train;
import core.components.PartialObservableDeck;
import games.coltexpress.ColtExpressParameters.CharacterType;
import utilities.Utils;

import java.util.*;

public class ColtExpressGameState extends AbstractGameState implements IPrintable {

    @Override
    public IObservation next(AbstractAction action) {
        return this;
    }

    @Override
    public IObservation copy() {
        return this;
    }

    // Colt express adds 4 game phases
    public enum ColtExpressGamePhase implements IGamePhase {
        DrawCards,
        PlanActions,
        ExecuteActions,
        DraftCharacter
    }

    List<PartialObservableDeck<ColtExpressCard>> playerHandCards;
    List<PartialObservableDeck<ColtExpressCard>> playerDecks;
    List<PartialObservableDeck<Loot>> playerLoot;
    int[] bulletsLeft;
    PartialObservableDeck<ColtExpressCard> plannedActions;
    HashMap<Integer, CharacterType> playerCharacters;
    int playerPlayingBelle = -1;
    // Cards in player hands
    // A deck for each player
    // The card stack built by players each round
    // The player characters available
    // The train to loot

    Train train;
    public Train getTrain(){return train;}
    public PartialObservableDeck<Loot> getLoot(int playerID){return playerLoot.get(playerID);}

    @Override
    public void addAllComponents() {
        allComponents.putComponent(plannedActions);
        allComponents.putComponent(train);
        allComponents.putComponents(train.getCompartments());
        for (Compartment compartment: train.getCompartments()) {
            for (Loot loot : compartment.getLootInside().getComponents())
                allComponents.putComponent(loot);
            for (Loot loot  : compartment.getLootOnTop().getComponents())
                allComponents.putComponent(loot);
        }

        allComponents.putComponents(playerHandCards);
        allComponents.putComponents(playerDecks);
        for (int i = 0; i < getNPlayers(); i++) {
            allComponents.putComponents(playerHandCards.get(i).getComponents());
            allComponents.putComponents(playerDecks.get(i).getComponents());
        }
    }

    @Override
    protected AbstractGameState copy(int playerId) {
        return null;
    }

    @Override
    public VectorObservation getVectorObservation() {
        return null;
    }

    @Override
    public double[] getDistanceFeatures(int playerId) {
        return new double[0];
    }

    @Override
    public HashMap<HashMap<Integer, Double>, Utils.GameResult> getTerminalFeatures(int playerId) {
        return null;
    }

    @Override
    public double getScore(int playerId) {
        return 0;
    }

    public ColtExpressGameState(ColtExpressParameters gameParameters, int nPlayers) {
        super(gameParameters, new ColtExpressTurnOrder(nPlayers));
        gamePhase = ColtExpressGamePhase.DrawCards;
    }

    /*
    public void setComponents(ColtExpressParameters gameParameters) {
        train = new Train(getNPlayers());
        playerCharacters = new HashMap<>();

        // give each player a single card
        playerDecks = new ArrayList<>(getNPlayers());
        playerHandCards = new ArrayList<>(getNPlayers());
        playerLoot = new ArrayList<>(getNPlayers());
        bulletsLeft = new int[getNPlayers()];
        plannedActions = new PartialObservableDeck<>("plannedActions", getNPlayers());

        Arrays.fill(bulletsLeft, 6);
        for (int playerIndex = 0; playerIndex < getNPlayers(); playerIndex++) {
            CharacterType characterType = ((ColtExpressForwardModel)forwardModel).pickRandomCharacterType();
            playerCharacters.put(playerIndex, characterType);
            if (characterType == CharacterType.Belle)
                playerPlayingBelle = playerIndex;

            boolean[] visibility = new boolean[getNPlayers()];
            Arrays.fill(visibility, !PARTIAL_OBSERVABLE);
            visibility[playerIndex] = true;

            PartialObservableDeck<ColtExpressCard> playerDeck =
                    new PartialObservableDeck<>("playerDeck"+playerIndex, visibility);
            for (ColtExpressCard.CardType type : gameParameters.cardCounts.keySet()){
                for (int j = 0; j < gameParameters.cardCounts.get(type); j++)
                    playerDeck.add(new ColtExpressCard(playerIndex, type));
            }
            playerDecks.add(playerDeck);
            playerDeck.shuffle();

            PartialObservableDeck<ColtExpressCard> playerHand = new PartialObservableDeck<>(
                    "playerHand"+playerIndex, visibility);

            playerHandCards.add(playerHand);

            PartialObservableDeck<Loot> loot = new PartialObservableDeck<>("playerLoot"+playerIndex, visibility);
            loot.add(new Loot(Loot.LootType.Purse, 250));
            playerLoot.add(loot);

            if (playerIndex%2 == 0)
                train.getCompartment(0).addPlayerInside(playerIndex);
            else
                train.getCompartment(1).addPlayerInside(playerIndex);

        }
        distributeCards();
    }
    */

    public void distributeCards(){
        for (int playerIndex = 0; playerIndex < getNPlayers(); playerIndex++) {
            PartialObservableDeck<ColtExpressCard> playerHand = playerHandCards.get(playerIndex);
            PartialObservableDeck<ColtExpressCard> playerDeck = playerDecks.get(playerIndex);

            playerDeck.add(playerHand);
            playerHand.clear();

            for (int i = 0; i < 6; i++)
                playerHand.add(playerDeck.draw());
            if (playerCharacters.get(playerIndex) == CharacterType.Doc)
                playerHand.add(playerDeck.draw());
        }
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

//    private ArrayList<AbstractAction> playerActions(int playerID) {
//        ArrayList<AbstractAction> actions = new ArrayList<>();
//
//        // add end turn by drawing a card
//        return actions;
//    }

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
        System.out.println(train.toString());

        System.out.println();
        System.out.print("Planned Actions: ");
        System.out.println(plannedActions.toString());

        System.out.println();
        System.out.println(turnOrder.toString());

        System.out.println();
        System.out.println("Current GamePhase: " + gamePhase);
    }
}
