package org.reactome.web.diagram.data.interactors.model;

import org.reactome.web.diagram.data.interactors.common.InteractorBox;
import org.reactome.web.diagram.data.layout.Coordinate;
import uk.ac.ebi.pwp.structures.quadtree.client.QuadTreeBox;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class DiagramInteractor implements QuadTreeBox {

    protected Double minX, minY, maxX, maxY;
    private boolean visible = true;

    DiagramInteractor() {
    }

    @Override
    public double getMinX() {
        return minX;
    }

    @Override
    public double getMinY() {
        return minY;
    }

    @Override
    public double getMaxX() {
        return maxX;
    }

    @Override
    public double getMaxY() {
        return maxY;
    }

    public InteractorBox transform(double factor, Coordinate delta) {
        return new InteractorBox(this).transform(factor, delta);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "minX=" + minX +
                ", minY=" + minY +
                ", maxX=" + maxX +
                ", maxY=" + maxY +
                ", visible=" + visible +
                '}';
    }
}
