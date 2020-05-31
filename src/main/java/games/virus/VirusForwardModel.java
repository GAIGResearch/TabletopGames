package games.virus;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.virus.cards.*;
import games.virus.components.VirusBody;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Random;

import static games.virus.cards.VirusCard.VirusCardOrgan.Wild;
import static games.virus.cards.VirusCard.VirusCardOrgan.None;

public class VirusForwardModel extends AbstractForwardModel {

    @Override
    public void setup(AbstractGameState firstState) {
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
        vgs.drawDeck = new Deck<>("DrawDeck");
        createCards(vgs);

        vgs.drawDeck.shuffle(new Random(vgs.getGameParameters().getGameSeed()));

        // Create the discard deck, at the beginning it is empty
        vgs.discardDeck = new Deck<>("DiscardDeck");

        // Draw initial cards to each player
        vgs.playerDecks = new ArrayList<>(vgs.getNPlayers());
        drawCardsToPlayers(vgs);
    }

    @Override
    public void next(AbstractGameState gameState, AbstractAction action) {
        action.execute(gameState);
        checkGameEnd((VirusGameState)gameState);
        if (gameState.getGameStatus() == Utils.GameResult.GAME_ONGOING)
            gameState.getTurnOrder().endPlayerTurn(gameState);
    }

    // TODO simplified version with only organ, virus and medicine cards. No wildcards, no treatments
    // Create all the cards and include them into the drawPile
    private void createCards(VirusGameState vgs) {
        VirusGameParameters vgp = (VirusGameParameters) vgs.getGameParameters();

        for (VirusCard.VirusCardOrgan organ: VirusCard.VirusCardOrgan.values()) {
            if (organ != None && organ != Wild) {

                // 5 cards for each organ plus 1 for wild organ
                for (int i=0; i<vgp.nCardsPerOrgan; i++)
                {
                    vgs.drawDeck.add(new VirusCard(organ, VirusCard.VirusCardType.Organ));
                }
                // 4 cards for each virus plus 1 for wild virus
                for (int i=0; i<vgp.nCardsPerVirus; i++)
                {
                    vgs.drawDeck.add(new VirusCard(organ, VirusCard.VirusCardType.Virus));
                }
                // 4 cards for each medicine plus 4 for wild medicine
                for (int i=0; i<vgp.nCardsPerMedicine; i++)
                {
                    vgs.drawDeck.add(new VirusCard(organ, VirusCard.VirusCardType.Medicine));
                }
            }
        }

//        VirusOrganCard cardOrganWild = new VirusOrganCard(Wild);
//        vgs.drawDeck.add(cardOrganWild);

//        VirusVirusCard cardVirusWild = new VirusVirusCard(Wild);
//        vgs.drawDeck.add(cardVirusWild);

//        VirusMedicineCard cardMedicineWild = new VirusMedicineCard(Wild);
//        vgs.drawDeck.add(cardMedicineWild);

//        // 2 cards for each treatment
//        for (VirusTreatmentCard.TreatmentType tt: VirusTreatmentCard.TreatmentType.values()) {
//            for (int i=0; i<vgp.nCardsPerTreatment; i++)
//            {
//                vgs.drawDeck.add(new VirusTreatmentCard(tt));
//            }
//        }

    }

    private void drawCardsToPlayers(VirusGameState vgs) {
        for (int i = 0; i < vgs.getNPlayers(); i++) {
            String playerDeckName = "Player" + i + "Deck";
            vgs.playerDecks.add(new Deck<>(playerDeckName));
            for (int j = 0; j < 3; j++) {
                vgs.playerDecks.get(i).add(vgs.drawDeck.draw());
            }
        }
    }

    // A player wins when have all organs (not infected)
    public void checkGameEnd(VirusGameState vgs) {
        for (int i = 0; i < vgs.getNPlayers(); i++) {
            if (vgs.playerBodies.get(i).areAllOrganHealthy()) {
                for (int j = 0; j < vgs.getNPlayers(); j++) {
                    if (i == j)
                        vgs.setPlayerResult(Utils.GameResult.GAME_WIN, i);
                    else
                        vgs.setPlayerResult(Utils.GameResult.GAME_LOSE, j);
                }
                vgs.setGameStatus(Utils.GameResult.GAME_END);
                break;
            }
        }
    }
}
