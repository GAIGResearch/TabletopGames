package web;

import javax.swing.*;
import java.awt.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Swing's repaint manager, which also notes the windows something has asked to be repainted in, so that a
 * {@link FrameStreamer} paints its frame only when it may have changed.
 */
class DirtyTracker extends RepaintManager {

    private static final Set<Window> dirty = ConcurrentHashMap.newKeySet();

    static void install() {
        RepaintManager.setCurrentManager(new DirtyTracker());
    }

    /**
     * Whether something has asked to be repainted in the window since the last call.
     */
    static boolean takeDirty(Window window) {
        return dirty.remove(window);
    }

    static void forget(Window window) {
        dirty.remove(window);
    }

    @Override
    public void addDirtyRegion(JComponent c, int x, int y, int w, int h) {
        mark(c);
        super.addDirtyRegion(c, x, y, w, h);
    }

    @Override
    public void addDirtyRegion(Window window, int x, int y, int w, int h) {
        dirty.add(window);
        super.addDirtyRegion(window, x, y, w, h);
    }

    @Override
    public synchronized void addInvalidComponent(JComponent invalid) {
        mark(invalid);
        super.addInvalidComponent(invalid);
    }

    private static void mark(Component c) {
        Window w = c instanceof Window window ? window : SwingUtilities.getWindowAncestor(c);
        if (w != null) dirty.add(w);
    }
}
