package core;

import evaluation.TunableParameters;
import games.GameType;
import games.battlelore.BattleloreGameParameters;
import games.blackjack.BlackjackParameters;
import games.cantstop.CantStopParameters;
import games.catan.CatanParameters;
import games.coltexpress.ColtExpressParameters;
import games.connect4.Connect4GameParameters;
import games.diamant.DiamantParameters;
import games.dicemonastery.DiceMonasteryParams;
import games.dominion.DominionParameters;
import games.dotsboxes.DBParameters;
import games.explodingkittens.ExplodingKittensParameters;
import games.loveletter.LoveLetterParameters;
import games.pandemic.PandemicParameters;
import games.poker.PokerGameParameters;
import games.stratego.StrategoParams;
import games.sushigo.SGParameters;
import games.terraformingmars.TMGameParameters;
import games.tictactoe.TicTacToeGameParameters;
import games.uno.UnoGameParameters;
import games.virus.VirusGameParameters;

public class ParameterFactory {

    static public AbstractParameters getDefaultParams(GameType game, long seed) {
        switch (game) {
            case Pandemic:
                return new PandemicParameters("data/pandemic/", seed);
            case TicTacToe:
                return new TicTacToeGameParameters(seed);
            case Connect4:
                return new Connect4GameParameters(seed);
            case ExplodingKittens:
                return new ExplodingKittensParameters(seed);
            case LoveLetter:
                return new LoveLetterParameters(seed);
            case Uno:
                return new UnoGameParameters(seed);
            case Virus:
                return new VirusGameParameters(seed);
            case ColtExpress:
                return new ColtExpressParameters(seed);
            case DotsAndBoxes:
                return new DBParameters(seed);
            case Diamant:
                return new DiamantParameters(seed);
            case Dominion:
                return DominionParameters.firstGame(seed);
            case DominionSizeDistortion:
                return DominionParameters.sizeDistortion(seed);
            case DominionImprovements:
                return DominionParameters.improvements(seed);
            case Poker:
                return new PokerGameParameters(seed);
            case Blackjack:
                return new BlackjackParameters(seed);
            case Battlelore:
                return new BattleloreGameParameters("data/battlelore/", seed);
            case DiceMonastery:
                return new DiceMonasteryParams(seed);
            case SushiGo:
                return new SGParameters(seed);
            case Catan:
                return new CatanParameters(seed);
            case Stratego:
                return new StrategoParams(seed);
            case TerraformingMars:
                return new TMGameParameters(seed);
            case CantStop:
                return new CantStopParameters(seed);
        }
        throw new AssertionError("No default Parameters specified for Game " + game);

    }

    static public AbstractParameters createFromFile(GameType game, String fileName) {
        AbstractParameters params = getDefaultParams(game, System.currentTimeMillis());
        if (fileName.isEmpty())
            return params;
        if (params instanceof TunableParameters) {
            TunableParameters.loadFromJSONFile((TunableParameters) params, fileName);
            return params;
        } else {
            throw new AssertionError("JSON parameter initialisation not supported for " + game);
        }
    }

}
