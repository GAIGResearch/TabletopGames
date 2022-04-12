package games.coltexpress;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IGamePhase;
import core.interfaces.IPrintable;
import games.GameType;
import games.coltexpress.ColtExpressTypes.CharacterType;
import games.coltexpress.actions.roundcardevents.RoundEvent;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.cards.RoundCard;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;
import utilities.Pair;
import utilities.Utils;

import java.util.*;

import static core.CoreConstants.VisibilityMode;
import static java.util.stream.Collectors.toList;
import static utilities.Utils.GameResult.WIN;

public class ColtExpressGameState extends AbstractGameState implements IPrintable {

    // Colt express adds 4 game phases
    public enum ColtExpressGamePhase implements IGamePhase {
        PlanActions,
        ExecuteActions,
        DraftCharacter
    }

    // Cards in player hands
    List<Deck<ColtExpressCard>> playerHandCards;
    // A deck for each player
    List<Deck<ColtExpressCard>> playerDecks;
    List<Deck<Loot>> playerLoot;
    int[] bulletsLeft;
    // The player characters available
    HashMap<Integer, CharacterType> playerCharacters;
    int playerPlayingBelle;

    // The card stack built by players each round
    PartialObservableDeck<ColtExpressCard> plannedActions;
    // The train to loot
    LinkedList<Compartment> trainCompartments;
    // The round cards
    PartialObservableDeck<RoundCard> rounds;

    Random rnd;

    public ColtExpressGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new ColtExpressTurnOrder(nPlayers, ((ColtExpressParameters) gameParameters).nMaxRounds), GameType.ColtExpress);
        gamePhase = ColtExpressGamePhase.PlanActions;
        trainCompartments = new LinkedList<>();
        playerPlayingBelle = -1;
        rnd = new Random(gameParameters.getRandomSeed());
    }

    @Override
    public List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();
        components.add(plannedActions);
        components.addAll(trainCompartments);
        components.addAll(playerHandCards);
        components.addAll(playerDecks);
        components.addAll(playerLoot);
        components.add(rounds);
        return components;
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        ColtExpressGameState copy = new ColtExpressGameState(gameParameters, getNPlayers());

        // These are always visible
        copy.bulletsLeft = bulletsLeft.clone();
        copy.playerCharacters = new HashMap<>(playerCharacters);
        copy.playerPlayingBelle = playerPlayingBelle;

        // These are modified in PO
        copy.playerHandCards = new ArrayList<>();
        for (Deck<ColtExpressCard> d : playerHandCards) {
            copy.playerHandCards.add(d.copy());
        }
        copy.playerDecks = new ArrayList<>();
        for (Deck<ColtExpressCard> d : playerDecks) {
            copy.playerDecks.add(d.copy());
        }
        copy.playerLoot = new ArrayList<>();
        for (Deck<Loot> d : playerLoot) {
            copy.playerLoot.add(d.copy());
        }
        copy.plannedActions = plannedActions.copy();
        copy.rounds = rounds.copy();
        copy.trainCompartments = new LinkedList<>();
        for (Compartment d : trainCompartments) {
            copy.trainCompartments.add((Compartment) d.copy());
        }

        if (getCoreGameParameters().partialObservable && playerId != -1) {
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    // Other player hands are hidden, but it's known what's in a player's deck
                    // Shuffle together and deal new hands for opponents (same hand size)
                    copy.playerDecks.get(i).add(copy.playerHandCards.get(i));
                    int nCardsInHand = copy.playerHandCards.get(i).getSize();
                    copy.playerHandCards.get(i).clear();
                    copy.playerDecks.get(i).shuffle(rnd);
                    for (int j = 0; j < nCardsInHand; j++) {
                        copy.playerHandCards.get(i).add(copy.playerDecks.get(i).draw());
                    }
                }
                // All loot is hidden
                Deck<Loot> dLoot = copy.playerLoot.get(i);
                dLoot.clear();
                for (int j = 0; j < playerLoot.get(i).getSize(); j++) {
//                    dLoot.add(new Loot(LootType.Unknown, 0));  // Unknown loot

                    // Random value for loot of this same type
                    Loot realLoot = playerLoot.get(i).get(j);
                    ArrayList<Pair<Integer, Integer>> lootOptions = ((ColtExpressParameters) copy.gameParameters).loot.get(realLoot.getLootType());
                    int randomValue = lootOptions.get(rnd.nextInt(lootOptions.size())).a;
                    dLoot.add(new Loot(realLoot.getLootType(), randomValue));
                }
            }

            // All loot in train is also hidden
            for (int i = 0; i < trainCompartments.size(); i++) {
                Compartment realCompartment = trainCompartments.get(i);
                Compartment copyCompartment = copy.trainCompartments.get(i);
                copyCompartment.lootOnTop.clear();
                copyCompartment.lootInside.clear();
                for (int j = 0; j < realCompartment.lootOnTop.getSize(); j++) {
                    // Random value for loot of this same type
                    Loot realLoot = realCompartment.lootOnTop.get(j);
                    ArrayList<Pair<Integer, Integer>> lootOptions = ((ColtExpressParameters) copy.gameParameters).loot.get(realLoot.getLootType());
                    int randomValue = lootOptions.get(rnd.nextInt(lootOptions.size())).a;
                    copyCompartment.lootOnTop.add(new Loot(realLoot.getLootType(), randomValue));
                }
                for (int j = 0; j < realCompartment.lootInside.getSize(); j++) {
                    // Random value for loot of this same type
                    Loot realLoot = realCompartment.lootInside.get(j);
                    ArrayList<Pair<Integer, Integer>> lootOptions = ((ColtExpressParameters) copy.gameParameters).loot.get(realLoot.getLootType());
                    int randomValue = lootOptions.get(rnd.nextInt(lootOptions.size())).a;
                    copyCompartment.lootInside.add(new Loot(realLoot.getLootType(), randomValue));
                }
            }

            // Some planned actions may be hidden, put them back in owner player's deck, shuffle decks and replace with
            // random options

            // First we add in the actions that are visible at the right index position
            // cardReplacements stores the cards that we have used so far, to avoid inserting the same card twice
            HashMap<Integer, ArrayList<Integer>> cardReplacements = new HashMap<>();
            for (int i = 0; i < plannedActions.getSize(); i++) {
                if (!plannedActions.isComponentVisible(i, playerId)) {
                    int p = plannedActions.get(i).playerID;
                    if (!cardReplacements.containsKey(p)) {
                        cardReplacements.put(p, new ArrayList<>());
                    }
                    cardReplacements.get(p).add(i);
                    copy.playerDecks.get(p).add(plannedActions.get(i));
                }
            }

            // Then we randomise the invisible ones
            for (Map.Entry<Integer, ArrayList<Integer>> e : cardReplacements.entrySet()) {
                // loop over each player, and shuffle their decks (which now includes all cards we can't see)
                copy.playerDecks.get(e.getKey()).shuffle(rnd);
                Deck<ColtExpressCard> bulletCards = new Deck<>("tempDeck", VisibilityMode.HIDDEN_TO_ALL);
                for (int i : e.getValue()) {
                    // This might be a bullet card...
                    ColtExpressCard topCard = copy.playerDecks.get(e.getKey()).draw();
                    if (topCard.cardType == ColtExpressCard.CardType.Bullet) {
                        // invalid to be a plannedAction
                        bulletCards.add(topCard);
                    } else {
                        copy.plannedActions.setComponent(i, topCard);
                    }
                }
                // then we put the bullet cards back into the player deck and reshuffle
                copy.playerDecks.get(e.getKey()).add(bulletCards);
                copy.playerDecks.get(e.getKey()).shuffle(rnd);
            }

            // Round cards are hidden for subsequent rounds, randomize those
            // We exclude any visible RoundCard
            List<RoundCard> exclusionList = copy.rounds.getVisibleComponents(playerId).stream()
                    .filter(Objects::nonNull).collect(toList());
            for (int i = 0; i < rounds.getSize(); i++) {
                if (!rounds.isComponentVisible(i, playerId)) {
                    if (i == rounds.getSize() - 1) { // last card, so use an End Round Card
                        copy.rounds.setComponent(i, getRandomEndRoundCard((ColtExpressParameters) getGameParameters()));
                    } else {
                        copy.rounds.setComponent(i, getRandomRoundCard((ColtExpressParameters) getGameParameters(), i, exclusionList));
                        exclusionList.add(copy.rounds.get(i));
                    }
                }
            }
        }

        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return new ColtExpressHeuristic().evaluateState(this, playerId);
    }

    @Override
    public double getGameScore(int playerId) {
        double retValue = getLoot(playerId).sumInt(Loot::getValue);
        if (getBestShooters().contains(playerId)) {
            ColtExpressTurnOrder ceto = (ColtExpressTurnOrder) turnOrder;
            ColtExpressParameters cep = (ColtExpressParameters) gameParameters;
            // the full shooter reward is given at the end of the game
            // so we only partially incorporate this into the 'score'
            double gameProgress = ceto.getRoundCounter() / (double) cep.nMaxRounds;
            if (!isNotTerminal() && gameProgress != 1.0)
                throw new AssertionError("Unexpected");
            retValue += cep.shooterReward * gameProgress;
        }
    return retValue;
    }

    @Override
    public double getTiebreak(int playerId) {
        // we use the number of bullet cards
        // in the players own deck and hands
        // fewer is better - so we return a negative number
        int bulletsTaken = 0;
        for (ColtExpressCard card : playerDecks.get(playerId).getComponents())
            if (card.cardType == ColtExpressCard.CardType.Bullet)
                bulletsTaken++;
        for (ColtExpressCard card : playerHandCards.get(playerId).getComponents())
            if (card.cardType == ColtExpressCard.CardType.Bullet)
                bulletsTaken++;
        return -bulletsTaken;
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
        rounds = new PartialObservableDeck<>("Rounds", -1, getNPlayers());
        gamePhase = ColtExpressGamePhase.PlanActions;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColtExpressGameState)) return false;
        if (!super.equals(o)) return false;
        ColtExpressGameState gameState = (ColtExpressGameState) o;
        return playerPlayingBelle == gameState.playerPlayingBelle &&
                Objects.equals(playerHandCards, gameState.playerHandCards) &&
                Objects.equals(playerDecks, gameState.playerDecks) &&
                Objects.equals(playerLoot, gameState.playerLoot) &&
                Arrays.equals(bulletsLeft, gameState.bulletsLeft) &&
                Objects.equals(playerCharacters, gameState.playerCharacters) &&
                Objects.equals(plannedActions, gameState.plannedActions) &&
                Objects.equals(trainCompartments, gameState.trainCompartments) &&
                Objects.equals(rounds, gameState.rounds);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), playerHandCards, playerDecks, playerLoot, playerCharacters, playerPlayingBelle, plannedActions, trainCompartments, rounds);
        result = 31 * result + Arrays.hashCode(bulletsLeft);
        return result;
    }

    /**
     * Adds loot collected by player
     *
     * @param playerID - ID of player collecting loot
     * @param loot     - loot collected
     */
    public void addLoot(Integer playerID, Loot loot) {
        playerLoot.get(playerID).add(loot);
    }

    /**
     * Adds a neutral bullet for player
     *
     * @param playerID - ID of player receiving the bullet
     */
    public void addNeutralBullet(Integer playerID) {
        addBullet(playerID, -1);
    }

    /**
     * Adds a bullet from another player
     *
     * @param playerID  - player receiving the bullet
     * @param shooterID - player sending the bullet
     */
    public void addBullet(Integer playerID, Integer shooterID) {
        if (shooterID == -1 || bulletsLeft[shooterID] > 0) {
            this.playerDecks.get(playerID).add(new ColtExpressCard(shooterID, ColtExpressCard.CardType.Bullet));
            if (playerCharacters.containsKey(shooterID))
                bulletsLeft[shooterID]--;
        }
    }

    /**
     * Calculates the current best shooters depending on the number of bullets left per player
     *
     * @return - list of player IDs tied for lowest number of bullets left
     */
    public List<Integer> getBestShooters() {
        ColtExpressParameters cep = (ColtExpressParameters) gameParameters;
        List<Integer> playersWithMostSuccessfulShots = new LinkedList<>();
        int bestValue = cep.nBulletsPerPlayer;
        for (int i = 0; i < getNPlayers(); i++) {
            if (bulletsLeft[i] < bestValue) {
                bestValue = bulletsLeft[i];
                playersWithMostSuccessfulShots.clear();
                playersWithMostSuccessfulShots.add(i);
            } else if (bulletsLeft[i] == bestValue) {
                playersWithMostSuccessfulShots.add(i);
            }
        }
        return playersWithMostSuccessfulShots;
    }

    // Getters, setters
    public LinkedList<Compartment> getTrainCompartments() {
        return trainCompartments;
    }

    public Deck<Loot> getLoot(int playerID) {
        return playerLoot.get(playerID);
    }

    public PartialObservableDeck<ColtExpressCard> getPlannedActions() {
        return plannedActions;
    }

    public List<Deck<ColtExpressCard>> getPlayerDecks() {
        return playerDecks;
    }

    public PartialObservableDeck<RoundCard> getRounds() {
        return rounds;
    }

    public HashMap<Integer, CharacterType> getPlayerCharacters() {
        return playerCharacters;
    }

    public List<Deck<ColtExpressCard>> getPlayerHandCards() {
        return playerHandCards;
    }

    public int[] getBulletsLeft() {
        return bulletsLeft;
    }

    public List<Deck<Loot>> getPlayerLoot() {
        return playerLoot;
    }

    @Override
    public void printToConsole() {
        System.out.println("Colt Express Game-State");
        System.out.println("=======================");

        int currentPlayer = turnOrder.getCurrentPlayer(this);

        for (int i = 0; i < getNPlayers(); i++) {
            if (currentPlayer == i)
                System.out.print(">>> ");
            System.out.print("Player " + i + " = " + playerCharacters.get(i).name() + ":  ");
            System.out.print("Hand=");
            System.out.print(playerHandCards.get(i).toString());
            System.out.print("; Deck=");
            System.out.print(playerDecks.get(i).toString());
            System.out.print("; Loot=");
            System.out.print(playerLoot.get(i).toString());
            System.out.println();
        }
        System.out.println();
        System.out.println(printTrain());

        System.out.println();
        System.out.print("Planned Actions: ");
        System.out.println(plannedActions.toString());

        System.out.println();
        int i = 0;
        for (RoundCard round : rounds.getComponents()) {
            if (i == turnOrder.getRoundCounter()) {
                System.out.print("->");
            }
            System.out.print(round.toString());
            System.out.print(", ");
            i++;
        }

        System.out.println();
        System.out.println("Current GamePhase: " + gamePhase);
    }

    public String printTrain() {
        StringBuilder sb = new StringBuilder();
        sb.append("Train:\n");
        for (Compartment compartment : trainCompartments) {
            sb.append(compartment.toString());
            sb.append("\n");
        }
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }

    /**
     * Helper getter methods for round card composition.
     */

    RoundCard getRandomEndRoundCard(ColtExpressParameters cep) {
        int nEndCards = cep.endRoundCards.length;
        int choice = rnd.nextInt(nEndCards);
        return getEndRoundCard(cep, choice);
    }

    RoundCard getEndRoundCard(ColtExpressParameters cep, int idx) {
        if (idx >= 0 && idx < cep.endRoundCards.length) {
            RoundCard.TurnType[] turnTypes = cep.endRoundCards[idx].getTurnTypeSequence();
            RoundEvent event = cep.endRoundCards[idx].getEndCardEvent();
            return new RoundCard(cep.endRoundCards[idx].name(), turnTypes, event);
        }
        return null;
    }

    RoundCard getRandomRoundCard(ColtExpressParameters cep, int i, List<RoundCard> exclusionList) {
        List<String> namesToExclude = exclusionList.stream().map(RoundCard::getComponentName).collect(toList());
        List<ColtExpressTypes.RegularRoundCard> availableTypes = Arrays.stream(cep.roundCards)
                .filter(rc -> !namesToExclude.contains(rc.name())).collect(toList());
        int nRoundCards = availableTypes.size();
        int choice = rnd.nextInt(nRoundCards);
        return getRoundCard(availableTypes.get(choice), getNPlayers());
    }

    RoundCard getRoundCard(ColtExpressTypes.RegularRoundCard cardType, int nPlayers) {
        RoundCard.TurnType[] turnTypes = cardType.getTurnTypeSequence(nPlayers);
        RoundEvent event = cardType.getEndCardEvent();
        return new RoundCard(cardType.name(), turnTypes, event);
    }

}
