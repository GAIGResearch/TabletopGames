package games.XIIScripta;

import core.AbstractParameters;
import games.GameType;
import games.backgammon.BGGameState;

public class XIIGameState extends BGGameState {

    public XIIGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    /**
     * @return the enum value corresponding to this game, declared in {@link GameType}.
     */
    @Override
    protected GameType _getGameType() {
        return GameType.XIIScripta;
    }


}
