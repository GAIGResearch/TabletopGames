package games.coltexpress;

import core.AbstractGameState;
import core.AbstractForwardModel;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.actions.DrawComponents;
import core.components.PartialObservableDeck;
import core.interfaces.IGamePhase;
import games.coltexpress.actions.*;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Loot;
import games.coltexpress.ColtExpressTypes.*;
import utilities.Group;
import games.coltexpress.components.Compartment;
import utilities.Utils;

import static core.CoreConstants.VERBOSE;

import java.util.*;

import static core.CoreConstants.PARTIAL_OBSERVABLE;
import static games.coltexpress.ColtExpressGameState.ColtExpressGamePhase.PlanActions;

public class ColtExpressForwardModel extends AbstractForwardModel {

    @Override
    public void _setup(AbstractGameState firstState) {
        Random rnd = new Random(firstState.getGameParameters().getGameSeed());
        ColtExpressGameState cegs = (ColtExpressGameState) firstState;
        ColtExpressParameters cep = (ColtExpressParameters) firstState.getGameParameters();

        setupRounds(cegs, cep);
        setupTrain(cegs);
        cegs.playerCharacters = new HashMap<>();

        HashSet<CharacterType> characters = new HashSet<>();
        Collections.addAll(characters, CharacterType.values());

        cegs.playerDecks = new ArrayList<>(cegs.getNPlayers());
        cegs.playerHandCards = new ArrayList<>(cegs.getNPlayers());
        cegs.playerLoot = new ArrayList<>(cegs.getNPlayers());
        cegs.bulletsLeft = new int[cegs.getNPlayers()];
        cegs.plannedActions = new PartialObservableDeck<>("plannedActions", cegs.getNPlayers());

        Arrays.fill(cegs.bulletsLeft, cep.nBulletsPerPlayer);

        for (int playerIndex = 0; playerIndex < cegs.getNPlayers(); playerIndex++) {
            CharacterType characterType = pickRandomCharacterType(rnd, characters);
            cegs.playerCharacters.put(playerIndex, characterType);
            if (characterType == CharacterType.Belle)
                cegs.playerPlayingBelle = playerIndex;

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
            playerCards.shuffle(new Random(cep.getGameSeed()));

            PartialObservableDeck<ColtExpressCard> playerHand = new PartialObservableDeck<>(
                    "playerHand" + playerIndex, visibility);

            cegs.playerHandCards.add(playerHand);

            PartialObservableDeck<Loot> loot = new PartialObservableDeck<>("playerLoot" + playerIndex, visibility);
            for (Group<LootType, Integer, Integer> e: cep.playerStartLoot) {
                LootType lootType = e.a;
                int value = e.b;
                int nLoot = e.c;
                for (int i = 0; i < nLoot; i++) {
                    loot.add(new Loot(lootType, value));
                }
            }
            cegs.playerLoot.add(loot);

            if (playerIndex % 2 == 0)
                cegs.getTrainCompartments().get(0).addPlayerInside(playerIndex);
            else
                cegs.getTrainCompartments().get(1).addPlayerInside(playerIndex);
        }
        distributeCards(cegs);

        firstState.setGamePhase(PlanActions);
    }

    private void setupRounds(ColtExpressGameState cegs, ColtExpressParameters cep){
        cegs.rounds = new ArrayList<>(cep.nMaxRounds);

        // Add random round cards
        ArrayList<Integer> availableRounds = new ArrayList<>();
        for (int i = 0; i < cep.roundCards.length; i++) {
            availableRounds.add(i);
        }
        Random r = new Random(cep.getGameSeed());
        for (int i = 0; i < cep.nMaxRounds-1; i++) {
            int choice = r.nextInt(availableRounds.size());
            cegs.rounds.add(cegs.getRoundCard(cep, choice, cegs.getNPlayers()));
            availableRounds.remove(Integer.valueOf(choice));
        }

        // Add 1 random end round card
        cegs.rounds.add(cegs.getRandomEndRoundCard(cep));
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new ColtExpressForwardModel();
    }

    @Override
    protected void _next(AbstractGameState gameState, AbstractAction action) {
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
        } else if (PlanActions.equals(gamePhase)) {
            ceto.endPlayerTurn(gameState);
        } else if (ColtExpressGameState.ColtExpressGamePhase.ExecuteActions.equals(gamePhase)) {
            ceto.endPlayerTurn(gameState);
            if (cegs.plannedActions.getSize() == 0) {
                ceto.endRoundCard((ColtExpressGameState) gameState);
                distributeCards((ColtExpressGameState) gameState);
            }
        }
    }

    private CharacterType pickRandomCharacterType(Random rnd, HashSet<CharacterType> characters){
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

    private void pickCharacterType(CharacterType characterType, HashSet<CharacterType> characters){
        characters.remove(characterType);
    }

    private void distributeCards(ColtExpressGameState cegs){
        for (int playerIndex = 0; playerIndex < cegs.getNPlayers(); playerIndex++) {
            PartialObservableDeck<ColtExpressCard> playerHand = cegs.playerHandCards.get(playerIndex);
            PartialObservableDeck<ColtExpressCard> playerDeck = cegs.playerDecks.get(playerIndex);

            playerDeck.add(playerHand);
            playerHand.clear();

            for (int i = 0; i < ((ColtExpressParameters)cegs.getGameParameters()).nCardsInHand; i++) {
                playerHand.add(playerDeck.draw());
            }
            if (cegs.playerCharacters.get(playerIndex) == CharacterType.Doc) {
                for (int i = 0; i < ((ColtExpressParameters) cegs.getGameParameters()).nCardsInHandExtraDoc; i++) {
                    playerHand.add(playerDeck.draw());
                }
            }

        }
    }

    @Override
    protected void endGame(AbstractGameState gameState) {
        ColtExpressGameState cegs = (ColtExpressGameState) gameState;
        ColtExpressParameters cep = (ColtExpressParameters) gameState.getGameParameters();

        cegs.setGameStatus(Utils.GameResult.GAME_END);
        Arrays.fill(cegs.getPlayerResults(), Utils.GameResult.LOSE);

        int[] pointsPerPlayer = new int[cegs.getNPlayers()];
        int[] bulletCardsPerPlayer = new int[cegs.getNPlayers()];

        List<Integer> playersWithMostSuccessfulShots = new LinkedList<>();
        int bestValue = cep.nBulletsPerPlayer;
        for (int i = 0; i < cegs.getNPlayers(); i++) {
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
            pointsPerPlayer[bestShooter] += cep.shooterReward;

        LinkedList<Integer> potentialWinnersByPoints = new LinkedList<>();
        bestValue = 0;
        for (int i = 0; i < cegs.getNPlayers(); i++) {
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
                cegs.setPlayerResult(Utils.GameResult.WIN, playerID);
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
            cegs.setPlayerResult(Utils.GameResult.WIN, playerID);

        System.out.println(Arrays.toString(cegs.getPlayerResults()));
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        ColtExpressGameState cegs = (ColtExpressGameState) gameState;

        ArrayList<AbstractAction> actions = new ArrayList<>();
        if (ColtExpressGameState.ColtExpressGamePhase.DraftCharacter.equals(gameState.getGamePhase())) {
            System.out.println("character drafting is not implemented yet");
//            actions = drawAction();
        } else if (PlanActions.equals(gameState.getGamePhase())) {
            actions = schemingActions(cegs);
        } else if (ColtExpressGameState.ColtExpressGamePhase.ExecuteActions.equals(gameState.getGamePhase())) {
            actions = stealingActions(cegs);
        }

        return actions;
    }

    private ArrayList<AbstractAction> schemingActions(ColtExpressGameState cegs){
        ColtExpressParameters cep = (ColtExpressParameters)cegs.getGameParameters();
        int player = cegs.getTurnOrder().getCurrentPlayer(cegs);
        ArrayList<AbstractAction> actions = new ArrayList<>();
        for (ColtExpressCard c : cegs.playerHandCards.get(player).getComponents()){
            if (c.cardType == ColtExpressCard.CardType.Bullet)
                continue;

            // ghost can play a card hidden during the first turn
            boolean hidden = ((ColtExpressTurnOrder) cegs.getTurnOrder()).isHiddenTurn() ||
                    (cegs.playerCharacters.get(player) == CharacterType.Ghost &&
                            ((ColtExpressTurnOrder) cegs.getTurnOrder()).getCurrentRoundCardIndex() == 0);

            actions.add(new SchemeAction(cegs.playerHandCards.get(player).getComponentID(),
                    cegs.plannedActions.getComponentID(), hidden));
        }
        actions.add(new DrawComponents<ColtExpressCard>(cegs.playerHandCards.get(player).getComponentID(), cegs.playerDecks.get(player).getComponentID(), cep.nCardsDraw));
        return actions;
    }

    private ArrayList<AbstractAction> stealingActions(ColtExpressGameState cegs)
    {
        int player = cegs.getTurnOrder().getCurrentPlayer(cegs);
        ArrayList<AbstractAction> actions = new ArrayList<>();
        if (cegs.plannedActions.getSize() == 0) {
            actions.add(new DoNothing());
            return actions;
        }

        ColtExpressCard plannedActionCard = cegs.plannedActions.peek(0);
        if (player == plannedActionCard.playerID)
        {
            switch (plannedActionCard.cardType){
                case Punch:
                    createPunchingActions(cegs, plannedActionCard, actions, player);
                    break;
                case Shoot:
                    if (cegs.bulletsLeft[player] <= 0)
                        break;
                    else
                        createShootingActions(cegs, plannedActionCard, actions, player);
                    break;
                case MoveUp:
                    for (Compartment compartment : cegs.trainCompartments) {
                        if (compartment.playersInsideCompartment.contains(player) ||
                                compartment.playersOnTopOfCompartment.contains(player)) {
                            boolean toRoof = compartment.playersInsideCompartment.contains(player);
                            actions.add(new MoveVerticalAction(cegs.plannedActions.getComponentID(),
                                    cegs.playerDecks.get(player).getComponentID(), compartment.getComponentID(), toRoof));
                            break;
                        }
                    }
                    break;
                case MoveMarshal:
                    for (int i = 0; i < cegs.trainCompartments.size(); i++){
                        Compartment compartment = cegs.trainCompartments.get(i);
                        if (compartment.containsMarshal){
                            if (i > 1)
                                actions.add(new MoveMarshalAction(cegs.plannedActions.getComponentID(),
                                        cegs.playerDecks.get(player).getComponentID(), compartment.getComponentID(),
                                        cegs.trainCompartments.get(i-1).getComponentID()));
                            if (i < cegs.trainCompartments.size() - 1)
                                actions.add(new MoveMarshalAction(cegs.plannedActions.getComponentID(),
                                        cegs.playerDecks.get(player).getComponentID(), compartment.getComponentID(),
                                        cegs.trainCompartments.get(i+1).getComponentID()));

                            break;
                        }
                    }
                    break;
                case CollectMoney:
                    PartialObservableDeck<Loot> availableLoot = null;
                    for (Compartment compartment : cegs.trainCompartments) {
                        if (compartment.playersOnTopOfCompartment.contains(player))
                            availableLoot = compartment.lootOnTop;
                        else if (compartment.playersInsideCompartment.contains(player))
                            availableLoot = compartment.lootInside;
                        if (availableLoot != null) {
                            for (Loot loot : availableLoot.getComponents()) {
                                actions.add(new CollectMoneyAction(cegs.plannedActions.getComponentID(),
                                        cegs.playerDecks.get(player).getComponentID(), loot.getComponentID(), availableLoot.getComponentID()));
                            }
                        }
                    }
                    if (actions.size() == 0) {
                        actions.add(new CollectMoneyAction(cegs.plannedActions.getComponentID(),
                                cegs.playerDecks.get(player).getComponentID(), -1, -1));
                    }
                    break;
                case MoveSideways:
                    for (int i = 0; i < cegs.trainCompartments.size(); i++){
                        Compartment compartment = cegs.trainCompartments.get(i);
                        if (compartment.playersOnTopOfCompartment.contains(player)){
                            for (int offset = 1; offset < ((ColtExpressParameters)cegs.getGameParameters()).nRoofMove; offset++){
                                if ((i-offset) > 0) {
                                    actions.add(new MoveSidewaysAction(cegs.plannedActions.getComponentID(),
                                            cegs.playerDecks.get(player).getComponentID(), compartment.getComponentID(),
                                            cegs.trainCompartments.get(i-offset).getComponentID()));
                                }
                                if ((i+offset) < cegs.trainCompartments.size()) {
                                    actions.add(new MoveSidewaysAction(cegs.plannedActions.getComponentID(),
                                            cegs.playerDecks.get(player).getComponentID(), compartment.getComponentID(),
                                            cegs.trainCompartments.get(i+offset).getComponentID()));
                                }
                            }
                            break;
                        }
                        else if (compartment.playersInsideCompartment.contains(player)){
                            if ((i-1) > 0) {
                                actions.add(new MoveSidewaysAction(cegs.plannedActions.getComponentID(),
                                        cegs.playerDecks.get(player).getComponentID(), compartment.getComponentID(),
                                        cegs.trainCompartments.get(i-1).getComponentID()));
                            }
                            if ((i+1) < cegs.trainCompartments.size()) {
                                actions.add(new MoveSidewaysAction(cegs.plannedActions.getComponentID(),
                                        cegs.playerDecks.get(player).getComponentID(), compartment.getComponentID(),
                                        cegs.trainCompartments.get(i+1).getComponentID()));
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

        } else {
            actions.add(new DoNothing());
        }
        return actions;
    }

    private void createPunchingActions(ColtExpressGameState cegs, ColtExpressCard card, ArrayList<AbstractAction> actions, int player){
        int playerCompartmentIndex = 0;
        Compartment playerCompartment = null;
        HashSet<Integer> availableTargets = new HashSet<>();

        for (int i = 0; i < cegs.trainCompartments.size(); i++)
        {
            Compartment compartment = cegs.trainCompartments.get(i);
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
            availableTargets.remove(cegs.playerPlayingBelle);
        boolean playerIsCheyenne = cegs.playerCharacters.get(player) == CharacterType.Django;

        // punch forward or backward
        for (int offset = -1; offset <= 1; offset++){
            if (offset == 0 || playerCompartmentIndex+offset < 0 || playerCompartmentIndex+offset >= cegs.trainCompartments.size())
                continue;
            Compartment targetCompartment = cegs.trainCompartments.get(playerCompartmentIndex+offset);
            for (Integer targetPlayer : availableTargets){
                PartialObservableDeck<Loot> availableLoot = cegs.playerLoot.get(targetPlayer);

                if (availableLoot.getSize() > 0){
                    for (Loot loot : availableLoot.getComponents())
                    {
                        actions.add(new PunchAction(cegs.plannedActions.getComponentID(),
                                cegs.playerDecks.get(player).getComponentID(), targetPlayer,
                                playerCompartment.getComponentID(), targetCompartment.getComponentID(),
                                loot.getComponentID(), availableLoot.getComponentID(), playerIsCheyenne));
                    }
                }
                else {
                    // punch opponent that cannot drop anymore loot
                    actions.add(new PunchAction(cegs.plannedActions.getComponentID(),
                            cegs.playerDecks.get(player).getComponentID(), targetPlayer,
                            playerCompartment.getComponentID(), targetCompartment.getComponentID(),
                            -1, -1, playerIsCheyenne));
                }
            }
        }

        if (actions.size() == 0)
            actions.add(new PunchAction(cegs.plannedActions.getComponentID(),
                    cegs.playerDecks.get(player).getComponentID(), -1, -1, -1,
                    -1, -1, playerIsCheyenne));
    }

    private void createShootingActions(ColtExpressGameState cegs, ColtExpressCard card, ArrayList<AbstractAction> actions, int player) {
        int playerCompartmentIndex = 0;
        Compartment playerCompartment = null;
        boolean playerOnTop = false;
        for (int i = 0; i < cegs.trainCompartments.size(); i++)
        {
            Compartment compartment = cegs.trainCompartments.get(i);
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
                    Compartment targetCompartment = cegs.trainCompartments.get(playerCompartmentIndex - offset);
                    if (targetCompartment.playersOnTopOfCompartment.size() > 0) {
                        for (Integer target : targetCompartment.playersOnTopOfCompartment)
                            targets.put(target, targetCompartment);
                        break;
                    }
                }

                //shots to the front of the train
                for (int offset = 1; playerCompartmentIndex + offset < cegs.trainCompartments.size(); offset++) {
                    Compartment targetCompartment = cegs.trainCompartments.get(playerCompartmentIndex + offset);
                    if (targetCompartment.playersOnTopOfCompartment.size() > 0) {
                        for (Integer target : targetCompartment.playersOnTopOfCompartment)
                            targets.put(target, targetCompartment);
                        break;
                    }
                }

                //add player below if your are tuco
                if (cegs.playerCharacters.get(player) == CharacterType.Tuco) {
                    for (Integer target : cegs.trainCompartments.get(playerCompartmentIndex).playersInsideCompartment)
                        targets.put(target, playerCompartment);
                }
            } else {
                if (playerCompartmentIndex - 1 >= 0) {
                    Compartment targetCompartment = cegs.trainCompartments.get(playerCompartmentIndex - 1);
                    if (targetCompartment.playersInsideCompartment.size() > 0) {
                        for (Integer target : targetCompartment.playersInsideCompartment)
                            targets.put(target, targetCompartment);
                    }
                }

                if (playerCompartmentIndex + 1 < cegs.trainCompartments.size()) {
                    Compartment targetCompartment = cegs.trainCompartments.get(playerCompartmentIndex + 1);
                    if (targetCompartment.playersInsideCompartment.size() > 0) {
                        for (Integer target : targetCompartment.playersInsideCompartment)
                            targets.put(target, targetCompartment);
                    }
                }

                // Add player below if your are tuco
                if (cegs.playerCharacters.get(player) == CharacterType.Tuco) {
                    for (Integer target : cegs.trainCompartments.get(playerCompartmentIndex).playersOnTopOfCompartment)
                        targets.put(target, playerCompartment);
                }
            }

            if (targets.size() > 1)
                targets.remove(cegs.playerPlayingBelle);

            boolean playerIsDjango = cegs.playerCharacters.get(player) == CharacterType.Django;
            for (Map.Entry<Integer, Compartment> entry : targets.entrySet()) {
                actions.add(new ShootPlayerAction(cegs.plannedActions.getComponentID(), cegs.playerDecks.get(player).getComponentID(),
                        playerCompartment.getComponentID(), entry.getValue().getComponentID(), entry.getKey(), playerIsDjango));
            }

            if (actions.size() == 0)
                actions.add(new ShootPlayerAction(cegs.plannedActions.getComponentID(), cegs.playerDecks.get(player).getComponentID(),
                        playerCompartment.getComponentID(),-1, -1, playerIsDjango));
        }
    }

    private void setupTrain(ColtExpressGameState cegs) {
        // Choose random compartment configurations
        Random random = new Random(cegs.getGameParameters().getGameSeed());
        ArrayList<Integer> availableCompartments = new ArrayList<>();
        for (int i = 0; i < ((ColtExpressParameters)cegs.getGameParameters()).trainCompartmentConfigurations.size() - 1; i++) {
            availableCompartments.add(i);
        }
        for (int i = 0; i < cegs.getNPlayers(); i++) {
            int which = random.nextInt(availableCompartments.size());
            cegs.trainCompartments.add(new Compartment(cegs.getNPlayers(), i, which, (ColtExpressParameters)cegs.getGameParameters()));
            availableCompartments.remove(Integer.valueOf(which));
        }

        // Add locomotive
        cegs.trainCompartments.add(Compartment.createLocomotive(cegs.getNPlayers(), (ColtExpressParameters) cegs.getGameParameters()));
    }
}
