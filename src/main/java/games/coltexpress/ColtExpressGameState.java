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
import core.components.PartialObservableDeck;
import utilities.Utils;
import games.coltexpress.ColtExpressTypes.*;

import java.util.*;


public class ColtExpressGameState extends AbstractGameState implements IObservation, IPrintable {

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
    private final LinkedList<Compartment> trainCompartments;

    @Override
    public void addAllComponents() {
        allComponents.putComponent(plannedActions);
        allComponents.putComponents(trainCompartments);
        for (Compartment compartment: trainCompartments) {
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
        super(gameParameters, model, new ColtExpressTurnOrder(nPlayers, gameParameters));
        gamePhase = ColtExpressGamePhase.DrawCards;
        trainCompartments = new LinkedList<>();
    }

    @Override
    public IObservation getObservation(int player) {
        return this;
    }

    @Override
    public void endGame() {
        ColtExpressParameters cep = (ColtExpressParameters) gameParameters;
        
        this.gameStatus = Utils.GameResult.GAME_END;
        Arrays.fill(playerResults, Utils.GameResult.GAME_LOSE);

        int[] pointsPerPlayer = new int[getNPlayers()];
        int[] bulletCardsPerPlayer = new int[getNPlayers()];

        List<Integer> playersWithMostSuccessfulShots = new LinkedList<>();
        int bestValue = cep.nBulletsPerPlayer;
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
            pointsPerPlayer[bestShooter] += cep.shooterReward;

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

    @Override
    public List<AbstractAction> computeAvailableActions() {

        ArrayList<AbstractAction> actions = new ArrayList<>();
        if (ColtExpressGamePhase.DraftCharacter.equals(gamePhase)) {
            System.out.println("character drafting is not implemented yet");
//            actions = drawAction();
        } else if (ColtExpressGamePhase.DrawCards.equals(gamePhase)) {
//            actions = drawAction(); TODO: this done automatically?
        } else if (ColtExpressGamePhase.PlanActions.equals(gamePhase)) {
            actions = schemingActions();
        } else if (ColtExpressGamePhase.ExecuteActions.equals(gamePhase)) {
            actions = stealingActions();
        }

        return actions;
    }

    public ArrayList<AbstractAction> schemingActions(){
        ColtExpressParameters cep = (ColtExpressParameters)gameParameters;
        int player = turnOrder.getCurrentPlayer(this);
        ArrayList<AbstractAction> actions = new ArrayList<>();
        for (ColtExpressCard c : playerHandCards.get(player).getComponents()){
            if (c.cardType == ColtExpressCard.CardType.Bullet)
                continue;

            // ghost can play a card hidden during the first turn
            boolean hidden = ((ColtExpressTurnOrder) turnOrder).isHiddenTurn() ||
                    (playerCharacters.get(player) == CharacterType.Ghost &&
                    ((ColtExpressTurnOrder) turnOrder).getCurrentRoundCardIndex() == 0);

            actions.add(new SchemeAction(playerHandCards.get(player).getComponentID(),
                    plannedActions.getComponentID(), hidden));
        }
        actions.add(new DrawCardsAction(playerHandCards.get(player).getComponentID(), playerDecks.get(player).getComponentID(), cep.nCardsDraw));
        return actions;
    }

    public ArrayList<AbstractAction> stealingActions()
    {
        int player = turnOrder.getCurrentPlayer(this);
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
                    for (Compartment compartment : trainCompartments) {
                        if (compartment.playersInsideCompartment.contains(player) ||
                                compartment.playersOnTopOfCompartment.contains(player)) {
                            boolean toRoof = compartment.playersInsideCompartment.contains(player);
                            actions.add(new MoveVerticalAction(plannedActions.getComponentID(),
                                    playerDecks.get(player).getComponentID(), compartment.getComponentID(), toRoof));
                            break;
                        }
                    }
                    break;
                case MoveMarshal:
                    for (int i = 0; i < trainCompartments.size(); i++){
                        Compartment compartment = trainCompartments.get(i);
                        if (compartment.containsMarshal){
                            if (i > 1)
                                actions.add(new MoveMarshalAction(plannedActions.getComponentID(),
                                        playerDecks.get(player).getComponentID(), compartment.getComponentID(),
                                        trainCompartments.get(i-1).getComponentID()));
                            if (i < trainCompartments.size() - 1)
                                actions.add(new MoveMarshalAction(plannedActions.getComponentID(),
                                        playerDecks.get(player).getComponentID(), compartment.getComponentID(),
                                        trainCompartments.get(i+1).getComponentID()));

                            break;
                        }
                    }
                    break;
                case CollectMoney:
                    PartialObservableDeck<Loot> availableLoot = null;
                    for (Compartment compartment : trainCompartments) {
                        if (compartment.playersOnTopOfCompartment.contains(player))
                            availableLoot = compartment.lootOnTop;
                        else if (compartment.playersInsideCompartment.contains(player))
                            availableLoot = compartment.lootInside;
                        if (availableLoot != null) {
                            for (Loot loot : availableLoot.getComponents()) {
                                actions.add(new CollectMoneyAction(plannedActions.getComponentID(),
                                        playerDecks.get(player).getComponentID(), loot.getComponentID(), availableLoot.getComponentID()));
                            }
                        }
                    }
                    if (actions.size() == 0) {
                        actions.add(new CollectMoneyAction(plannedActions.getComponentID(),
                                playerDecks.get(player).getComponentID(), -1, -1));
                    }
                    break;
                case MoveSideways:
                    for (int i = 0; i < trainCompartments.size(); i++){
                        Compartment compartment = trainCompartments.get(i);
                        if (compartment.playersOnTopOfCompartment.contains(player)){
                            for (int offset = 1; offset < ((ColtExpressParameters)gameParameters).nRoofMove; offset++){
                                if ((i-offset) > 0) {
                                    actions.add(new MoveSidewaysAction(plannedActions.getComponentID(),
                                            playerDecks.get(player).getComponentID(), compartment.getComponentID(),
                                            trainCompartments.get(i-offset).getComponentID()));
                                }
                                if ((i+offset) < trainCompartments.size()) {
                                    actions.add(new MoveSidewaysAction(plannedActions.getComponentID(),
                                            playerDecks.get(player).getComponentID(), compartment.getComponentID(),
                                            trainCompartments.get(i+offset).getComponentID()));
                                }
                            }
                            break;
                        }
                        else if (compartment.playersInsideCompartment.contains(player)){
                            if ((i-1) > 0) {
                                actions.add(new MoveSidewaysAction(plannedActions.getComponentID(),
                                        playerDecks.get(player).getComponentID(), compartment.getComponentID(),
                                        trainCompartments.get(i-1).getComponentID()));
                            }
                            if ((i+1) < trainCompartments.size()) {
                                actions.add(new MoveSidewaysAction(plannedActions.getComponentID(),
                                        playerDecks.get(player).getComponentID(), compartment.getComponentID(),
                                        trainCompartments.get(i+1).getComponentID()));
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

        for (int i = 0; i < trainCompartments.size(); i++)
        {
            Compartment compartment = trainCompartments.get(i);
            if (compartment.playersOnTopOfCompartment.contains(player)) {
                for (Integer targetID : compartment.playersOnTopOfCompartment){
                    if (targetID != player)
                        availableTargets.add(targetID);
                }

                playerCompartmentIndex = i;
                playerCompartment = compartment;
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

        if (availableTargets.size() > 1)
            availableTargets.remove(playerPlayingBelle);
        boolean playerIsCheyenne = playerCharacters.get(player) == CharacterType.Django;

        // punch forward or backward
        for (int offset = -1; offset <= 1; offset++){
            if (offset == 0 || playerCompartmentIndex+offset < 0 || playerCompartmentIndex+offset >= trainCompartments.size())
                continue;
            Compartment targetCompartment = trainCompartments.get(playerCompartmentIndex+offset);
            for (Integer targetPlayer : availableTargets){
                PartialObservableDeck<Loot> availableLoot = playerLoot.get(targetPlayer);

                if (availableLoot.getSize() > 0){
                    for (Loot loot : availableLoot.getComponents())
                    {
                        actions.add(new PunchAction(plannedActions.getComponentID(),
                                playerDecks.get(player).getComponentID(), targetPlayer,
                                playerCompartment.getComponentID(), targetCompartment.getComponentID(),
                                loot.getComponentID(), availableLoot.getComponentID(), playerIsCheyenne));
                    }
                }
                else {
                    // punch opponent that cannot drop anymore loot
                    actions.add(new PunchAction(plannedActions.getComponentID(),
                            playerDecks.get(player).getComponentID(), targetPlayer,
                            playerCompartment.getComponentID(), targetCompartment.getComponentID(),
                            -1, -1, playerIsCheyenne));
                }
            }
        }

        if (actions.size() == 0)
            actions.add(new PunchAction(plannedActions.getComponentID(),
                    playerDecks.get(player).getComponentID(), -1, -1, -1,
                    -1, -1, playerIsCheyenne));
    }

    private void createShootingActions(ColtExpressCard card, ArrayList<AbstractAction> actions, int player) {
        int playerCompartmentIndex = 0;
        Compartment playerCompartment = null;
        boolean playerOnTop = false;
        for (int i = 0; i < trainCompartments.size(); i++)
        {
            Compartment compartment = trainCompartments.get(i);
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
        if (playerCompartment != null) {

            HashMap<Integer, Compartment> targets = new HashMap<>();

            if (playerOnTop) {
                //shots in rear direction
                for (int offset = 1; playerCompartmentIndex - offset >= 0; offset++) {
                    Compartment targetCompartment = trainCompartments.get(playerCompartmentIndex - offset);
                    if (targetCompartment.playersOnTopOfCompartment.size() > 0) {
                        for (Integer target : targetCompartment.playersOnTopOfCompartment)
                            targets.put(target, targetCompartment);
                        break;
                    }
                }

                //shots to the front of the train
                for (int offset = 1; playerCompartmentIndex + offset < trainCompartments.size(); offset++) {
                    Compartment targetCompartment = trainCompartments.get(playerCompartmentIndex + offset);
                    if (targetCompartment.playersOnTopOfCompartment.size() > 0) {
                        for (Integer target : targetCompartment.playersOnTopOfCompartment)
                            targets.put(target, targetCompartment);
                        break;
                    }
                }

                //add player below if your are tuco
                if (playerCharacters.get(player) == CharacterType.Tuco) {
                    for (Integer target : trainCompartments.get(playerCompartmentIndex).playersInsideCompartment)
                        targets.put(target, playerCompartment);
                }
            } else {
                if (playerCompartmentIndex - 1 >= 0) {
                    Compartment targetCompartment = trainCompartments.get(playerCompartmentIndex - 1);
                    if (targetCompartment.playersInsideCompartment.size() > 0) {
                        for (Integer target : targetCompartment.playersInsideCompartment)
                            targets.put(target, targetCompartment);
                    }
                }

                if (playerCompartmentIndex + 1 < trainCompartments.size()) {
                    Compartment targetCompartment = trainCompartments.get(playerCompartmentIndex + 1);
                    if (targetCompartment.playersInsideCompartment.size() > 0) {
                        for (Integer target : targetCompartment.playersInsideCompartment)
                            targets.put(target, targetCompartment);
                    }
                }

                // Add player below if your are tuco
                if (playerCharacters.get(player) == CharacterType.Tuco) {
                    for (Integer target : trainCompartments.get(playerCompartmentIndex).playersOnTopOfCompartment)
                        targets.put(target, playerCompartment);
                }
            }

            if (targets.size() > 1)
                targets.remove(playerPlayingBelle);

            boolean playerIsDjango = playerCharacters.get(player) == CharacterType.Django;
            for (Map.Entry<Integer, Compartment> entry : targets.entrySet()) {
                actions.add(new ShootPlayerAction(plannedActions.getComponentID(), playerDecks.get(player).getComponentID(),
                        playerCompartment.getComponentID(), entry.getValue().getComponentID(), entry.getKey(), playerIsDjango));
            }

            if (actions.size() == 0)
                actions.add(new ShootPlayerAction(plannedActions.getComponentID(), playerDecks.get(player).getComponentID(),
                        playerCompartment.getComponentID(),-1, -1, playerIsDjango));
        }
    }

    void setupTrain() {
        // Choose random compartment configurations
        Random random = new Random(gameParameters.getGameSeed());
        ArrayList<Integer> availableCompartments = new ArrayList<>();
        for (int i = 0; i < ((ColtExpressParameters)gameParameters).trainCompartmentConfigurations.size() - 1; i++) {
            availableCompartments.add(i);
        }
        for (int i = 0; i < getNPlayers(); i++) {
            int which = random.nextInt(availableCompartments.size());
            trainCompartments.add(new Compartment(getNPlayers(), i, which, (ColtExpressParameters)gameParameters));
            availableCompartments.remove(Integer.valueOf(which));
        }

        // Add locomotive
        trainCompartments.add(Compartment.createLocomotive(getNPlayers(), (ColtExpressParameters) gameParameters));
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
