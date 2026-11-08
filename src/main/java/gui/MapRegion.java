package gui;

import javax.swing.*;
import java.awt.*;

/**
 * A named part of a board drawn as a map, such as a province or a territory, on which a player may act. See
 * {@link AbstractGUIManager#getMapRegions()}.
 *
 * @param id    the region's identifier, which {@link MapMove}s refer to
 * @param name  the name shown to the player
 * @param view  the component the shape is drawn in
 * @param shape the region, in the view's coordinates
 */
public record MapRegion(String id, String name, JComponent view, Shape shape) {
}
