package gui.views;

import core.components.Component;

import javax.swing.*;
import java.awt.*;

public abstract class ComponentView extends JComponent {
    protected core.components.Component component;
    protected int width, height;

    public ComponentView(core.components.Component c, int width, int height) {
        updateComponent(c);
        this.width = width;
        this.height = height;
    }

    public void updateComponent(core.components.Component c) {
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

    @Override
    public void setPreferredSize(Dimension preferredSize) {
        this.width = preferredSize.width;
        this.height = preferredSize.height;
    }
}
