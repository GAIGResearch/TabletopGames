package games.virus;

import core.AbstractForwardModel;
import core.AbstractGameParameters;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IObservation;
import games.virus.actions.*;
import games.virus.cards.*;
import games.virus.components.VirusBody;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;

public class VirusGameState extends AbstractGameState {
    List<VirusBody>       playerBodies;   // Each player has a body
    List<Deck<VirusCard>> playerDecks;    // Each player has a deck withh 3 cards
    Deck<VirusCard>       drawDeck;       // The deck with the not yet played cards, It is not visible for any player
    Deck<VirusCard>       discardDeck;    // The deck with already played cards. It is visible for all players

    @Override
    public void addAllComponents() {
        allComponents.putComponents(playerBodies);
        allComponents.putComponents(playerDecks);
        allComponents.putComponent(drawDeck);
        allComponents.putComponent(discardDeck);
    }

    public VirusGameState(AbstractGameParameters gameParameters, AbstractForwardModel model, int nPlayers) {
        super(gameParameters, model, new VirusTurnOrder(nPlayers));
    }

    @Override
    public IObservation getObservation(int playerID) {
        Deck<VirusCard> playerHand = playerDecks.get(playerID);
        return new VirusObservation(playerBodies, playerHand);
    }

    @Override
    public List<AbstractAction> computeAvailableActions() {
        ArrayList<AbstractAction> actions    = new ArrayList<>();
        VirusGameParameters vgp = (VirusGameParameters) gameParameters;
        int                player     = getCurrentPlayer();
        Deck<VirusCard>    playerHand = playerDecks.get(player);
        List<VirusCard>    cards      = playerHand.getComponents();

        for (int i = 0; i < vgp.maxCardsDiscard; i++) {
            // Playable cards actions:
            addActionsForCard(playerHand.peek(i), actions, playerHand);

            // Create DiscardCard actions. The player can always discard 1, 2 or 3 cards
            actions.add(new ReplaceCards(playerHand.getComponentID(), discardDeck.getComponentID(), i, drawDeck.getComponentID()));
        }

        return actions;
    }

    // Organ:    Add if it does not exit yet in the player's body
    // Medicine: Apply only in own body if organ exists and it is not immunised yet.
    // Virus:    It can be applied in other players' organ, if exist and they are not immunised yet.
    private void addActionsForCard(VirusCard card, ArrayList<AbstractAction> actions, Deck<VirusCard> playerHand) {
        int playerID = getCurrentPlayer();
        int cardIdx = playerHand.getComponents().indexOf(card);
        switch (card.type) {
            case Organ: {
                VirusBody myBody = playerBodies.get(playerID);
                if (!myBody.hasOrgan(card.organ))
                    actions.add(new AddOrgan(playerHand.getComponentID(), discardDeck.getComponentID(), cardIdx, myBody.getComponentID()));
                break;
            }
            case Medicine: {
                VirusBody myBody = playerBodies.get(playerID);
                if (myBody.hasOrgan(card.organ) && !myBody.hasOrganImmunised(card.organ))
                    actions.add(new ApplyMedicine(playerHand.getComponentID(), discardDeck.getComponentID(), cardIdx, myBody.getComponentID()));
                break;
            }
            case Virus:
                for (int i = 0; i < getNPlayers(); i++) {
                    if (i != playerID) {
                        VirusBody itsBody = playerBodies.get(i);
                        if (itsBody.hasOrgan(card.organ) && !itsBody.hasOrganImmunised(card.organ))
                            actions.add(new ApplyVirus(playerHand.getComponentID(), discardDeck.getComponentID(), cardIdx, itsBody.getComponentID()));
                    }
                }
                break;
        }
    }

    public Deck<VirusCard> getDiscardDeck() {
        return discardDeck;
    }

    public Deck<VirusCard> getDrawDeck() {
        return drawDeck;
    }

    public List<Deck<VirusCard>> getPlayerDecks() {
        return playerDecks;
    }

    public List<VirusBody> getPlayerBodies() {
        return playerBodies;
    }

    @Override
    public void endGame() {
        System.out.println("Game Results:");
        for (int playerID = 0; playerID < getNPlayers(); playerID++) {
            if (playerResults[playerID] == Utils.GameResult.GAME_WIN) {
                System.out.println("The winner is the player : " + playerID);
                break;
            }
        }
    }
}
