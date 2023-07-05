package core.interfaces;

import java.util.Set;

public interface IGameEvent {
    String name();
    Set<IGameEvent> getValues();
}
