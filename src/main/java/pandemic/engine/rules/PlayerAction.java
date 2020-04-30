package pandemic.engine.rules;

import components.Card;
import components.Counter;
import content.PropertyString;
import core.GameState;
import pandemic.Constants;
import pandemic.PandemicGameState;
import pandemic.actions.MovePlayer;
import pandemic.actions.QuietNight;
import pandemic.actions.TreatDisease;

import static pandemic.Constants.nameHash;

public class PlayerAction extends RuleNode {

    int n_initial_disease_cubes;
    public PlayerAction(int n_initial_disease_cubes) {
        super(true);
        this.n_initial_disease_cubes = n_initial_disease_cubes;
    }

    @Override
    protected boolean run(GameState gs) {
        action.execute(gs);

        int nReactivePlayers = gs.getReactivePlayers().size();
        if (nReactivePlayers == 0) {
            // Only advance round step if no one is reacting
            gs.roundStep += 1;
        }

        if (action instanceof QuietNight) {
            ((PandemicGameState)gs).setQuietNight(true);
        } else if (action instanceof MovePlayer){
            // if player is Medic and a disease has been cured, then it should remove all cubes when entering the city
            int playerIdx = gs.getActivePlayer();
            Card playerCard = (Card) gs.getAreas().get(playerIdx).getComponent(Constants.playerCardHash);
            String roleString = ((PropertyString)playerCard.getProperty(nameHash)).value;

            if (roleString.equals("Medic")){
                for (String color: Constants.colors){
                    Counter diseaseToken = gs.findCounter("Disease " + color);
                    String city = ((MovePlayer)action).getDestination();
                    boolean disease_cured = diseaseToken.getValue() > 0;
                    if (disease_cured){
                        new TreatDisease(n_initial_disease_cubes, color, city, true);
                    }
                }
            }
        }
        gs.removeReactivePlayer();  // Reaction (if any) done.
        return true;
    }
}
