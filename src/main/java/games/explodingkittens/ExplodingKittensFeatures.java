package games.explodingkittens;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateFeatureVector;
import games.explodingkittens.cards.ExplodingKittensCard;

import java.util.ArrayList;
import java.util.Arrays;

public class ExplodingKittensFeatures implements IStateFeatureVector {
    private int nDims = 25; // number of features
    private ArrayList<String> cardTypes = new ArrayList<>(Arrays.asList("EXPLODING_KITTEN", "DEFUSE", "NOPE", "ATTACK", "SKIP", "FAVOR",
            "SHUFFLE", "SEETHEFUTURE", "TACOCAT", "MELONCAT", "FURRYCAT", "BEARDCAT", "RAINBOWCAT"));

    @Override
    public String[] names() {

        return new String[25];
    }

    @Override
    public double[] featureVector(AbstractGameState state, int playerID) {
        // TODO: see the future is not actually encoded here - could leave a placeholder for 3 cards?
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) state;

        double[] obs = new double[this.nDims];
        // player's hand : 12 card types, cards in drawpile, n players alive, n_cards per player
        for (ExplodingKittensCard cardType : ekgs.playerHandCards.get(playerID).getComponents()){
            obs[cardTypes.indexOf(cardType.toString())] += 1;
        }
        obs[13] = ekgs.drawPile.getSize();
        int nPlayersActive = 0;
        for (int i = 0; i < ekgs.getNPlayers(); i++) {
            if (ekgs.getPlayerResults()[i] == CoreConstants.GameResult.GAME_ONGOING) nPlayersActive++;
            obs[15+i] = ekgs.playerHandCards.get(i).getComponents().size();
        }
        obs[14] = nPlayersActive;
        // gamephases are represented here: main/favor/nope/see the future...
        if (ekgs.getGamePhase().equals(CoreConstants.DefaultGamePhase.Main)){
            obs[20] = 1.0;
        } else{
            // find id of gamephase
            for (int i = 0; i < ExplodingKittensGameState.ExplodingKittensGamePhase.values().length; i++) {
                if (ExplodingKittensGameState.ExplodingKittensGamePhase.values()[i].equals(ekgs.getGamePhase())){
                    obs[21+i] = 1.0;
                    break;
                }
            }
        }
        return obs;
    }

//    public double[] getNormalizedObservationVector() {
//        double[] normalized = getObservationVector();
//        for (int i = 0; i < 13; i++) {
//            normalized[i] = normalized[i] / 4;
//        }
//        normalized[13] = normalized[13] / (56 - ((ExplodingKittensParameters)getGameParameters()).nCardsPerPlayer * this.getNPlayers());
//        normalized[14] = normalized[14] / this.getNPlayers();
//        for (int i = 15; i < 15 + getNPlayers(); i++){
//            normalized[i] = normalized[i] / (56 / getNPlayers());
//        }
//        return normalized;
//    }

}
