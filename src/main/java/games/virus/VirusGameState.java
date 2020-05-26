package games.virus;

import core.AbstractGameState;
import core.ForwardModel;
import core.GameParameters;
import core.actions.IAction;
import core.components.Deck;
import core.observations.IObservation;
import games.explodingkittens.actions.PlayCard;
import games.virus.actions.*;
import games.virus.cards.*;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VirusGameState extends AbstractGameState {
    private List<VirusBody>       playerBodies;   // Each player has a body
    private List<Deck<VirusCard>> playerDecks;    // Each player has a deck withh 3 cards
    private Deck<VirusCard>       drawDeck;       // The deck with the not yet played cards, It is not visible for any player
    private Deck<VirusCard>       discardDeck;    // The deck with already played cards. It is visible for all players
    private int                   nPlayers;       // Number of players

    public VirusGameState(GameParameters gameParameters, ForwardModel model, int nPlayers) {
        super(gameParameters, model, nPlayers, new VirusTurnOrder(nPlayers));
        this.nPlayers = nPlayers;
    }

    @Override
    public IObservation getObservation(int playerID) {
        Deck<VirusCard> playerHand = playerDecks.get(playerID);
        return new VirusObservation(playerBodies, playerHand);
    }

    @Override
    public List<IAction> computeAvailableActions() {
        ArrayList<IAction> actions    = new ArrayList<>();
        int                player     = getCurrentPlayerID();
        Deck<VirusCard>    playerHand = playerDecks.get(player);
        List<VirusCard>    cards      = playerHand.getCards();

        VirusCard card1 = cards.get(0);
        VirusCard card2 = cards.get(1);
        VirusCard card3 = cards.get(2);

        // Create DiscardCard actions. The player can always discard 1, 2 or 3 cards
        actions.add(new DiscardCards(card1, playerHand, drawDeck, discardDeck));
        actions.add(new DiscardCards(card2, playerHand, drawDeck, discardDeck));
        actions.add(new DiscardCards(card3, playerHand, drawDeck, discardDeck));
        actions.add(new DiscardCards(card1, card2, playerHand, drawDeck, discardDeck));
        actions.add(new DiscardCards(card1, card3, playerHand, drawDeck, discardDeck));
        actions.add(new DiscardCards(card2, card3, playerHand, drawDeck, discardDeck));
        actions.add(new DiscardCards(card1, card2, card3, playerHand, drawDeck, discardDeck));

        // Playable cards actions:
        addActionsForCard(card1, actions, playerHand);
        addActionsForCard(card2, actions, playerHand);
        addActionsForCard(card3, actions, playerHand);

        return actions;
    }

    // Organ:    Add if it does not exit yet in the player's body
    // Medicine: Apply only in own body if organ exists and it is not immunised yet.
    // Virus:    It can be applied in other players' organ, if exist and they are not immunised yet.
    private void addActionsForCard(VirusCard card, ArrayList<IAction> actions, Deck<VirusCard> playerHand) {
        int playerID = getCurrentPlayerID();
        if (card instanceof VirusOrganCard) {
            VirusBody myBody = playerBodies.get(playerID);
            if (! myBody.hasOrgan(card.organ))
                actions.add(new AddOrgan(card, myBody, playerHand, playerID, drawDeck));
        }
        else if (card instanceof  VirusMedicineCard)
        {
            VirusBody myBody = playerBodies.get(playerID);
            if (myBody.hasOrgan(card.organ) && !myBody.hasOrganImmunised(card.organ))
                actions.add(new ApplyMedicine(card, myBody, playerHand, playerID, drawDeck, discardDeck));
        }
        else if (card instanceof  VirusVirusCard)
        {
            for (int i=0; i<nPlayers; i++) {
                if ( i != playerID) {
                    VirusBody itsBody = playerBodies.get(i);
                    if (itsBody.hasOrgan(card.organ) && !itsBody.hasOrganImmunised(card.organ))
                        actions.add(new ApplyVirus(card, itsBody, playerHand, i, drawDeck, discardDeck));
                }
            }
        }
    }

    // Set the components of the game
    // 1. Each player has a body
    // 2. Each player has a deck with 3 cards
    // 3. There is a draw deck with 68 cards at the beginning.
    //    21 organs, 17 viruses, 20 medicines, 10 treatments
    // 4. There is a discard card. Empty at the beginning.
    @Override
    public void setComponents() {
        playerBodies = new ArrayList<>(nPlayers);

        // Create an empty body for each player
        for (int i=0; i<nPlayers; i++) {
            playerBodies.add(new VirusBody());
        }

        // Create the draw deck with all the cards
        drawDeck = new Deck<>("DrawDeck");
        CreateCards();

        Random rnd = new Random(1);
        drawDeck.shuffle(rnd);

        // Create the discard deck, at the beginning it is empty
        discardDeck = new Deck<>("DiscardDeck");

        // Draw initial cards to each player
        playerDecks = new ArrayList<>(nPlayers);
        DrawCardsToPlayers();
    }

    // TODO simplified version with only organ, virus and medicine cards. No wildcards, no treatments
    // Create all the cards and include them into the drawPile
    private void CreateCards() {
        // 5 cards for each organ plus 1 for wild organ
        for (int i=0; i<5; i++)
        {
            VirusOrganCard cardHearth  = new VirusOrganCard(VirusCard.VirusCardOrgan.Hearth);
            VirusOrganCard cardStomach = new VirusOrganCard(VirusCard.VirusCardOrgan.Stomach);
            VirusOrganCard cardBrain   = new VirusOrganCard(VirusCard.VirusCardOrgan.Brain);
            VirusOrganCard cardBone    = new VirusOrganCard(VirusCard.VirusCardOrgan.Bone);

            drawDeck.add(cardHearth);
            drawDeck.add(cardStomach);
            drawDeck.add(cardBrain);
            drawDeck.add(cardBone);
        }
        /*
        VirusOrganCard cardOrganWild = new VirusOrganCard(VirusCard.VirusCardColor.Wild);
        drawDeck.add(cardOrganWild);
*/
        // 4 cards for each virus plus 1 for wild virus
        for (int i=0; i<4; i++)
        {
            VirusVirusCard cardHearth  = new VirusVirusCard(VirusCard.VirusCardOrgan.Hearth);
            VirusVirusCard cardStomach = new VirusVirusCard(VirusCard.VirusCardOrgan.Stomach);
            VirusVirusCard cardBrain   = new VirusVirusCard(VirusCard.VirusCardOrgan.Brain);
            VirusVirusCard cardBone    = new VirusVirusCard(VirusCard.VirusCardOrgan.Bone);

            drawDeck.add(cardHearth);
            drawDeck.add(cardStomach);
            drawDeck.add(cardBrain);
            drawDeck.add(cardBone);
        }
        /*
        VirusVirusCard cardVirusWild = new VirusVirusCard(VirusCard.VirusCardColor.Wild);
        drawDeck.add(cardVirusWild);
*/
        // 4 cards for each medicine plus 4 for wild medicine
        for (int i=0; i<4; i++)
        {
            VirusMedicineCard cardHearth  = new VirusMedicineCard(VirusCard.VirusCardOrgan.Hearth);
            VirusMedicineCard cardStomach = new VirusMedicineCard(VirusCard.VirusCardOrgan.Stomach);
            VirusMedicineCard cardBrain   = new VirusMedicineCard(VirusCard.VirusCardOrgan.Brain);
            VirusMedicineCard cardBone    = new VirusMedicineCard(VirusCard.VirusCardOrgan.Bone);
            //VirusMedicineCard cardMedicineWild = new VirusMedicineCard(VirusCard.VirusCardColor.Wild);

            drawDeck.add(cardHearth);
            drawDeck.add(cardStomach);
            drawDeck.add(cardBrain);
            drawDeck.add(cardBone);
            //drawDeck.add(cardMedicineWild);
        }
/*
        // 2 cards for each treatment
        for (int i=0; i<2; i++)
        {
            VirusTreatmentCard cardTreatment1 = new VirusTreatmentCard(VirusTreatmentCard.TreatmentType.Transplant);
            VirusTreatmentCard cardTreatment2 = new VirusTreatmentCard(VirusTreatmentCard.TreatmentType.OrganThief);
            VirusTreatmentCard cardTreatment3 = new VirusTreatmentCard(VirusTreatmentCard.TreatmentType.Spreading);
            VirusTreatmentCard cardTreatment4 = new VirusTreatmentCard(VirusTreatmentCard.TreatmentType.LatexGlove);
            VirusTreatmentCard cardTreatment5 = new VirusTreatmentCard(VirusTreatmentCard.TreatmentType.MedicalError);

            drawDeck.add(cardTreatment1);
            drawDeck.add(cardTreatment2);
            drawDeck.add(cardTreatment3);
            drawDeck.add(cardTreatment4);
            drawDeck.add(cardTreatment5);
        }
        */

    }

    private void DrawCardsToPlayers() {
        for (int i = 0; i < nPlayers; i++) {
            String playerDeckName = "Player" + i + "Deck";
            playerDecks.add(new Deck<>(playerDeckName));
            for (int j = 0; j < 3; j++) {
                playerDecks.get(i).add(drawDeck.draw());
            }
        }
    }

    // A player wins when have all organs (not infected)
    public void checkWinCondition() {
        for (int i = 0; i < nPlayers; i++) {
            if (playerBodies.get(i).areAllOrganHealthy()) {
                for (int j = 0; j < nPlayers; j++) {
                    if (i == j)
                        playerResults[i] = Utils.GameResult.GAME_WIN;
                    else
                        playerResults[j] = Utils.GameResult.GAME_LOSE;
                }
                gameStatus = Utils.GameResult.GAME_END;
                break;
            }
        }
    }

    public void endTurn() {
        turnOrder.endPlayerTurn(this);
    }

    public int getCurrentPlayerID() {
        return turnOrder.getTurnOwner();
    }

    @Override
    public void endGame() {
        System.out.println("Game Results:");
        for (int playerID = 0; playerID < nPlayers; playerID++) {
            if (playerResults[playerID] == Utils.GameResult.GAME_WIN) {
                System.out.println("The winner is the player : " + playerID);
                break;
            }
        }
    }

    // Move all cards from discard deck to draw one and shuffle
    // TODO: check it
    public void discardToDraw() {
        while (discardDeck.getSize()>0) {
            VirusCard card = discardDeck.pick();
            drawDeck.add(card);
        }
        drawDeck.shuffle();
    }
}
