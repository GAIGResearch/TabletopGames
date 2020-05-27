package games.coltexpress;

import core.AbstractGameState;
import core.AbstractForwardModel;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import core.interfaces.IGamePhase;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Loot;
import games.coltexpress.ColtExpressTypes.*;
import utilities.Group;
import static core.CoreConstants.VERBOSE;

import java.util.*;

import static core.CoreConstants.PARTIAL_OBSERVABLE;

public class ColtExpressForwardModel extends AbstractForwardModel {

    @Override
    public void setup(AbstractGameState firstState) {
        ColtExpressGameState cegs = (ColtExpressGameState) firstState;
        ColtExpressParameters cep = (ColtExpressParameters) firstState.getGameParameters();

        cegs.setupTrain();
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
            CharacterType characterType = pickRandomCharacterType(characters);
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
            for (Group<ColtExpressParameters.LootType, Integer, Integer> e: cep.playerStartLoot) {
                ColtExpressParameters.LootType lootType = e.a;
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
            if (cegs.plannedActions.getSize() == 0) {
                ceto.endRoundCard(gameState);
                distributeCards((ColtExpressGameState) gameState);
            }
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

    public void distributeCards(ColtExpressGameState cegs){
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

}
