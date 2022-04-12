package games.coltexpress;

import core.AbstractGameState;
import core.AbstractForwardModel;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.actions.DrawCard;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IGamePhase;
import games.coltexpress.ColtExpressTypes.CharacterType;
import games.coltexpress.ColtExpressTypes.LootType;
import games.coltexpress.actions.*;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;
import utilities.Group;
import utilities.Utils;

import java.util.*;

import static core.CoreConstants.VisibilityMode;
import static games.coltexpress.ColtExpressGameState.ColtExpressGamePhase.PlanActions;
import static utilities.Utils.GameResult.LOSE;
import static utilities.Utils.GameResult.WIN;

public class ColtExpressForwardModel extends AbstractForwardModel {

    @Override
    public void _setup(AbstractGameState firstState) {
        Random rnd = new Random(firstState.getGameParameters().getRandomSeed());
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
        cegs.plannedActions = new PartialObservableDeck<>("plannedActions", -1, cegs.getNPlayers());

        Arrays.fill(cegs.bulletsLeft, cep.nBulletsPerPlayer);

        for (int playerIndex = 0; playerIndex < cegs.getNPlayers(); playerIndex++) {
            CharacterType characterType = pickRandomCharacterType(rnd, characters);
            cegs.playerCharacters.put(playerIndex, characterType);
            if (characterType == CharacterType.Belle)
                cegs.playerPlayingBelle = playerIndex;

            Deck<ColtExpressCard> playerCards = new Deck<>("playerCards" + playerIndex, playerIndex, VisibilityMode.HIDDEN_TO_ALL);
            for (ColtExpressCard.CardType type : cep.cardCounts.keySet()){
                for (int j = 0; j < cep.cardCounts.get(type); j++) {
                    playerCards.add(new ColtExpressCard(playerIndex, type));
                }
            }
            cegs.playerDecks.add(playerCards);
            playerCards.shuffle(new Random(cep.getRandomSeed()+playerIndex));

            Deck<ColtExpressCard> playerHand = new Deck<>("playerHand" + playerIndex, playerIndex, VisibilityMode.VISIBLE_TO_OWNER);

            cegs.playerHandCards.add(playerHand);

            Deck<Loot> loot = new Deck<>("playerLoot" + playerIndex, playerIndex, VisibilityMode.HIDDEN_TO_ALL);
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
        cegs.rounds = new PartialObservableDeck<>("Rounds", -1, cegs.getNPlayers());

        // Add 1 random end round card
        // A deck works on a First In Last Out basis - so we deal the last card to be drawn first (it goes to the bottom of the deck

        cegs.rounds.add(cegs.getRandomEndRoundCard(cep));

        // Add random round cards
        ArrayList<ColtExpressTypes.RegularRoundCard> availableRounds = new ArrayList<>(Arrays.asList(cep.roundCards));
        for (int i = 0; i < cep.nMaxRounds-1; i++) {
            Random r = new Random(cep.getRandomSeed() + cegs.getTurnOrder().getRoundCounter() + i);
            int choice = r.nextInt(availableRounds.size());
            cegs.rounds.add(cegs.getRoundCard(availableRounds.get(choice), cegs.getNPlayers()));
            availableRounds.remove(availableRounds.get(choice));
        }
        // set first card to be visible
        boolean[] allTrue = new boolean[cegs.getNPlayers()];
        Arrays.fill(allTrue, true);
        cegs.rounds.setVisibilityOfComponent(0, allTrue);
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
            action.execute(gameState);
        } else {
            if (gameState.getCoreGameParameters().verbose)
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

    @Override
    protected void illegalActionPlayed(AbstractGameState gameState, AbstractAction action) {
        _next(gameState, action);
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

    private void distributeCards(ColtExpressGameState cegs){
        for (int playerIndex = 0; playerIndex < cegs.getNPlayers(); playerIndex++) {
            Deck<ColtExpressCard> playerHand = cegs.playerHandCards.get(playerIndex);
            Deck<ColtExpressCard> playerDeck = cegs.playerDecks.get(playerIndex);

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

        Arrays.fill(cegs.getPlayerResults(), LOSE);

        for (int p = 0; p < cegs.getNPlayers(); p++) {
            cegs.setPlayerResult(cegs.getOrdinalPosition(p) == 1 ? WIN : LOSE, p);
        }

        if (gameState.getCoreGameParameters().verbose) {
            System.out.println(Arrays.toString(cegs.getPlayerResults()));
        }
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
        ArrayList<AbstractAction> actions = new ArrayList<>();

        ColtExpressParameters cep = (ColtExpressParameters)cegs.getGameParameters();
        ColtExpressTurnOrder ceto = (ColtExpressTurnOrder) cegs.getTurnOrder();
        int player = cegs.getCurrentPlayer();

        HashSet<ColtExpressCard.CardType> types = new HashSet<>();

        Deck<ColtExpressCard> playerHand = cegs.playerHandCards.get(player);
        int fromID = playerHand.getComponentID();
        int toID = cegs.plannedActions.getComponentID();

        // Add 1 action for each card type in player's hand
        for (int i = 0; i < playerHand.getSize(); i++){
            ColtExpressCard c = playerHand.get(i);
            if (c.cardType == ColtExpressCard.CardType.Bullet || types.contains(c.cardType))
                continue;

            // Ghost can play a card hidden during the first turn of a round, otherwise hidden if turn is hidden
            boolean hidden = ceto.isHiddenTurn() ||
                    (cegs.playerCharacters.get(player) == CharacterType.Ghost && ceto.getFullPlayerTurnCounter() == 0);

            // Add action
            actions.add(new SchemeAction(fromID, toID, i, hidden));
            types.add(c.cardType);
        }

        // Can draw cards if enough left in deck
        int nDraw = Math.min(cegs.playerDecks.get(player).getSize(), cep.nCardsDraw);
        if (nDraw > 0) {
            actions.add(new DrawComponents<ColtExpressCard>(cegs.playerDecks.get(player).getComponentID(), fromID, nDraw));
        }
        return actions;
    }

    private ArrayList<AbstractAction> stealingActions(ColtExpressGameState cegs)
    {
        int player = cegs.getCurrentPlayer();
        ArrayList<AbstractAction> actions = new ArrayList<>();
        if (cegs.plannedActions.getSize() == 0) {
            actions.add(new DoNothing());
            return actions;
        }

        int cardIdx = cegs.plannedActions.getSize()-1;
        int deckFromID = cegs.plannedActions.getComponentID();
        int deckToID = cegs.playerDecks.get(player).getComponentID();

        ColtExpressCard plannedActionCard = cegs.plannedActions.peek(cardIdx);
        if (plannedActionCard.playerID == -1 || plannedActionCard.cardType == ColtExpressCard.CardType.Bullet) {
            if (cegs.getCoreGameParameters().verbose) {
                System.out.println("Player on planned action card is -1: " + plannedActionCard);
            }
            new DrawCard(deckFromID, deckToID, cardIdx).execute(cegs);
            actions.add(new DoNothing());
            return actions;
        }

        if (player == plannedActionCard.playerID)
        {
            switch (plannedActionCard.cardType){
                case Punch:
                    createPunchingActions(cegs, actions, player, cardIdx);
                    break;
                case Shoot:
                    createShootingActions(cegs, actions, player, cardIdx);
                    break;
                case MoveVertical:
                    for (Compartment compartment : cegs.trainCompartments) {
                        if (compartment.playersInsideCompartment.contains(player) ||
                                compartment.playersOnTopOfCompartment.contains(player)) {
                            boolean toRoof = compartment.playersInsideCompartment.contains(player);
                            actions.add(new MoveVerticalAction(deckFromID, deckToID, cardIdx, compartment.getComponentID(), toRoof));
                            break;
                        }
                    }
                    break;
                case MoveMarshal:
                    for (int i = 0; i < cegs.trainCompartments.size(); i++){
                        Compartment compartment = cegs.trainCompartments.get(i);
                        if (compartment.containsMarshal){
                            if (i > 0)
                                // Move marshal left
                                actions.add(new MoveMarshalAction(deckFromID, deckToID, cardIdx, compartment.getComponentID(),
                                        cegs.trainCompartments.get(i-1).getComponentID()));
                            if (i < cegs.trainCompartments.size() - 1)
                                // Move marshal right
                                actions.add(new MoveMarshalAction(deckFromID, deckToID, cardIdx, compartment.getComponentID(),
                                        cegs.trainCompartments.get(i+1).getComponentID()));
                            break;
                        }
                    }
                    break;
                case CollectMoney:
                    for (Compartment compartment : cegs.trainCompartments) {
                        Deck<Loot> availableLoot = null;
                        if (compartment.playersOnTopOfCompartment.contains(player))
                            availableLoot = compartment.lootOnTop;
                        else if (compartment.playersInsideCompartment.contains(player))
                            availableLoot = compartment.lootInside;
                        if (availableLoot != null && availableLoot.getSize() > 0) {
                            HashSet<LootType> lootTypes = new HashSet<>();
                            for (Loot loot : availableLoot.getComponents()) {
                                lootTypes.add(loot.getLootType());
                            }
                            for (LootType lt: lootTypes) {
                                actions.add(new CollectMoneyAction(deckFromID, deckToID, cardIdx, lt,
                                        availableLoot.getComponentID()));
                            }
                            break;
                        }
                    }
                    if (actions.size() == 0) {
                        actions.add(new CollectMoneyAction(deckFromID, deckToID, cardIdx,null, -1));
                    }
                    break;
                case MoveSideways:
                    for (int i = 0; i < cegs.trainCompartments.size(); i++){
                        Compartment compartment = cegs.trainCompartments.get(i);
                        if (compartment.playersOnTopOfCompartment.contains(player)){
                            // Rules for movement on top
                            for (int offset = 1; offset < ((ColtExpressParameters)cegs.getGameParameters()).nRoofMove; offset++){
                                if ((i-offset) >= 0) {
                                    // Move left
                                    actions.add(new MoveSidewaysAction(deckFromID, deckToID, cardIdx, compartment.getComponentID(),
                                            cegs.trainCompartments.get(i-offset).getComponentID()));
                                }
                                if ((i+offset) <= cegs.trainCompartments.size()-1) {
                                    // Move right
                                    actions.add(new MoveSidewaysAction(deckFromID, deckToID, cardIdx, compartment.getComponentID(),
                                            cegs.trainCompartments.get(i+offset).getComponentID()));
                                }
                            }
                            break;
                        } else if (compartment.playersInsideCompartment.contains(player)){
                            // Inside can only move to adjacent compartment
                            if ((i-1) >= 0) {
                                // Move left
                                actions.add(new MoveSidewaysAction(deckFromID, deckToID, cardIdx, compartment.getComponentID(),
                                        cegs.trainCompartments.get(i-1).getComponentID()));
                            }
                            if ((i+1) <= cegs.trainCompartments.size()-1) {
                                // Move right
                                actions.add(new MoveSidewaysAction(deckFromID, deckToID, cardIdx, compartment.getComponentID(),
                                        cegs.trainCompartments.get(i+1).getComponentID()));
                            }
                            break;
                        }
                    }
                    break;
                default:
                    throw new IllegalArgumentException("cardType " + plannedActionCard.cardType + "" +
                            " unknown to ColtExpressGameState");
            }

        } else {
            actions.add(new DoNothing());
        }
        return actions;
    }

    private void createPunchingActions(ColtExpressGameState cegs, ArrayList<AbstractAction> actions, int player, int cardIdx){
        int deckFromID = cegs.plannedActions.getComponentID();
        int deckToID = cegs.playerDecks.get(player).getComponentID();
        boolean playerIsCheyenne = cegs.playerCharacters.get(player) == CharacterType.Cheyenne;

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

        // punch forward or backward
        if (playerCompartment != null) {
            int sourceCompID = playerCompartment.getComponentID();

            // Belle can't be a target if others are available
            if (availableTargets.size() > 1)
                availableTargets.remove(cegs.playerPlayingBelle);

            // Create punch actions
            for (int offset = -1; offset <= 1; offset++) {
                if (offset == 0 || playerCompartmentIndex + offset < 0 || playerCompartmentIndex + offset >= cegs.trainCompartments.size())
                    continue;
                Compartment targetCompartment = cegs.trainCompartments.get(playerCompartmentIndex + offset);

                // For each available target
                for (Integer targetPlayer : availableTargets) {
                    Deck<Loot> availableLoot = cegs.playerLoot.get(targetPlayer);

                    if (availableLoot.getSize() > 0) {
                        // Punch and make them drop random loot of type
                        HashSet<LootType> lootTypes = new HashSet<>();
                        for (Loot loot : availableLoot.getComponents()) {
                            lootTypes.add(loot.getLootType());
                        }
                        for (LootType lt: lootTypes) {
                            actions.add(new PunchAction(deckFromID, deckToID, cardIdx, targetPlayer,
                                    sourceCompID, targetCompartment.getComponentID(),
                                    lt, availableLoot.getComponentID(), playerIsCheyenne));
                        }
                    } else {
                        // punch opponent that cannot drop anymore loot
                        actions.add(new PunchAction(deckFromID, deckToID, cardIdx, targetPlayer,
                                sourceCompID, targetCompartment.getComponentID(),
                                null, -1, playerIsCheyenne));
                    }
                }
            }
        }

        if (actions.size() == 0)
            actions.add(new PunchAction(deckFromID, deckToID, cardIdx, -1, -1, -1,
                    null, -1, playerIsCheyenne));
    }

    private void createShootingActions(ColtExpressGameState cegs, ArrayList<AbstractAction> actions, int player, int cardIdx) {
        int deckFromID = cegs.plannedActions.getComponentID();
        int deckToID = cegs.playerDecks.get(player).getComponentID();
        boolean playerIsDjango = cegs.playerCharacters.get(player) == CharacterType.Django;

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

            int sourceCompID = playerCompartment.getComponentID();
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

                // Add player above if your are tuco
                if (cegs.playerCharacters.get(player) == CharacterType.Tuco) {
                    for (Integer target : cegs.trainCompartments.get(playerCompartmentIndex).playersOnTopOfCompartment)
                        targets.put(target, playerCompartment);
                }
            }

            if (targets.size() > 1)
                targets.remove(cegs.playerPlayingBelle);

            for (Map.Entry<Integer, Compartment> entry : targets.entrySet()) {
                actions.add(new ShootPlayerAction(deckFromID, deckToID, cardIdx, sourceCompID,
                        entry.getValue().getComponentID(), entry.getKey(), playerIsDjango));
            }

            if (actions.size() == 0)
                actions.add(new ShootPlayerAction(deckFromID, deckToID, cardIdx, sourceCompID,
                        -1, -1, playerIsDjango));
        }
    }

    private void setupTrain(ColtExpressGameState cegs) {
        // Choose random compartment configurations
        Random random = new Random(cegs.getGameParameters().getRandomSeed());
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
