package games.stratego;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import evaluation.TunableParameters;
import games.stratego.components.Piece;
import utilities.Utils;

import java.util.ArrayList;

public class StrategoHeuristic extends TunableParameters implements IStateHeuristic {

    double bombValue = 0.0;
    double spyValue = 0.1;
    double scoutValue = 0.2;
    double minerValue = 0.3;
    double sergeantValue = 0.4;
    double lieutenantValue = 0.5;
    double captainValue = 0.6;
    double majorValue = 0.7;
    double colonelValue = 0.8;
    double generalValue = 0.9;
    double marshalValue = 1.0;

    public StrategoHeuristic() {
        addTunableParameter("bombValue", 0.0);
        addTunableParameter("spyValue", 0.1);
        addTunableParameter("scoutValue", 0.2);
        addTunableParameter("minerValue", 0.3);
        addTunableParameter("sergeantValue", 0.4);
        addTunableParameter("lieutenantValue", 0.5);
        addTunableParameter("captainValue", 0.6);
        addTunableParameter("majorValue", 0.7);
        addTunableParameter("colonelValue", 0.8);
        addTunableParameter("generalValue", 0.9);
        addTunableParameter("marshalValue", 1.0);
    }

    @Override
    public void _reset() {
        bombValue = (double) getParameterValue("bombValue");
        spyValue = (double) getParameterValue("spyValue");
        scoutValue = (double) getParameterValue("scoutValue");
        minerValue = (double) getParameterValue("minerValue");
        sergeantValue = (double) getParameterValue("sergeantValue");
        lieutenantValue = (double) getParameterValue("lieutenantValue");
        captainValue = (double) getParameterValue("captainValue");
        majorValue = (double) getParameterValue("majorValue");
        colonelValue = (double) getParameterValue("colonelValue");
        generalValue = (double) getParameterValue("generalValue");
        marshalValue = (double) getParameterValue("marshalValue");

    }
    @Override
    protected StrategoHeuristic _copy() {
        StrategoHeuristic retValue = new StrategoHeuristic();
        retValue.bombValue = bombValue;
        retValue.spyValue = spyValue;
        retValue.scoutValue = scoutValue;
        retValue.minerValue = minerValue;
        retValue.sergeantValue = sergeantValue;
        retValue.lieutenantValue = lieutenantValue;
        retValue.captainValue = captainValue;
        retValue.majorValue = majorValue;
        retValue.colonelValue = colonelValue;
        retValue.generalValue = generalValue;
        retValue.marshalValue = marshalValue;
        return retValue;
    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        StrategoGameState state = (StrategoGameState) gs;
        Utils.GameResult playerResult = gs.getPlayerResults()[playerId];

        if(playerResult == Utils.GameResult.LOSE) {
            return -1;
        }
        if(playerResult == Utils.GameResult.WIN) {
            return 1;
        }

        String playerAlliance = StrategoConstants.playerMapping.get(playerId);
        ArrayList<Double> playerRanks = new ArrayList<>();
        ArrayList<Double> opponentRanks = new ArrayList<>();
        double pieceValue=0;

        for (Piece piece : state.gridBoard.getComponents()){
            pieceValue=0;
            if (piece != null){
                switch (piece.toString()){
                    case "Bomb": pieceValue = bombValue;
                        break;
                    case "Spy": pieceValue = spyValue;
                        break;
                    case "Scout": pieceValue = scoutValue;
                        break;
                    case "Miner": pieceValue = minerValue;
                        break;
                    case "Sergeant": pieceValue = sergeantValue;
                        break;
                    case "Lieutenant": pieceValue = lieutenantValue;
                        break;
                    case "Captain": pieceValue = captainValue;
                        break;
                    case "Major": pieceValue = majorValue;
                        break;
                    case "Colonel": pieceValue = colonelValue;
                        break;
                    case "General": pieceValue = generalValue;
                        break;
                    case "Marshal": pieceValue = marshalValue;
                        break;
                }
                if (playerAlliance.equals(piece.getPieceAlliance().toString())) {
                    playerRanks.add(pieceValue);
                } else{
                    opponentRanks.add(pieceValue);
                }
            }
        }
        double sum = 0.0;
        for (double item : playerRanks){
            sum += item;
        }
        double avgPlayerRank = sum/playerRanks.size();
        sum=0.0;
        for (double item : opponentRanks){
            sum += item;
        }
        double avgOpponentRank = sum/opponentRanks.size();

        return avgPlayerRank - avgOpponentRank;
    }

    @Override
    public StrategoHeuristic instantiate() {
        return this._copy();
    }

}
