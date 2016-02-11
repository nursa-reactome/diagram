package org.reactome.web.diagram.renderers.layout.s100;

import org.reactome.web.diagram.data.layout.Coordinate;
import org.reactome.web.diagram.data.layout.DiagramObject;
import org.reactome.web.diagram.data.layout.Node;
import org.reactome.web.diagram.data.layout.SummaryItem;
import org.reactome.web.diagram.data.layout.category.ShapeCategory;
import org.reactome.web.diagram.renderers.common.HoveredItem;
import org.reactome.web.diagram.renderers.layout.abs.ChemicalAbstractRenderer;
import org.reactome.web.diagram.util.AdvancedContext2d;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class ChemicalRenderer100 extends ChemicalAbstractRenderer {
    @Override
    public void draw(AdvancedContext2d ctx, DiagramObject item, Double factor, Coordinate offset) {
        super.draw(ctx, item, factor, offset);
        drawSummaryItems(ctx, (Node) item, factor, offset);
    }

//    @Override
//    public void drawText(AdvancedContext2d ctx, DiagramObject item, Double factor, Coordinate offset) {
//        Node node = (Node) item;
//        GraphObject graphObject = node.getGraphObject();
//        if(graphObject instanceof GraphSimpleEntity){
//            GraphSimpleEntity simpleEntity = (GraphSimpleEntity) graphObject;
//            if(simpleEntity.getChemicalImage() != null){
//                NodeProperties prop = node.getProp();
//                Coordinate pos = CoordinateFactory.get(prop.getX(), prop.getY()).transform(factor, offset);
//                double delta = prop.getHeight() * factor;
//                ctx.drawImage(simpleEntity.getChemicalImage(), pos.getX(), pos.getY(), delta, delta);
//            }
//        } else {
//            super.drawText(ctx, item, factor, offset);
//        }
//    }

    @Override
    public HoveredItem getHovered(DiagramObject item, Coordinate pos) {
        Node node = (Node) item;

        SummaryItem interactorsSummary = node.getInteractorsSummary();
        if (interactorsSummary != null) {
            if (ShapeCategory.isHovered(interactorsSummary.getShape(), pos)) {
                return new HoveredItem(node.getId(), interactorsSummary);
            }
        }
        return super.getHovered(item, pos);
    }

    @Override
    public void highlight(AdvancedContext2d ctx, DiagramObject item, Double factor, Coordinate offset) {
        super.highlight(ctx, item, factor, offset);
        drawSummaryItems(ctx, (Node) item, factor, offset);
    }

    @Override
    public boolean isVisible(DiagramObject item) {
        return true;
    }
}