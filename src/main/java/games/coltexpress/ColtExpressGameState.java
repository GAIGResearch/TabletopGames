package games.coltexpress;

import core.AbstractGameState;
import core.AbstractForwardModel;
import core.interfaces.IGamePhase;
import core.actions.AbstractAction;
import core.interfaces.IObservation;
import core.interfaces.IPrintable;
import games.coltexpress.actions.*;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;
import games.coltexpress.components.Train;
import core.components.PartialObservableDeck;
import utilities.Utils;
import games.coltexpress.ColtExpressParameters.CharacterType;

import java.util.*;

public class ColtExpressGameState extends AbstractGameState implements IObservation, IPrintable {

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

    public ColtExpressGameState(ColtExpressParameters gameParameters, AbstractForwardModel model, int nPlayers) {
        super(gameParameters, model, new ColtExpressTurnOrder(nPlayers));
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

    @Override
    public IObservation getObservation(int player) {
        return this;
    }

    @Override
    public void endGame() {
        this.gameStatus = Utils.GameResult.GAME_END;
        Arrays.fill(playerResults, Utils.GameResult.GAME_LOSE);

        int[] pointsPerPlayer = new int[getNPlayers()];
        int[] bulletCardsPerPlayer = new int[getNPlayers()];

        List<Integer> playersWithMostSuccessfulShots = new LinkedList<>();
        int bestValue = 6;
        for (int i = 0; i < getNPlayers(); i++) {
            for (Loot loot : playerLoot.get(i).getComponents())
                pointsPerPlayer[i] += loot.getValue();
            for (ColtExpressCard card : playerDecks.get(i).getComponents())
                if (card.cardType == ColtExpressCard.CardType.Bullet)
                    bulletCardsPerPlayer[i]++;
            for (ColtExpressCard card : playerHandCards.get(i).getComponents())
                if (card.cardType == ColtExpressCard.CardType.Bullet)
                    bulletCardsPerPlayer[i]++;

            if (bulletsLeft[i] < bestValue){
                bestValue = bulletsLeft[i];
                playersWithMostSuccessfulShots.clear();
                playersWithMostSuccessfulShots.add(i);
            } else if (bulletsLeft[i] == bestValue) {
                playersWithMostSuccessfulShots.add(i);
            }
        }

        for (Integer bestShooter : playersWithMostSuccessfulShots)
            pointsPerPlayer[bestShooter] += 1000;

        LinkedList<Integer> potentialWinnersByPoints = new LinkedList<>();
        bestValue = 0;
        for (int i = 0; i < getNPlayers(); i++) {
            if (pointsPerPlayer[i] > bestValue){
                bestValue = pointsPerPlayer[i];
                potentialWinnersByPoints.clear();
                potentialWinnersByPoints.add(i);
            } else if (pointsPerPlayer[i] == bestValue) {
                potentialWinnersByPoints.add(i);
            }
        }
        if (potentialWinnersByPoints.size() == 1)
        {
            for (Integer playerID : potentialWinnersByPoints)
                playerResults[playerID] = Utils.GameResult.GAME_WIN;
            return;
        }

        //In case of a tie, the winner is the tied player who has received the fewest
        // Bullet cards from other players and Events during the game.
        LinkedList<Integer> potentialWinnerByBulletCards = new LinkedList<>();
        bestValue = -1;
        for (Integer potentialWinner : potentialWinnersByPoints) {
            if (bestValue == -1 || bulletCardsPerPlayer[potentialWinner] < bestValue){
                bestValue = bulletCardsPerPlayer[potentialWinner];
                potentialWinnerByBulletCards.clear();
                potentialWinnerByBulletCards.add(potentialWinner);
            } else if (bulletCardsPerPlayer[potentialWinner] == bestValue) {
                potentialWinnerByBulletCards.add(potentialWinner);
            }
        }

        for (Integer playerID : potentialWinnerByBulletCards)
            playerResults[playerID] = Utils.GameResult.GAME_WIN;

    }

    public ArrayList<AbstractAction> schemingActions(int player){
        ArrayList<AbstractAction> actions = new ArrayList<>();
        for (ColtExpressCard card : playerHandCards.get(player).getComponents()){
            if (card.cardType == ColtExpressCard.CardType.Bullet)
                continue;

            actions.add(new SchemeAction(card, playerHandCards.get(player),
                    plannedActions, ((ColtExpressTurnOrder) turnOrder).isHiddenTurn()));

            // ghost can play a card hidden during the first turn
            if (playerCharacters.get(player) == CharacterType.Ghost &&
                    !((ColtExpressTurnOrder) turnOrder).isHiddenTurn() &&
                    ((ColtExpressTurnOrder) turnOrder).getCurrentRoundCardIndex() == 0)
                actions.add(new SchemeAction(card, playerHandCards.get(player),
                        plannedActions, true));
        }
        actions.add(new DrawCardsAction(playerHandCards.get(player), playerDecks.get(player)));
        return actions;
    }

    public ArrayList<AbstractAction> stealingActions(int player)
    {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        if (plannedActions.getSize() == 0)
            return actions;

        ColtExpressCard plannedActionCard = plannedActions.peek(0);
        if (player == plannedActionCard.playerID)
        {
            switch (plannedActionCard.cardType){
                case Punch:
                    createPunchingActions(plannedActionCard, actions, player);
                    break;
                case Shoot:
                    if (bulletsLeft[player] <= 0)
                        break;
                    else
                        createShootingActions(plannedActionCard, actions, player);
                    break;
                case MoveUp:
                    for (int i = 0; i < train.getSize(); i++){
                        Compartment compartment = train.getCompartment(i);
                        if (compartment.playersOnTopOfCompartment.contains(player)){
                            actions.add(new MoveVerticalAction(plannedActionCard, plannedActions,
                                    playerDecks.get(player), compartment, false));
                            break;
                        }
                        else if (compartment.playersInsideCompartment.contains(player)){
                            actions.add(new MoveVerticalAction(plannedActionCard, plannedActions,
                                    playerDecks.get(player), compartment, true));
                            break;
                        }
                    }
                    break;
                case MoveMarshal:
                    for (int i = 0; i < train.getSize(); i++){
                        Compartment compartment = train.getCompartment(i);
                        if (compartment.containsMarshal){
                            if (i-1 > 0)
                                actions.add(new MoveMarshalAction(plannedActionCard, plannedActions,
                                        playerDecks.get(player), compartment, train.getCompartment(i-1)));
                            if (i+1 < train.getSize())
                                actions.add(new MoveMarshalAction(plannedActionCard, plannedActions,
                                        playerDecks.get(player), compartment, train.getCompartment(i+1)));

                            break;
                        }
                    }
                    break;
                case CollectMoney:
                    PartialObservableDeck<Loot> availableLoot = null;
                    for (int i = 0; i < train.getSize(); i++){
                        Compartment compartment = train.getCompartment(i);
                        if (compartment.playersOnTopOfCompartment.contains(player))
                            availableLoot = compartment.lootOnTop;
                        else if (compartment.playersInsideCompartment.contains(player))
                            availableLoot = compartment.lootInside;
                        if (availableLoot != null){
                            for (Loot loot : availableLoot.getComponents())
                            {
                                if (loot.getLootType() == Loot.LootType.Purse){
                                    actions.add(new CollectMoneyAction(plannedActionCard, plannedActions,
                                            playerDecks.get(player), Loot.LootType.Purse, availableLoot));
                                    break;
                                }
                            }
                            for (Loot loot : availableLoot.getComponents())
                            {
                                if (loot.getLootType() == Loot.LootType.Strongbox){
                                    actions.add(new CollectMoneyAction(plannedActionCard, plannedActions,
                                            playerDecks.get(player), Loot.LootType.Strongbox, availableLoot));
                                    break;
                                }
                            }
                            for (Loot loot : availableLoot.getComponents())
                            {
                                if (loot.getLootType() == Loot.LootType.Jewel){
                                    actions.add(new CollectMoneyAction(plannedActionCard, plannedActions,
                                            playerDecks.get(player), Loot.LootType.Jewel, availableLoot));
                                    break;
                                }
                            }
                        }
                    }
                    if (actions.size() == 0)
                        actions.add(new CollectMoneyAction(plannedActionCard, plannedActions,
                                playerDecks.get(player), null, null));
                    break;
                case MoveSideways:
                    for (int i = 0; i < train.getSize(); i++){
                        Compartment compartment = train.getCompartment(i);
                        if (compartment.playersOnTopOfCompartment.contains(player)){
                            for (int offset = 1; offset < 4; offset++){
                                if ((i-offset) > 0) {
                                    actions.add(new MoveSidewaysAction(plannedActionCard, plannedActions,
                                            playerDecks.get(player), compartment,
                                            train.getCompartment(i-offset)));
                                }
                                if ((i+offset) < train.getSize()) {
                                    actions.add(new MoveSidewaysAction(plannedActionCard, plannedActions,
                                            playerDecks.get(player), compartment,
                                            train.getCompartment(i+offset)));
                                }
                            }
                            break;
                        }
                        else if (compartment.playersInsideCompartment.contains(player)){
                            if ((i-1) > 0) {
                                actions.add(new MoveSidewaysAction(plannedActionCard, plannedActions,
                                        playerDecks.get(player), compartment,
                                        train.getCompartment(i-1)));
                            }
                            if ((i+1) < train.getSize()) {
                                actions.add(new MoveSidewaysAction(plannedActionCard, plannedActions,
                                        playerDecks.get(player), compartment,
                                        train.getCompartment(i+1)));
                            }
                            break;
                        }
                    }
                    break;
                case Bullet:
                    throw new IllegalArgumentException("Bullets cannot be played!");
                default:
                    throw new IllegalArgumentException("cardType " + plannedActionCard.cardType + "" +
                            " unknown to ColtExpressGameState");
            }

        }
        return actions;
    }

    private void createPunchingActions(ColtExpressCard card, ArrayList<AbstractAction> actions, int player){
        int playerCompartmentIndex = 0;
        Compartment playerCompartment = null;
        HashSet<Integer> availableTargets = new HashSet<>();
        boolean playerOnTop = false;

        for (int i = 0; i < train.getSize(); i++)
        {
            Compartment compartment = train.getCompartment(i);
            if (compartment.playersOnTopOfCompartment.contains(player)) {
                for (Integer targetID : compartment.playersOnTopOfCompartment){
                    if (targetID != player)
                        availableTargets.add(targetID);
                }

                playerCompartmentIndex = i;
                playerCompartment = compartment;
                playerOnTop = true;
                break;
            } else if (compartment.playersInsideCompartment.contains(player)){
                for (Integer targetID : compartment.playersInsideCompartment){
                    if (targetID != player)
                        availableTargets.add(targetID);
                }

                playerCompartmentIndex = i;
                playerCompartment = compartment;
                break;
            }
        }

        if (availableTargets.size() > 1 && availableTargets.contains(playerPlayingBelle))
            availableTargets.remove(playerPlayingBelle);
        boolean playerIsCheyenne = playerCharacters.get(player) == CharacterType.Django;

        // punch forward
        for (int offset = -1; offset <= 1; offset++){
            if (offset == 0 || playerCompartmentIndex+offset < 0 || playerCompartmentIndex+offset >= train.getSize())
                continue;
            Compartment targetCompartment = train.getCompartment(playerCompartmentIndex+offset);
            for (Integer targetPlayer : availableTargets){
                PartialObservableDeck<Loot> availableLoot = playerLoot.get(targetPlayer);

                if (availableLoot.getSize() > 0){
                    for (Loot loot : availableLoot.getComponents())
                    {
                        if (loot.getLootType() == Loot.LootType.Purse){
                            actions.add(new PunchAction(card, plannedActions,
                                    playerDecks.get(player), targetPlayer, playerCompartment, targetCompartment,
                                    Loot.LootType.Purse, availableLoot, playerIsCheyenne));
                            break;
                        }
                    }
                    for (Loot loot : availableLoot.getComponents())
                    {
                        if (loot.getLootType() == Loot.LootType.Strongbox){
                            actions.add(new PunchAction(card, plannedActions,
                                    playerDecks.get(player), targetPlayer, playerCompartment, targetCompartment,
                                    Loot.LootType.Purse, availableLoot, playerIsCheyenne));
                            break;
                        }
                    }

                    for (Loot loot : availableLoot.getComponents())
                    {
                        if (loot.getLootType() == Loot.LootType.Jewel){
                            actions.add(new PunchAction(card, plannedActions,
                                    playerDecks.get(player), targetPlayer, playerCompartment, targetCompartment,
                                    Loot.LootType.Purse, availableLoot, playerIsCheyenne));
                            break;
                        }
                    }
                }
                else {
                    // punch opponent that cannot drop anymore loot
                    actions.add(new PunchAction(card, plannedActions,
                            playerDecks.get(player), targetPlayer, playerCompartment, targetCompartment,
                            null, null, playerIsCheyenne));
                }
            }
        }



        if (actions.size() == 0)
            actions.add(new PunchAction(card, plannedActions,
                    playerDecks.get(player), -1, null, null,
                    null, null, playerIsCheyenne));
    }

    private void createShootingActions(ColtExpressCard card, ArrayList<AbstractAction> actions, int player) {
        int playerCompartmentIndex = 0;
        Compartment playerCompartment = null;
        boolean playerOnTop = false;
        for (int i = 0; i < train.getSize(); i++)
        {
            Compartment compartment = train.getCompartment(i);
            if (compartment.playersOnTopOfCompartment.contains(player)) {
                playerCompartmentIndex = i;
                playerCompartment = compartment;
                playerOnTop = true;
                break;
            } else if (compartment.playersInsideCompartment.contains(player)){
                playerCompartmentIndex = i;
                playerCompartment = compartment;
                break;
            }
        }

        HashMap<Integer, Compartment> targets = new HashMap<>();

        if (playerOnTop){
            //shots in rear direction
            for (int offset = 1; playerCompartmentIndex-offset >=0; offset++){
                Compartment targetCompartment = train.getCompartment(playerCompartmentIndex-offset);
                if (targetCompartment.playersOnTopOfCompartment.size() > 0){
                    for (Integer target : targetCompartment.playersOnTopOfCompartment)
                        targets.put(target, targetCompartment);
                    break;
                }
            }

            //shots to the front of the train
            for (int offset = 1; playerCompartmentIndex+offset < train.getSize(); offset++){
                Compartment targetCompartment = train.getCompartment(playerCompartmentIndex+offset);
                if (targetCompartment.playersOnTopOfCompartment.size() > 0){
                    for (Integer target : targetCompartment.playersOnTopOfCompartment)
                        targets.put(target, targetCompartment);
                    break;
                }
            }

            //add player below if your are tuco
            if (playerCharacters.get(player) == CharacterType.Tuco){
                for (Integer target : train.getCompartment(playerCompartmentIndex).playersInsideCompartment)
                    targets.put(target, playerCompartment);
            }
        } else {
            if (playerCompartmentIndex - 1 >= 0){
                Compartment targetCompartment = train.getCompartment(playerCompartmentIndex-1);
                if (targetCompartment.playersInsideCompartment.size() > 0){
                    for (Integer target : targetCompartment.playersInsideCompartment)
                        targets.put(target, targetCompartment);
                }
            }

            if (playerCompartmentIndex + 1 < train.getSize()){
                Compartment targetCompartment = train.getCompartment(playerCompartmentIndex+1);
                if (targetCompartment.playersInsideCompartment.size() > 0){
                    for (Integer target : targetCompartment.playersInsideCompartment)
                        targets.put(target, targetCompartment);
                }
            }

            //add player below if your are tuco
            if (playerCharacters.get(player) == CharacterType.Tuco){
                for (Integer target : train.getCompartment(playerCompartmentIndex).playersOnTopOfCompartment)
                    targets.put(target, playerCompartment);
            }
        }

        if (targets.size() > 1 && targets.containsKey(playerPlayingBelle))
            targets.remove(playerPlayingBelle);

        boolean playerIsDjango = playerCharacters.get(player) == CharacterType.Django;
        for (Map.Entry<Integer, Compartment> entry : targets.entrySet()){
            actions.add(new ShootPlayerAction(card, plannedActions, playerDecks.get(player), playerCompartment,
                    entry.getKey(), entry.getValue(), playerIsDjango));
        }

        if (actions.size() == 0)
            actions.add(new ShootPlayerAction(card, plannedActions, playerDecks.get(player), playerCompartment,
                    -1, null, playerIsDjango));
    }

    @Override
    public List<AbstractAction> computeAvailableActions() {

        ArrayList<AbstractAction> actions;
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

    private ArrayList<AbstractAction> drawAction(){
        ArrayList<AbstractAction> actions = new ArrayList<>();
        return actions;
    }

    public ArrayList<AbstractAction> schemingActions(){
        ArrayList<AbstractAction> actions = new ArrayList<>();
        return actions;
    }

    public ArrayList<AbstractAction> stealingActions()
    {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        return actions;
    }

//    private ArrayList<AbstractAction> playerActions(int playerID) {
//        ArrayList<AbstractAction> actions = new ArrayList<>();
//
//        // add end turn by drawing a card
//        return actions;
//    }

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
