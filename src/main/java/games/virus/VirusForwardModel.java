package games.virus;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.virus.actions.*;
import games.virus.cards.*;
import games.virus.components.VirusBody;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static core.CoreConstants.VERBOSE;
import static games.virus.cards.VirusCard.OrganType.Wild;

public class VirusForwardModel extends AbstractForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        // 1. Each player has a body
        // 2. Each player has a deck with 3 cards
        // 3. There is a draw deck with 68 cards at the beginning.
        //    21 organs, 17 viruses, 20 medicines, 10 treatments
        // 4. There is a discard card. Empty at the beginning.

        VirusGameState vgs = (VirusGameState) firstState;

        vgs.playerBodies = new ArrayList<>(vgs.getNPlayers());

        // Create an empty body for each player
        for (int i=0; i<vgs.getNPlayers(); i++) {
            vgs.playerBodies.add(new VirusBody());
        }

        // Create the draw deck with all the cards
        vgs.drawDeck = new Deck<>("DrawDeck", -1);
        createCards(vgs);

        vgs.drawDeck.shuffle(new Random(vgs.getGameParameters().getRandomSeed()));

        // Create the discard deck, at the beginning it is empty
        vgs.discardDeck = new Deck<>("DiscardDeck", -1);

        // Draw initial cards to each player
        vgs.playerDecks = new ArrayList<>(vgs.getNPlayers());
        drawCardsToPlayers(vgs);
    }

    @Override
    protected void _next(AbstractGameState gameState, AbstractAction action) {
        action.execute(gameState);
        checkGameEnd((VirusGameState)gameState);
        if (gameState.getGameStatus() == Utils.GameResult.GAME_ONGOING)
            gameState.getTurnOrder().endPlayerTurn(gameState);
    }

    /**
     * Create all the cards and include them into the drawPile
     * @param vgs - Virus game state
     */
    private void createCards(VirusGameState vgs) {
        VirusGameParameters vgp = (VirusGameParameters) vgs.getGameParameters();

        // Organs, Virus and Medicine cards
        for (VirusCard.OrganType organ: VirusCard.OrganType.values()) {
            if (organ != Wild) {
                for (int i=0; i<vgp.nCardsPerOrgan; i++)
                    vgs.drawDeck.add(new VirusCard(organ, VirusCard.VirusCardType.Organ));

                for (int i=0; i<vgp.nCardsPerVirus; i++)
                    vgs.drawDeck.add(new VirusCard(organ, VirusCard.VirusCardType.Virus));

                for (int i=0; i<vgp.nCardsPerMedicine; i++)
                    vgs.drawDeck.add(new VirusCard(organ, VirusCard.VirusCardType.Medicine));
            }
        }

        //  Wilds cards
        for (int i=0; i<vgp.nCardsPerWildOrgan; i++)
            vgs.drawDeck.add(new VirusCard(Wild, VirusCard.VirusCardType.Organ));

        for (int i=0; i<vgp.nCardsPerWildVirus; i++)
            vgs.drawDeck.add(new VirusCard(Wild, VirusCard.VirusCardType.Virus));

        for (int i=0; i<vgp.nCardsPerWildMedicine; i++)
            vgs.drawDeck.add(new VirusCard(Wild, VirusCard.VirusCardType.Medicine));
/*
        // Treatment cards
        for (int i=0; i<vgp.nCardsPerTreatmentSpreading; i++)
            vgs.drawDeck.add(new VirusTreatmentCard(VirusTreatmentCard.TreatmentType.Spreading));

        for (int i=0; i<vgp.nCardsPerTreatmentTransplant; i++)
            vgs.drawDeck.add(new VirusTreatmentCard(VirusTreatmentCard.TreatmentType.Transplant));

        for (int i=0; i<vgp.nCardsPerTreatmentOrganThief; i++)
            vgs.drawDeck.add(new VirusTreatmentCard(VirusTreatmentCard.TreatmentType.OrganThief));

        for (int i=0; i<vgp.nCardsPerTreatmentLatexGlove; i++)
            vgs.drawDeck.add(new VirusTreatmentCard(VirusTreatmentCard.TreatmentType.LatexGlove));

        for (int i=0; i<vgp.nCardsPerTreatmentMedicalError; i++)
            vgs.drawDeck.add(new VirusTreatmentCard(VirusTreatmentCard.TreatmentType.MedicalError));
            */

    }

    /**
     * Draw cards to players
     * @param vgs - Virus game state
     */
    private void drawCardsToPlayers(VirusGameState vgs) {
        for (int i = 0; i < vgs.getNPlayers(); i++) {
            String playerDeckName = "Player" + i + "Deck";
            vgs.playerDecks.add(new Deck<>(playerDeckName, i));
            for (int j = 0; j < 3; j++) {
                vgs.playerDecks.get(i).add(vgs.drawDeck.draw());
            }
        }
    }

    /**
     * Check if the game is end. A player wins when have, at least, 4 organs not infected
     * @param vgs - Virus game state
     */
    public void checkGameEnd(VirusGameState vgs) {
        for (int i = 0; i < vgs.getNPlayers(); i++) {
            if (vgs.playerBodies.get(i).getNOrganHealthy()>=4) {
                for (int j = 0; j < vgs.getNPlayers(); j++) {
                    if (i == j)
                        vgs.setPlayerResult(Utils.GameResult.WIN, i);
                    else
                        vgs.setPlayerResult(Utils.GameResult.LOSE, j);
                }
                vgs.setGameStatus(Utils.GameResult.GAME_END);
                break;
            }
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        VirusGameState vgs = (VirusGameState) gameState;
        ArrayList<AbstractAction> actions    = new ArrayList<>();
        VirusGameParameters vgp = (VirusGameParameters) vgs.getGameParameters();
        int                player     = vgs.getCurrentPlayer();
        Deck<VirusCard>    playerHand = vgs.playerDecks.get(player);

        // Playable cards actions
        for (int i = 0; i < playerHand.getSize(); i++)
            addActionsForCard(vgs, playerHand.peek(i), actions, playerHand);

        // Create DiscardCard actions. The player can always discard 1, 2 or 3 cards
        // Discard one card
        for (int i = 0; i < vgp.maxCardsDiscard; i++)
            actions.add(new ReplaceOneCard(playerHand.getComponentID(), vgs.discardDeck.getComponentID(), i, vgs.drawDeck.getComponentID()));

/*
        // Discard two cards: 0,1 or 0,2 or 1,2
        // TODO: it should be generalized to any player hand size
        int [] cardsToBeDiscarded01 = new int[2];
        int [] cardsToBeDiscarded02 = new int[2];
        int [] cardsToBeDiscarded12 = new int[2];


        cardsToBeDiscarded01[0] = 0;
        cardsToBeDiscarded01[1] = 1;
        actions.add(new ReplaceCards(playerHand.getComponentID(), vgs.discardDeck.getComponentID(), cardsToBeDiscarded01, vgs.drawDeck.getComponentID()));

        cardsToBeDiscarded02[0] = 0;
        cardsToBeDiscarded02[1] = 2;
        actions.add(new ReplaceCards(playerHand.getComponentID(), vgs.discardDeck.getComponentID(), cardsToBeDiscarded02, vgs.drawDeck.getComponentID()));

        cardsToBeDiscarded12[0] = 1;
        cardsToBeDiscarded12[1] = 2;
        actions.add(new ReplaceCards(playerHand.getComponentID(), vgs.discardDeck.getComponentID(), cardsToBeDiscarded12, vgs.drawDeck.getComponentID()));

        // Discard all cards
        actions.add(new ReplaceAllCards(playerHand.getComponentID(), vgs.discardDeck.getComponentID(), vgs.drawDeck.getComponentID(), playerHand.getSize()));
*/
        return actions;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new VirusForwardModel();
    }

    /**
     * Compute possible actions given a card from player hand and add to actions
     * Organ:    Add if it does not exit yet in the player's body
     * Medicine: Apply only in own body if organ exists and it is not immunised yet.
     * Virus:    It can be applied in other players' organ, if exist and they are not immunised yet.
     * @param gameState - Virus game state
     * @param card - card that can be played
     * @param actions - list of actions to be filled
     * @param playerHand - Player hand
     */
    // TODO: treatments cards
    private void addActionsForCard(VirusGameState gameState, VirusCard card, ArrayList<AbstractAction> actions, Deck<VirusCard> playerHand) {
        int playerID = gameState.getCurrentPlayer();
        int cardIdx = playerHand.getComponents().indexOf(card);
        switch (card.type) {
            case Organ: {
                VirusBody myBody = gameState.playerBodies.get(playerID);
                if (!myBody.hasOrgan(card.organ))
                    actions.add(new AddOrgan(playerHand.getComponentID(), gameState.discardDeck.getComponentID(), cardIdx, myBody.getComponentID()));
                break;
            }
            case Medicine: {
                // A medicine (not wild) card can be played if in the player's body, there is:
                // - An organ of the same type and it is has not been yet immunised
                // - An organ of any type has been infected or vaccinated with a wild card and is has not been yet immunised
                // - the wild organ and it has not been yet immunised
                // A medicine Wild can be played in any not yet immunised organ
                VirusBody myBody = gameState.playerBodies.get(playerID);
                for (VirusCard.OrganType organ: VirusCard.OrganType.values())
                {
                    if (myBody.hasOrgan(organ) && myBody.organNotYetImmunised(organ)) {  // Organ exists and it not immunised
                        if (card.organ == VirusCard.OrganType.Wild ||                  // It is a medicine wild
                                organ == VirusCard.OrganType.Wild ||                   // It is the organ wild
                                organ == card.organ ||                                 // Same organ type
                                myBody.hasOrganVaccinatedWild(organ) ||                // The organ is vaccinated Wild
                                myBody.hasOrganInfectedWild(organ))                    // The organ is infected wild
                            actions.add(new ApplyMedicine(playerHand.getComponentID(), gameState.discardDeck.getComponentID(), cardIdx, myBody.getComponentID(), organ));
                    }
                }
                break;
            }
            case Virus:
                // A virus (not wild) card can be played if in the other player's body, there is:
                // - An organ of the same type and it is has not been yet immunised
                // - An organ of any type has been infected or vaccinated with a wild card and is has not been yet immunised
                // - the wild organ and it has not been yet immunised
                // A virus Wild can be played in any not yet immunised organ

                for (int i = 0; i < gameState.getNPlayers(); i++) {
                    if (i != playerID) {
                        VirusBody itsBody = gameState.playerBodies.get(i);
                        for (VirusCard.OrganType organ : VirusCard.OrganType.values()) {
                            if (itsBody.hasOrgan(organ) && itsBody.organNotYetImmunised(organ)) {  // Organ exists and it not immunised
                                if (card.organ == VirusCard.OrganType.Wild ||                  // It is a medicine wild
                                        organ == VirusCard.OrganType.Wild ||                   // It is the organ wild
                                        organ == card.organ ||                                 // Same organ type
                                        itsBody.hasOrganVaccinatedWild(organ) ||                // The organ is vaccinated Wild
                                        itsBody.hasOrganInfectedWild(organ))                    // The organ is infected wild
                                    actions.add(new ApplyVirus(playerHand.getComponentID(), gameState.discardDeck.getComponentID(), cardIdx, itsBody.getComponentID(), organ, i));
                            }
                        }
                    }
                }
                break;
        }
    }

    @Override
    protected void endGame(AbstractGameState gameState) {
        if (VERBOSE) {
            System.out.println("Game Results:");
            for (int playerID = 0; playerID < gameState.getNPlayers(); playerID++) {
                if (gameState.getPlayerResults()[playerID] == Utils.GameResult.WIN) {
                    System.out.println("The winner is the player : " + playerID);
                    break;
                }
            }
        }
    }
}
