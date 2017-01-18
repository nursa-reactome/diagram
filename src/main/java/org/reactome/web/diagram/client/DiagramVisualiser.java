package org.reactome.web.diagram.client;

import com.google.gwt.animation.client.AnimationScheduler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.RequiresResize;
import org.reactome.web.analysis.client.model.AnalysisType;
import org.reactome.web.diagram.client.visualisers.Visualiser;
import org.reactome.web.diagram.common.DiagramAnimationHandler;
import org.reactome.web.diagram.common.DisplayManager;
import org.reactome.web.diagram.data.AnalysisStatus;
import org.reactome.web.diagram.data.Context;
import org.reactome.web.diagram.data.DiagramStatus;
import org.reactome.web.diagram.data.GraphObjectFactory;
import org.reactome.web.diagram.data.graph.model.GraphEvent;
import org.reactome.web.diagram.data.graph.model.GraphObject;
import org.reactome.web.diagram.data.graph.model.GraphPhysicalEntity;
import org.reactome.web.diagram.data.interactors.model.DiagramInteractor;
import org.reactome.web.diagram.data.interactors.model.InteractorEntity;
import org.reactome.web.diagram.data.layout.Coordinate;
import org.reactome.web.diagram.data.layout.DiagramObject;
import org.reactome.web.diagram.data.layout.Node;
import org.reactome.web.diagram.data.layout.SummaryItem;
import org.reactome.web.diagram.data.layout.impl.CoordinateFactory;
import org.reactome.web.diagram.data.loader.AnalysisDataLoader;
import org.reactome.web.diagram.data.loader.AnalysisTokenValidator;
import org.reactome.web.diagram.events.*;
import org.reactome.web.diagram.handlers.*;
import org.reactome.web.diagram.renderers.common.HoveredItem;
import org.reactome.web.diagram.util.ViewportUtils;
import org.reactome.web.diagram.util.chemical.ChemicalImageLoader;
import org.reactome.web.diagram.util.pdbe.PDBeLoader;
import uk.ac.ebi.pwp.structures.quadtree.client.Box;

import java.util.Collection;

import static org.reactome.web.diagram.data.content.Content.Type.DIAGRAM;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 */
public class DiagramVisualiser extends AbsolutePanel implements Visualiser, RequiresResize,
        UserActionsManager.Handler,
        DiagramAnimationHandler,
        DiagramObjectsFlaggedHandler, DiagramObjectsFlagResetHandler,
        DiagramProfileChangedHandler, AnalysisProfileChangedHandler, InteractorProfileChangedHandler,
        StructureImageLoadedHandler {
    protected EventBus eventBus;

    private boolean initialised = false;
    private int viewportWidth = 0;
    private int viewportHeight = 0;

    private final DiagramCanvas canvas; //Canvas only created once and reused every time a new diagram is loaded
    private final DiagramManager diagramManager;

    private Context context;
//    private LoaderManager loaderManager;

    private UserActionsManager userActionsManager;

//    private String flagTerm;
    private AnalysisStatus analysisStatus;
    private LayoutManager layoutManager;
    private InteractorsManager interactorsManager;

    // mouse positions relative to canvas (not the model)
    // Do not assign the same value at the beginning
    private Coordinate mouseCurrent = CoordinateFactory.get(-100, -100);
    private Coordinate mousePrevious = CoordinateFactory.get(-200, -200);

    private boolean forceDraw = false;

    DiagramVisualiser() {
        super();
        this.canvas = new DiagramCanvas(eventBus);
//        this.loaderManager = new LoaderManager(eventBus);
//        AnalysisDataLoader.initialise(eventBus);
        PDBeLoader.initialise(eventBus);
        ChemicalImageLoader.initialise(eventBus);
        this.layoutManager = new LayoutManager(eventBus);
        this.interactorsManager = new InteractorsManager(eventBus);

        this.userActionsManager = new UserActionsManager(this, canvas);

        this.diagramManager = new DiagramManager(new DisplayManager(this));
//        this.initWidget(this.canvas);
        this.add(this.canvas);
        this.getElement().addClassName("pwp-DiagramVisualiser"); //IMPORTANT!
    }

    protected void initialise() {
        if(!initialised) {

            this.initialised = true;
            this.viewportWidth = getOffsetWidth();
            this.viewportHeight = getOffsetHeight();

            this.initHandlers();
            AnimationScheduler.get().requestAnimationFrame(new AnimationScheduler.AnimationCallback() {
                @Override
                public void execute(double timestamp) {
                    doUpdate();
                    AnimationScheduler.get().requestAnimationFrame(this); // Call it again.
                }
            });
        }
    }

    private void initHandlers() {
        canvas.addUserActionsHandlers(userActionsManager);

//        //Attaching this as a KeyDownHandler
//        RootPanel.get().addDomHandler(this, KeyDownEvent.getType());

        eventBus.addHandler(AnalysisProfileChangedEvent.TYPE, this);
//        eventBus.addHandler(AnalysisResultRequestedEvent.TYPE, this);
//        eventBus.addHandler(AnalysisResultLoadedEvent.TYPE, this);
//        eventBus.addHandler(AnalysisResetEvent.TYPE, this);
//        eventBus.addHandler(ExpressionColumnChangedEvent.TYPE, this);
//
//        eventBus.addHandler(GraphObjectSelectedEvent.TYPE, this);
//        eventBus.addHandler(GraphObjectHoveredEvent.TYPE, this);
//
//        eventBus.addHandler(ContentLoadedEvent.TYPE, this);
//        eventBus.addHandler(ContentRequestedEvent.TYPE, this);
        eventBus.addHandler(DiagramObjectsFlaggedEvent.TYPE, this);
//        eventBus.addHandler(DiagramObjectsFlagRequestedEvent.TYPE, this);
        eventBus.addHandler(DiagramObjectsFlagResetEvent.TYPE, this);
//        eventBus.addHandler(CanvasExportRequestedEvent.TYPE, this);
//
        eventBus.addHandler(DiagramProfileChangedEvent.TYPE, this);
//        eventBus.addHandler(IllustrationSelectedEvent.TYPE, this);
//
//        eventBus.addHandler(InteractorsCollapsedEvent.TYPE, this);
//        eventBus.addHandler(InteractorHoveredEvent.TYPE, this);
//        eventBus.addHandler(InteractorsResourceChangedEvent.TYPE, this);
//        eventBus.addHandler(InteractorsLayoutUpdatedEvent.TYPE, this);
//        eventBus.addHandler(InteractorsFilteredEvent.TYPE, this);
//        eventBus.addHandler(InteractorSelectedEvent.TYPE, this);
        eventBus.addHandler(InteractorProfileChangedEvent.TYPE, this);
//
//        eventBus.addHandler(LayoutLoadedEvent.TYPE, this);
//        eventBus.addHandler(InteractorsLoadedEvent.TYPE, this);
//        eventBus.addHandler(ThumbnailAreaMovedEvent.TYPE, this);
//        eventBus.addHandler(ControlActionEvent.TYPE, this);
//
//        eventBus.addHandler(StructureImageLoadedEvent.TYPE, this);
    }

    private void doUpdate() {
        if (context == null || context.getContent().getType() != DIAGRAM) return;
        if (forceDraw) {
            forceDraw = false;
            Box visibleArea = context.getVisibleModelArea(viewportWidth, viewportHeight);
            draw(visibleArea);
            drawInteractors(visibleArea);
        }else if (!mouseCurrent.equals(mousePrevious)) {
            mousePrevious = mouseCurrent;
            DiagramInteractor hoveredInteractor = getHoveredInteractor();
            canvas.setCursor(hoveredInteractor == null ? Style.Cursor.DEFAULT : Style.Cursor.POINTER);
            if (hoveredInteractor != null) {
                resetHighlight(true); // This resets the layout highlight -> please note that the method is defined in the DiagramViewer interface
            } else { // It is needed otherwise the getHoveredDiagramObject will find a possible diagram layout object behind the interactor
                HoveredItem hovered = getHoveredDiagramObject();
                DiagramObject item = hovered != null ? hovered.getHoveredObject() : null;
                canvas.setCursor(item == null ? Style.Cursor.DEFAULT : Style.Cursor.POINTER);
                highlightHoveredItem(hovered);
            }
            highlightInteractor(hoveredInteractor); //if hovered interactor is null, calling highlightInteractor will clear the previous highlight
        }
    }

    private void draw(Box visibleArea) {
        if (context == null) return;
        long start = System.currentTimeMillis();
        canvas.clear();
        Collection<DiagramObject> items = context.getContent().getVisibleItems(visibleArea);
        canvas.render(items, context);
        canvas.select(layoutManager.getSelectedDiagramObjects(), context);
        canvas.highlight(layoutManager.getHovered(), context);
        canvas.decorators(layoutManager.getHovered(), context);
        canvas.halo(layoutManager.getHalo(), context);
        canvas.flag(layoutManager.getFlagged(), context);
        long time = System.currentTimeMillis() - start;
        this.eventBus.fireEventFromSource(new DiagramRenderedEvent(context.getContent(), visibleArea, items.size(), time), this);
    }

    private void drawInteractors(Box visibleArea) {
        if (context == null) return;
//        long start = System.currentTimeMillis();
        String resource = interactorsManager.getCurrentResource();
        Collection<DiagramInteractor> items = context.getInteractors().getVisibleInteractors(resource, visibleArea);
        canvas.renderInteractors(items, context);
        canvas.highlightInteractor(interactorsManager.getHovered(), context);
//        long time = System.currentTimeMillis() - start;
//        this.eventBus.fireEventFromSource(new DiagramRenderedEvent(context.getContent(), visibleArea, items.size(), time), this);
    }

    @Override
    public DiagramStatus getDiagramStatus() {
        return this.context.getDiagramStatus();
    }

    @Override
    public void transform(Coordinate offset, double factor) {
        //An animation can be working and a new pathway might be requested :(
        if (this.context == null) return;
        DiagramStatus status = this.context.getDiagramStatus();
        status.setOffset(offset);
        status.setFactor(factor);
        this.forceDraw = true;
        Box visibleArea = this.context.getVisibleModelArea(viewportWidth, viewportHeight);
        this.eventBus.fireEventFromSource(new DiagramZoomEvent(factor, visibleArea), this);
    }

    private void highlightHoveredItem(HoveredItem hovered) {
        if (layoutManager.isHighlighted(hovered)) return;
        canvas.highlight(hovered, context);
        canvas.decorators(hovered, context);
        GraphObjectHoveredEvent event = layoutManager.setHovered(hovered);
        if (event != null) {
            // Outside notification is handled at a higher level.
            this.eventBus.fireEventFromSource(event, this);
//            fireEvent(event);
        }
    }

    public void highlightInteractor(DiagramInteractor hovered) {
        if (interactorsManager.isHighlighted(hovered)) return;
        // setInteractorHovered knows when an interactor is being dragged, so it won't set a new one if that case
        if (userActionsManager.setInteractorHovered(hovered)) {
            canvas.highlightInteractor(hovered, context);
            InteractorHoveredEvent event = interactorsManager.setHovered(hovered);
            if (event != null) {
                this.eventBus.fireEventFromSource(event, this);
//                fireEvent(event); //needs outside notification
            }
        }
    }

    @Override
    public boolean highlightGraphObject(GraphObject graphObject, boolean notify) {
        boolean rtn = false;
        HoveredItem hovered = new HoveredItem(graphObject);
        if (!layoutManager.isHighlighted(hovered)) {
            canvas.highlight(hovered, context);
            if (notify) {
                //we don't rely on the listener of the following event because finer grain of the hovering is lost
                eventBus.fireEventFromSource(new GraphObjectHoveredEvent(graphObject), this);
            }
            rtn = true;
        }
        return rtn;
    }

    @Override
    public void loadDiagram(String stId) {
        if (stId != null) {
            if (this.context == null || !stId.equals(this.context.getContent().getStableId())) {
                this.load(stId); //Names are interchangeable because there are symlinks
            }
        }
    }

//    @Override
//    public void loadDiagram(Long dbId) {
//        if (dbId != null) {
//            if (this.context == null || !dbId.equals(this.context.getContent().getDbId())) {
//                this.load("" + dbId); //Names are interchangeable because there are symlinks
//            }
//        }
//    }

    private void load(String identifier) {
        eventBus.fireEventFromSource(new ContentRequestedEvent(identifier), this);
    }

//    private void clearAnalysisOverlay(){
//        context.clearAnalysisOverlay();
//        interactorsManager.clearAnalysisOverlay();
//    }

//    private void loadAnalysis(AnalysisStatus analysisStatus) {
//        if (analysisStatus == null) {
//            if (this.analysisStatus != null) {
//                this.eventBus.fireEventFromSource(new AnalysisResetEvent(false), this);
//            }
//        } else if (!analysisStatus.equals(this.context.getAnalysisStatus())) {
//            this.analysisStatus = analysisStatus;
//            clearAnalysisOverlay();
//            AnalysisDataLoader.get().loadAnalysisResult(analysisStatus, this.context.getContent());
//        }
//    }

//    @Override
//    public void onAnalysisReset(AnalysisResetEvent event) {
//        if (event.getFireExternally()) {
//            fireEvent(event);
//        }
//        this.resetAnalysis();
//        this.canvas.setWatermarkURL(this.context, layoutManager.getSelected(), this.flagTerm);
//    }

//    @Override
//    public void onAnalysisResultLoaded(AnalysisResultLoadedEvent event) {
//        analysisStatus.setAnalysisSummary(event.getSummary());
//        analysisStatus.setExpressionSummary(event.getExpressionSummary());
//        context.setAnalysisOverlay(analysisStatus, event.getFoundElements(), event.getPathwaySummaries());
//        interactorsManager.setAnalysisOverlay(event.getFoundElements(), context.getContent().getIdentifierMap());
//        this.canvas.setWatermarkURL(this.context, layoutManager.getSelected(), this.flagTerm);
//        forceDraw = true;
//    }

//    @Override
//    public void onAnalysisResultRequested(AnalysisResultRequestedEvent event) {
//        clearAnalysisOverlay();
//        this.analysisStatus.setExpressionSummary(null);
//        forceDraw = true;
//    }

    @Override
    public void loadAnalysis(){
        forceDraw = true;
    }

    @Override
    public void resetAnalysis(){
        forceDraw = true;
    }

    @Override
    public void onAnalysisProfileChanged(AnalysisProfileChangedEvent event) {
        forceDraw = true;
    }

    @Override
    public void exportView() {
        if (context != null) {
            canvas.exportImage(context.getContent().getStableId());
        }
    }

    @Override
    public void onDiagramObjectsFlagged(DiagramObjectsFlaggedEvent event) {
        layoutManager.setFlagged(event.getFlaggedItems());
        this.canvas.flag(event.getFlaggedItems(), this.context);
    }

    @Override
    public void onDiagramObjectsFlagReset(DiagramObjectsFlagResetEvent event) {
        if(layoutManager.resetFlagged()){
            this.canvas.flag(layoutManager.getFlagged(), this.context);
        }
    }

    @Override
    public void contentLoaded(Context context) { //on contentFullyLoaded
        this.context = context;
        this.context.restoreDialogs();
    }

    @Override
    public void contentRequested() {
        this.resetSelection(true);
        this.resetHighlight(true);
        this.resetDialogs();
        this.diagramManager.cancelDisplayAnimation();
        this.resetContext();
    }

    @Override
    public void onExpressionColumnChanged(ExpressionColumnChangedEvent e) {
        Scheduler.get().scheduleDeferred(() -> {
            Coordinate model = context.getDiagramStatus().getModelCoordinate(mouseCurrent);
            DiagramObject hovered = layoutManager.getHoveredDiagramObject();
            canvas.notifyHoveredExpression(hovered, model);
            forceDraw = true; //We give priority to other listeners here
        });
    }

//    @Override
//    public void onGraphObjectSelected(final GraphObjectSelectedEvent event) {
//        GraphObject graphObject = event.getGraphObject();
//        if (!layoutManager.isSelected(graphObject)) {
//            layoutManager.setSelected(graphObject);
//            this.canvas.setWatermarkURL(this.context, layoutManager.getSelected(), this.flagTerm);
//            if (event.getZoom()) {
//                this.diagramManager.displayDiagramObjects(layoutManager.getHalo());
//            }
//            forceDraw = true;
//            if (event.getFireExternally()) {
//                fireEvent(event);
//            }
//        }
//    }

    @Override
    public void onIllustrationSelected(IllustrationSelectedEvent event) {
        this.canvas.setIllustration(event.getUrl());
    }

    @Override
    public void onInteractorHovered(InteractorHoveredEvent event) {
        //In order to have fine grain hovering capabilities, this class is not taking actions for onInteractorHovered
        //when it is fired by its own, so we ONLY want to do the STANDARD action (highlight) when the event comes from
        //the outside. That is the reason of the next line of code
        if (event.getSource().equals(this)) return;
        highlightInteractor(event.getInteractor());
    }

    @Override
    public void onInteractorsCollapsed(InteractorsCollapsedEvent event) {
        Collection<DiagramObject> diagramObjects = context.getContent().getDiagramObjects();
        context.getInteractors().resetBurstInteractors(event.getResource(), diagramObjects);
        forceDraw = true;
    }

    @Override
    public void onInteractorsFiltered(InteractorsFilteredEvent event) {
        Box visibleArea = context.getVisibleModelArea(viewportWidth, viewportHeight);
        drawInteractors(visibleArea);
    }

    @Override
    public void onInteractorsLayoutUpdated(InteractorsLayoutUpdatedEvent event) {
        Box visibleArea = context.getVisibleModelArea(viewportWidth, viewportHeight);
        drawInteractors(visibleArea);
    }

    @Override
    public void onInteractorsLoaded(InteractorsLoadedEvent event) {
        forceDraw = true;
    }

    @Override
    public void onInteractorsResourceChanged(InteractorsResourceChangedEvent event) {
        context.getContent().clearDisplayedInteractors();
        if(context.getInteractors().isInteractorResourceCached(event.getResource().getIdentifier())) {
            context.getInteractors().restoreInteractorsSummary(event.getResource().getIdentifier(), context.getContent());
        }
        forceDraw = true;
    }

    @Override
    public void onInteractorSelected(InteractorSelectedEvent event) {
        String url = event.getUrl();
        if (url != null) {
            Window.open(url, "_blank", "");
        }
    }

    @Override
    public void layoutLoaded(Context context) {
        this.setContext(context);
        this.fitDiagram(false);
    }

    @Override
    protected void onLoad() {
        super.onLoad();
        Scheduler.get().scheduleDeferred(() -> initialise());
    }

    @Override
    public void onDiagramProfileChanged(DiagramProfileChangedEvent event) {
        forceDraw = true;
    }

    @Override
    public void onInteractorProfileChanged(InteractorProfileChangedEvent event) {
        forceDraw = true;
    }

    @Override
    public void onLayoutImageLoaded(StructureImageLoadedEvent event) {
        forceDraw = true;
    }

    @Override
    public void onResize() {
        this.viewportWidth = getOffsetWidth();
        this.viewportHeight = getOffsetHeight();
        this.forceDraw = true;

        if (this.context != null) {
            Box visibleArea = this.context.getVisibleModelArea(viewportWidth, viewportHeight);
            this.eventBus.fireEventFromSource(new ViewportResizedEvent(getOffsetWidth(), getOffsetHeight(), visibleArea), this);
        }
    }

    @Override
    public void onThumbnailAreaMoved(ThumbnailAreaMovedEvent event) {
        this.padding(event.getCoordinate().multiply(context.getDiagramStatus().getFactor()));
    }

    private void resetDialogs() {
        if (this.context != null) {
            this.context.hideDialogs();
        }
    }

//    private void resetIllustration() { //TODO: remove from here. Move it to ViewerContainer
//        this.canvas.resetIllustration();
//    }

    @Override
    public boolean resetHighlight(boolean notify) {
        boolean rtn = false;
        if (context==null) return rtn;
        if (layoutManager.resetHovered()) {
            canvas.highlight(null, context);
            if (notify) {
                eventBus.fireEventFromSource(new GraphObjectHoveredEvent(), this);
            }
            rtn = true;
        }
        return rtn;
    }

    @Override
    public boolean resetSelection(boolean notify) {
        boolean rtn = false;
        if (context==null) return rtn;
        if (layoutManager.resetSelected()) {
            forceDraw = true;
            if(notify) {
                eventBus.fireEventFromSource(new GraphObjectSelectedEvent(null, false), this);
            }
            rtn = true;
        }
        return rtn;
    }

    @Override
    public boolean selectGraphObject(GraphObject graphObject, boolean notify){
        if (graphObject != null) {
            return setSelection(new HoveredItem(graphObject), true, false, true);
        } else {
            return resetSelection(notify);
        }
    }

//    @Override
//    public void setAnalysisToken(String token, String resource) {
//        final AnalysisStatus analysisStatus = (token == null) ? null : new AnalysisStatus(eventBus, token, resource);
//        AnalysisTokenValidator.checkTokenAvailability(token, new AnalysisTokenValidator.TokenAvailabilityHandler() {
//            @Override
//            public void onTokenAvailabilityChecked(boolean available, String message) {
//                if (available) {
//                    loadAnalysis(analysisStatus);
//                } else {
//                    eventBus.fireEventFromSource(new DiagramInternalErrorEvent(message), DiagramVisualiser.this);
//                }
//            }
//        });
//    }

    @Override
    public void setMousePosition(Coordinate mouse){
        mouseCurrent = mouse;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) onResize(); //ToDo check if this has to be moved one layer up
        if (context != null) {
            if (visible) context.restoreDialogs();
            else context.hideDialogs();
        }
    }

    @Override
    public void showDialog(DiagramObject item){
        this.context.showDialog(this.eventBus, item, this.canvas);
    }

    @Override
    public HoveredItem getHoveredDiagramObject() {
        Coordinate model = context.getDiagramStatus().getModelCoordinate(mouseCurrent);
        Collection<HoveredItem> hoveredItems = layoutManager.getHovered(model);
        for (HoveredItem hovered : hoveredItems) {
            DiagramObject item = context.getContent().getDiagramObject(hovered.getDiagramId());
            hovered.setDiagramObject(item); //VERY IMPORTANT! Here we have access to the content and can transform diagramId to diagramObject

            //TODO: The graph has to be pruned in the server side
            if (item == null || item.getIsFadeOut() != null) continue;

            if (item.getGraphObject() instanceof GraphPhysicalEntity || item.getGraphObject() instanceof GraphEvent) {
                this.notifyHoveredExpression(item, model);
                return hovered;
            }
        }
        this.notifyHoveredExpression(null, model);
        return null;
    }

    @Override
    public DiagramInteractor getHoveredInteractor(){
        Coordinate model = context.getDiagramStatus().getModelCoordinate(mouseCurrent);
        Collection<DiagramInteractor> hoveredItems = interactorsManager.getHovered(model);
        DiagramInteractor rtn = null;
        for (DiagramInteractor item : hoveredItems) {
            if(item.isVisible()) {
                if (item instanceof InteractorEntity) { //Preference to nodes
                    return item;
                } else if (rtn == null) {
                    rtn = item; //In case there aren't nodes hovered, the "first" hovered link is returned
                }
            }
        }
        return rtn;
    }

    //Before notifying is good practise to check whether there is expression overlay or not
    private void notifyHoveredExpression(DiagramObject item, Coordinate model) {
        if (context.getAnalysisStatus() != null) {
            AnalysisType type = context.getAnalysisStatus().getAnalysisType();
            if (type.equals(AnalysisType.EXPRESSION)) {
                //The reason why the notification is delegated to the canvas is because it keeps track of the
                //expression changes already, so this do not need to be done here.
                this.canvas.notifyHoveredExpression(item, model);
            }
        }
    }

    private void resetContext() {
        this.canvas.clear();
        this.canvas.clearThumbnail();
        if (this.context != null) {
            //this.resetAnalysis(); !IMPORTANT! Do not use this method here
            //Once a context is due to be replaced, the analysis overlay has to be cleaned up
            clearAnalysisOverlay();
            this.context = null;
        }
        GraphObjectFactory.content = null;
    }

    private void setContext(final Context context) {
        this.resetContext();

        this.context = context;
        GraphObjectFactory.content = context.getContent();

        layoutManager.resetHovered();

        this.forceDraw = true;
        if (this.context.getContent().isGraphLoaded()) {
            this.loadAnalysis(this.analysisStatus); //IMPORTANT: This needs to be done once context is been set up above
            this.eventBus.fireEventFromSource(new ContentLoadedEvent(context), this);
        }
        this.context.restoreDialogs();
    }

    @Override
    public void setSelection(boolean zoom, boolean fireExternally) {
        DiagramInteractor interactor = getHoveredInteractor();
        if (interactor != null) {
            eventBus.fireEventFromSource(new InteractorSelectedEvent(interactor.getUrl()), this);
        } else {
            setSelection(this.getHoveredDiagramObject(), zoom, fireExternally, true);
        }
    }


    private boolean setSelection(HoveredItem hoveredItem, boolean zoom, boolean fireExternally, boolean notify) {
        GraphObject toSelect = hoveredItem != null ? hoveredItem.getGraphObject() : null;
        if (toSelect != null) {
            if (hoveredItem.getAttachment() != null) {
                eventBus.fireEventFromSource(new EntityDecoratorSelectedEvent(toSelect, hoveredItem.getAttachment()), this);
            }
            if (hoveredItem.getSummaryItem() != null) {
                SummaryItem summaryItem = hoveredItem.getSummaryItem();
                if(summaryItem.getType().equals("TR")){
                    forceDraw |= interactorsManager.update(summaryItem, (Node) hoveredItem.getHoveredObject());
                }
                eventBus.fireEventFromSource(new EntityDecoratorSelectedEvent(toSelect, hoveredItem.getSummaryItem()), this);
            }
            if (hoveredItem.getContextMenuTrigger() != null) {
                eventBus.fireEventFromSource(new EntityDecoratorSelectedEvent(toSelect, hoveredItem.getContextMenuTrigger()), this);
                DiagramObject item = layoutManager.getHoveredDiagramObject();
                context.showDialog(this.eventBus, item, this.canvas);
            }
        }
        return makeSelection(toSelect, zoom, fireExternally, true);
//        if (!layoutManager.isSelected(toSelect)) {
//            //this.resetIllustration(); //TODO this has been moved to a higher layer
//            this.eventBus.fireEventFromSource(new GraphObjectSelectedEvent(toSelect, zoom, fireExternally), this);
//        }
    }

    private boolean makeSelection(GraphObject toSelect, boolean zoom, boolean fireExternally, boolean notify){
        boolean rtn = false;
        if (!layoutManager.isSelected(toSelect)) {
            layoutManager.setSelected(toSelect);
            rtn = true;
            if (zoom) {
                diagramManager.displayDiagramObjects(layoutManager.getHalo());
            }
            forceDraw = true;
            if (notify) {
                eventBus.fireEventFromSource(new GraphObjectSelectedEvent(toSelect, zoom, fireExternally), this);
            }
        }
        return rtn;
    }

    @Override
    public void fitDiagram(boolean animation) {
//        if(context.getContent().getType() == DIAGRAM) { //TODO to be moved to Viewer
            diagramManager.fitDiagram(context.getContent(), animation);
//        }
    }

    private void overview() {
        fireEvent(new FireworksOpenedEvent(context.getContent().getDbId()));
    }

    private void padding(int dX, int dY) {
//        if(context.getContent().getType() == DIAGRAM) { //TODO to be moved to Viewer
            padding(CoordinateFactory.get(dX, dY));
//        }
    }

    @Override
    public void padding(Coordinate delta) {
        context.getDiagramStatus().padding(delta);
        forceDraw = true;
        Box visibleArea = context.getVisibleModelArea(viewportWidth, viewportHeight);
        eventBus.fireEventFromSource(new DiagramPanningEvent(visibleArea), this);
    }

    @Override
    public void zoomDelta(double deltaFactor) {
//        if(context.getContent().getType() == DIAGRAM) {  //TODO to be moved to Viewer
            zoom(context.getDiagramStatus().getFactor() + deltaFactor);
//        }
    }

    private void zoom(double factor) {
        Coordinate viewportCentre = CoordinateFactory.get(viewportWidth / 2, viewportHeight / 2);
        factor = ViewportUtils.checkFactor(factor);
        zoom(factor, viewportCentre);
    }

    @Override
    public void mouseZoom(double delta){
        if (context == null) return;
        double factor = context.getDiagramStatus().getFactor();
        factor = ViewportUtils.checkFactor(factor  - delta);
        zoom(factor, mouseCurrent);
    }

    @Override
    public void dragInteractor(InteractorEntity interactor, Coordinate delta) {
        delta = delta.divide(context.getDiagramStatus().getFactor());
        interactorsManager.drag(interactor, delta.getX(), delta.getY());
        Box visibleArea = context.getVisibleModelArea(viewportWidth, viewportHeight);
        drawInteractors(visibleArea);
    }

    private void zoom(double factor, Coordinate mouse) {
        DiagramStatus status = context.getDiagramStatus();
        if (factor == status.getFactor()) return;

        //current and new model positions are used to calculate the delta in order to perform
        //padding to the result of the zooming
        Coordinate current_model = status.getModelCoordinate(mouse);
        status.setFactor(factor);
        Coordinate new_model = status.getModelCoordinate(mouse);

        //the calculated delta also needs to be scaled to the factor and then applied to the status
        Coordinate delta = new_model.minus(current_model).multiply(factor);
        status.padding(delta);

        Box visibleArea = context.getVisibleModelArea(viewportWidth, viewportHeight);
        eventBus.fireEventFromSource(new DiagramZoomEvent(factor, visibleArea), this);
        forceDraw = true;  //IMPORTANT: Please leave it at the very end after the event firing
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    @Override
    public GraphObject getSelected() {
        return layoutManager.getSelected();
    }
}
