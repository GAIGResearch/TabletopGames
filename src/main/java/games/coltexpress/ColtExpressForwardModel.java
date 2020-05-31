package games.coltexpress;

import core.AbstractGameState;
import core.AbstractForwardModel;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import core.interfaces.IGamePhase;
import games.coltexpress.actions.*;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;
import games.coltexpress.components.Train;
import games.coltexpress.ColtExpressParameters.CharacterType;
import utilities.Utils;

import static core.CoreConstants.VERBOSE;

import java.util.*;

import static core.CoreConstants.PARTIAL_OBSERVABLE;

public class ColtExpressForwardModel extends AbstractForwardModel {

    @Override
    public void setup(AbstractGameState firstState) {
        ColtExpressGameState cegs = (ColtExpressGameState) firstState;
        ColtExpressParameters cep = (ColtExpressParameters) firstState.getGameParameters();

        cegs.train = new Train(cegs.getNPlayers());
        cegs.playerCharacters = new HashMap<>();

        HashSet<CharacterType> characters = new HashSet<>();
        Collections.addAll(characters, CharacterType.values());

        cegs.playerDecks = new ArrayList<>(cegs.getNPlayers());
        for (int playerIndex = 0; playerIndex < cegs.getNPlayers(); playerIndex++) {
            cegs.playerCharacters.put(playerIndex, pickRandomCharacterType(characters));

            boolean[] visibility = new boolean[cegs.getNPlayers()];
            Arrays.fill(visibility, !PARTIAL_OBSERVABLE);
            visibility[playerIndex] = true;

            PartialObservableDeck<ColtExpressCard> playerCards =
                    new PartialObservableDeck<>("playerCards" + playerIndex, visibility);
            for (ColtExpressCard.CardType type : cep.cardCounts.keySet()){
                for (int j = 0; j < cep.cardCounts.get(type); j++) {
                    playerCards.add(new ColtExpressCard(playerIndex, type));
                }
            }
            cegs.playerDecks.add(playerCards);
        }
    }

    @Override
    protected AbstractForwardModel getCopy() {
        return null;
    }

    @Override
    public void next(AbstractGameState gameState, AbstractAction action) {
        ColtExpressGameState cegs = (ColtExpressGameState) gameState;
        ColtExpressTurnOrder ceto = (ColtExpressTurnOrder) gameState.getTurnOrder();
        if (action != null) {
            if (VERBOSE)
                System.out.println(action.toString());
            action.execute(gameState);
        } else {
            if (VERBOSE)
                System.out.println("Player cannot do anything since he has drawn cards or " +
                    " doesn't have any targets available");
        }

        IGamePhase gamePhase = cegs.getGamePhase();
        if (ColtExpressGameState.ColtExpressGamePhase.DraftCharacter.equals(gamePhase)) {
            System.out.println("character drafting is not implemented yet");
            throw new UnsupportedOperationException("not implemented yet");
        } else if (ColtExpressGameState.ColtExpressGamePhase.PlanActions.equals(gamePhase)) {
            ceto.endPlayerTurn(gameState);
        } else if (ColtExpressGameState.ColtExpressGamePhase.ExecuteActions.equals(gamePhase)) {
            ceto.endPlayerTurn(gameState);
            if (cegs.plannedActions.getSize() == 0)
                ceto.endRoundCard(gameState);
        }
    }

    public CharacterType pickRandomCharacterType(HashSet<CharacterType> characters){
        int size = characters.size();
        int item = rnd.nextInt(size);
        int i = 0;
        for(CharacterType obj : characters) {
            if (i == item){
                characters.remove(obj);
                return obj;
            }
            i++;
        }
        return null;
    }

    public void pickCharacterType(CharacterType characterType, HashSet<CharacterType> characters){
        characters.remove(characterType);
    }

    @Override
    protected void endGame(AbstractGameState gameState) {
        ColtExpressGameState cegs = (ColtExpressGameState) gameState;
        gameState.setGameStatus(Utils.GameResult.GAME_END);
        Arrays.fill(gameState.getPlayerResults(), Utils.GameResult.GAME_LOSE);

        int[] pointsPerPlayer = new int[gameState.getNPlayers()];
        int[] bulletCardsPerPlayer = new int[gameState.getNPlayers()];

        List<Integer> playersWithMostSuccessfulShots = new LinkedList<>();
        int bestValue = 6;
        for (int i = 0; i < gameState.getNPlayers(); i++) {
            for (Loot loot : cegs.playerLoot.get(i).getComponents())
                pointsPerPlayer[i] += loot.getValue();
            for (ColtExpressCard card : cegs.playerDecks.get(i).getComponents())
                if (card.cardType == ColtExpressCard.CardType.Bullet)
                    bulletCardsPerPlayer[i]++;
            for (ColtExpressCard card : cegs.playerHandCards.get(i).getComponents())
                if (card.cardType == ColtExpressCard.CardType.Bullet)
                    bulletCardsPerPlayer[i]++;

            if (cegs.bulletsLeft[i] < bestValue){
                bestValue = cegs.bulletsLeft[i];
                playersWithMostSuccessfulShots.clear();
                playersWithMostSuccessfulShots.add(i);
            } else if (cegs.bulletsLeft[i] == bestValue) {
                playersWithMostSuccessfulShots.add(i);
            }
        }

        for (Integer bestShooter : playersWithMostSuccessfulShots)
            pointsPerPlayer[bestShooter] += 1000;

        LinkedList<Integer> potentialWinnersByPoints = new LinkedList<>();
        bestValue = 0;
        for (int i = 0; i < gameState.getNPlayers(); i++) {
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
                gameState.setPlayerResult(Utils.GameResult.GAME_WIN, playerID);
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
            gameState.setPlayerResult(Utils.GameResult.GAME_WIN, playerID);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        ArrayList<AbstractAction> actions;
        if (ColtExpressGameState.ColtExpressGamePhase.DraftCharacter.equals(gameState.getGamePhase())) {
            System.out.println("character drafting is not implemented yet");
            actions = drawAction();
        } else if (ColtExpressGameState.ColtExpressGamePhase.DrawCards.equals(gameState.getGamePhase())) {
            actions = drawAction();
        } else if (ColtExpressGameState.ColtExpressGamePhase.PlanActions.equals(gameState.getGamePhase())) {
            actions = schemingActions(gameState);
        } else if (ColtExpressGameState.ColtExpressGamePhase.ExecuteActions.equals(gameState.getGamePhase())) {
            actions = stealingActions(gameState);
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
}
