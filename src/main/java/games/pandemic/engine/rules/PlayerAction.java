package games.pandemic.engine.rules;

import core.AbstractGameState;
import core.components.Card;
import core.components.Counter;
import core.content.PropertyString;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;
import games.pandemic.actions.MovePlayer;
import games.pandemic.actions.QuietNight;
import games.pandemic.actions.TreatDisease;
import utilities.Hash;

import static games.pandemic.PandemicConstants.nameHash;

public class PlayerAction extends RuleNode {

    int n_initial_disease_cubes;
    public PlayerAction(int n_initial_disease_cubes) {
        super(true);
        this.n_initial_disease_cubes = n_initial_disease_cubes;
    }

    @Override
    protected boolean run(AbstractGameState gs) {
        action.execute(gs);
        PandemicGameState pgs = (PandemicGameState)gs;

        if (action instanceof QuietNight) {
            ((PandemicGameState)gs).setQuietNight(true);
        } else if (action instanceof MovePlayer){
            // if player is Medic and a disease has been cured, then it should remove all cubes when entering the city
            int playerIdx = pgs.getTurnOrder().getCurrentPlayer(gs);
            Card playerCard = (Card) pgs.getComponent(PandemicConstants.playerCardHash, playerIdx);
            String roleString = ((PropertyString)playerCard.getProperty(nameHash)).value;

            if (roleString.equals("Medic")){
                for (String color: PandemicConstants.colors){
                    Counter diseaseToken = (Counter)((PandemicGameState)gs).getComponent(Hash.GetInstance().hash("Disease " + color));
                    String city = ((MovePlayer)action).getDestination();
                    boolean disease_cured = diseaseToken.getValue() > 0;
                    if (disease_cured){
                        new TreatDisease(n_initial_disease_cubes, color, city, true);
                    }
                }
            }
        }
        return true;
    }
}
