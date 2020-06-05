package gui.views;

import core.components.Component;

import javax.swing.*;
import java.awt.*;

public abstract class ComponentView extends JComponent {
    protected Component component;
    protected int width, height;

    public ComponentView(Component c, int width, int height) {
        updateComponent(c);
        this.width = width;
        this.height = height;
    }

    public void updateComponent(Component c) {
        this.component = c;
        if (component != null) {
            setToolTipText("ID: " + component.getComponentID());
        }
    }

    public Component getComponent() {
        return component;
    }

    @Override
    protected abstract void paintComponent(Graphics g);

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }
}
