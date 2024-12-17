package control;

import fill.SeedFill;
import fill.ScanLine;
import model.Line;
import model.Point;
import model.Polygon;
import model.RegularPentagon;
import rasterize.LineRasterizerGraphics;
import rasterize.Raster;
import view.Panel;

import javax.swing.*;
import java.awt.event.*;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

public class Controller2D implements Controller {

    private final Panel panel;
    private final List<Polygon> completedPolygons = new ArrayList<>();
    private Polygon currentPolygon = new Polygon();
    private LineRasterizerGraphics rasterizer;

    private boolean drawing = false;
    private int mouseX, mouseY;

    private static final int BOUNDARY_COLOR = 0xFFFFFF;
    private static final int FILL_COLOR = Color.GREEN.getRGB();

    private enum DrawMode {
        FREEFORM,
        REGULAR_PENTAGON,
        EDIT //tak to asi nedodělám
    }

    private DrawMode currentDrawMode = DrawMode.FREEFORM;

    private Point pentagonCenter = null;
    private RegularPentagon previewPentagon = null;

    private boolean isClippingMode = false;
    private Polygon subjectPolygon = null;
    private Polygon clipperPolygon = null;

    private boolean isFillMode = false;

    public Controller2D(Panel panel) {
        this.panel = panel;
        initObjects(panel.getRaster());
        initListeners(panel);
    }

    public void initObjects(Raster raster) {
        rasterizer = new LineRasterizerGraphics(raster);
    }

    @Override
    public void initListeners(Panel panel) {
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (isFillMode) {
                        handleFill(e.getX(), e.getY());
                    } else {
                        if (currentDrawMode == DrawMode.FREEFORM) {
                            if (!isClippingMode) {
                                if (e.isShiftDown() && !currentPolygon.getVertices().isEmpty()) {
                                    Point last = currentPolygon.getVertices().get(currentPolygon.getVertices().size() - 1);
                                    int[] aligned = getAlignedPoint(last.x, last.y, e.getX(), e.getY());
                                    currentPolygon.addVertex(new Point(aligned[0], aligned[1]));
                                } else {
                                    currentPolygon.addVertex(new Point(e.getX(), e.getY()));
                                }
                                drawing = true;
                                panel.repaint();
                            } else {
                                if (subjectPolygon == null) {
                                    subjectPolygon = findPolygonAt(e.getX(), e.getY());
                                    if (subjectPolygon != null) {
                                        System.out.println("Subject polygon selected.");
                                    } else {
                                        System.out.println("No polygon found at the clicked point.");
                                    }
                                } else if (clipperPolygon == null) {
                                    clipperPolygon = findPolygonAt(e.getX(), e.getY());
                                    if (clipperPolygon != null) {
                                        System.out.println("Clipper polygon selected.");
                                        performClipping();
                                    } else {
                                        System.out.println("No polygon found at the clicked point.");
                                    }
                                }
                            }
                        } else if (currentDrawMode == DrawMode.REGULAR_PENTAGON) {
                            if (pentagonCenter == null) {
                                pentagonCenter = new Point(e.getX(), e.getY());
                                System.out.println("Pentagon center set at (" + pentagonCenter.x + ", " + pentagonCenter.y + "). Move mouse to set radius and click to finalize.");
                            } else {
                                int radius = (int) Math.hypot(e.getX() - pentagonCenter.x, e.getY() - pentagonCenter.y);
                                if (radius > 0) {
                                    RegularPentagon pentagon = new RegularPentagon(pentagonCenter, radius);
                                    performClippingWithPentagon(pentagon);
                                    System.out.println("Regular pentagon drawn and used as clipper with center (" + pentagonCenter.x + ", " + pentagonCenter.y + ") and radius " + radius + ".");
                                } else {
                                    System.out.println("Invalid radius. Pentagon not drawn.");
                                }
                                pentagonCenter = null;
                                previewPentagon = null;
                                redrawScene();
                            }
                        }
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    if (!isFillMode) {
                        handleFill(e.getX(), e.getY());
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                drawing = false;
            }
        });

        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {

                boolean needRedraw = false;

                if (currentDrawMode == DrawMode.REGULAR_PENTAGON && pentagonCenter != null) {
                    int radius = (int) Math.hypot(e.getX() - pentagonCenter.x, e.getY() - pentagonCenter.y);
                    if (radius > 0) {
                        previewPentagon = new RegularPentagon(pentagonCenter, radius);
                        needRedraw = true;
                    }
                }

                if (currentDrawMode == DrawMode.FREEFORM && !currentPolygon.getVertices().isEmpty() && !isClippingMode) {
                    if (e.isShiftDown()) {
                        Point last = currentPolygon.getVertices().get(currentPolygon.getVertices().size() - 1);
                        int[] aligned = getAlignedPoint(last.x, last.y, e.getX(), e.getY());
                        mouseX = aligned[0];
                        mouseY = aligned[1];
                    } else {
                        mouseX = e.getX();
                        mouseY = e.getY();
                    }
                    needRedraw = true;
                }

                if (needRedraw) {
                    redrawScene();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (currentDrawMode == DrawMode.FREEFORM && drawing && !isClippingMode) {
                    if (e.isShiftDown() && !currentPolygon.getVertices().isEmpty()) {
                        Point last = currentPolygon.getVertices().get(currentPolygon.getVertices().size() - 1);
                        int[] aligned = getAlignedPoint(last.x, last.y, e.getX(), e.getY());
                        mouseX = aligned[0];
                        mouseY = aligned[1];
                    } else {
                        mouseX = e.getX();
                        mouseY = e.getY();
                    }
                    redrawScene();
                    drawDynamicLines(mouseX, mouseY);
                }
            }
        });

        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    finalizePolygon();
                } else if (e.getKeyCode() == KeyEvent.VK_C) {
                    clearPolygons();
                } else if (e.getKeyCode() == KeyEvent.VK_F) {
                    toggleFillMode();
                } else if (e.getKeyCode() == KeyEvent.VK_P) {
                    toggleDrawMode();
                } else if (e.getKeyCode() == KeyEvent.VK_O) {
                    toggleClippingMode();
                }
            }

            private void finalizePolygon() {
                if (currentDrawMode == DrawMode.FREEFORM && currentPolygon.getVertices().size() > 2 && !currentPolygon.isClosed()) {
                    currentPolygon.setClosed(true);
                    completedPolygons.add(currentPolygon);
                    currentPolygon = new Polygon();
                    drawing = false;
                    redrawScene();
                }
            }

            private void clearPolygons() {
                panel.clear();
                completedPolygons.clear();
                currentPolygon.clear();
                drawing = false;
                pentagonCenter = null;
                previewPentagon = null;
                redrawScene();
            }

            private void toggleFillMode() {
                isFillMode = !isFillMode;
                if (isFillMode) {
                    System.out.println("Fill mode activated. Click on a polygon to fill.");
                } else {
                    System.out.println("Fill mode deactivated.");
                }
            }

            private void toggleDrawMode() {
                if (currentDrawMode == DrawMode.FREEFORM) {
                    currentDrawMode = DrawMode.REGULAR_PENTAGON;
                    pentagonCenter = null;
                    previewPentagon = null;
                    System.out.println("Draw mode set to Regular Pentagon");
                } else if (currentDrawMode == DrawMode.REGULAR_PENTAGON) {
                    currentDrawMode = DrawMode.EDIT;
                    System.out.println("Draw mode set to Edit Polygon");
                } else {
                    currentDrawMode = DrawMode.FREEFORM;
                    pentagonCenter = null;
                    previewPentagon = null;
                    System.out.println("Draw mode set to Freeform Polygon");
                }
            }

            private void toggleClippingMode() {
                isClippingMode = !isClippingMode;
                if (isClippingMode) {
                    System.out.println("Clipping mode activated. Select subject polygon.");
                    subjectPolygon = null;
                    clipperPolygon = null;
                } else {
                    System.out.println("Clipping mode deactivated.");
                    subjectPolygon = null;
                    clipperPolygon = null;
                }
            }
        });

        panel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                panel.resize();
                initObjects(panel.getRaster());
                redrawScene();
            }
        });

        panel.setFocusable(true);
        panel.requestFocusInWindow();
    }

    private void handleFill(int x, int y) {

        Polygon targetPolygon = findPolygonAt(x, y);
        if (targetPolygon != null && targetPolygon.isClosed()) {
            Raster raster = panel.getRaster();

            if (targetPolygon.getFillMode() == FillMode.SEED_FILL) {

                Point seed = computeSeedPoint(targetPolygon);
                SeedFill seedFill = new SeedFill(raster, seed.x, seed.y, FILL_COLOR, BOUNDARY_COLOR);
                seedFill.fill();
                System.out.println("Seed Fill applied to polygon.");
                targetPolygon.setFilled(true);
                targetPolygon.setFillMode(FillMode.SEED_FILL);
            } else if (targetPolygon.getFillMode() == FillMode.SCAN_LINE) {

                ScanLine scanLineFill = new ScanLine(raster, targetPolygon, FILL_COLOR, BOUNDARY_COLOR);
                scanLineFill.fill();
                System.out.println("Scan Line Fill applied to polygon.");
                targetPolygon.setFilled(true);
                targetPolygon.setFillMode(FillMode.SCAN_LINE);
            }

            panel.repaint();
        } else {
            System.out.println("No closed polygon found at the clicked point.");
        }
    }

    private Polygon findPolygonAt(int x, int y) {
        for (Polygon polygon : completedPolygons) {
            if (isPointInsidePolygon(x, y, polygon)) {
                return polygon;
            }
        }
        return null;
    }

    private boolean isPointInsidePolygon(int x, int y, Polygon polygon) {
        boolean inside = false;
        List<Point> vertices = polygon.getVertices();
        int n = vertices.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            int xi = vertices.get(i).x, yi = vertices.get(i).y;
            int xj = vertices.get(j).x, yj = vertices.get(j).y;

            boolean intersect = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (double) (yj - yi) + xi);
            if (intersect) {
                inside = !inside;
            }
        }
        System.out.println("Point (" + x + ", " + y + ") inside polygon: " + inside);
        return inside;
    }

    private Point computeSeedPoint(Polygon polygon) {
        double centroidX = 0, centroidY = 0;
        double signedArea = 0;
        double x0, y0, x1, y1;
        int n = polygon.getVertices().size();
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            x0 = polygon.getVertices().get(i).x;
            y0 = polygon.getVertices().get(i).y;
            x1 = polygon.getVertices().get(j).x;
            y1 = polygon.getVertices().get(j).y;
            double a = x0 * y1 - x1 * y0;
            signedArea += a;
            centroidX += (x0 + x1) * a;
            centroidY += (y0 + y1) * a;
        }
        signedArea *= 0.5;
        centroidX /= (6 * signedArea);
        centroidY /= (6 * signedArea);
        return new Point((int) centroidX, (int) centroidY);
    }

    private int[] getAlignedPoint(int x1, int y1, int x2, int y2) {
        int dx = x2 - x1;
        int dy = y2 - y1;

        if (dx == 0 && dy == 0) {
            return new int[]{x2, y2};
        }

        double angle = Math.toDegrees(Math.atan2(dy, dx));
        angle = (angle + 360) % 360;

        double[] directions = {0, 45, 90, 135, 180, 225, 270, 315};
        double closestAngle = 0;
        double minDifference = 360;

        for (double dir : directions) {
            double difference = Math.abs(angle - dir);
            difference = Math.min(difference, 360 - difference);
            if (difference < minDifference) {
                minDifference = difference;
                closestAngle = dir;
            }
        }

        int offset = (int) Math.min(Math.abs(dx), Math.abs(dy));

        switch ((int) Math.round(closestAngle)) {
            case 0:
            case 180:
                return new int[]{x2, y1};
            case 45:
            case 225:
                return new int[]{
                        x1 + (int) Math.signum(dx) * offset,
                        y1 + (int) Math.signum(dy) * offset
                };
            case 90:
            case 270:
                return new int[]{x1, y2};
            case 135:
                return new int[]{
                        x1 - offset,
                        y1 + offset
                };
            case 315:
                return new int[]{
                        x1 + offset,
                        y1 - offset
                };
            default:
                break;
        }

        return new int[]{x2, y2};
    }

    private void performClippingWithPentagon(RegularPentagon pentagon) {
        Area pentagonArea = polygonToArea(pentagon);

        List<Polygon> polygonsToRemove = new ArrayList<>();
        List<Polygon> polygonsToAdd = new ArrayList<>();

        for (Polygon polygon : new ArrayList<>(completedPolygons)) {
            Area polygonArea = polygonToArea(polygon);
            Area intersection = new Area(pentagonArea);
            intersection.intersect(polygonArea);

            if (!intersection.isEmpty()) {
                if (isPentagonInsideAndNotTouching(pentagon, polygon)) {
                    System.out.println("Pentagon is completely inside the polygon. Adding as a hole.");

                    clearInsidePentagon(pentagon, panel.getRaster());

                    Polygon hole = new Polygon();
                    hole.getVertices().addAll(pentagon.getVertices());
                    hole.setClosed(true);
                    hole.setHole(true);

                    polygon.addHole(hole);
                }
                else {

                    System.out.println("Performing regular clipping with pentagon.");
                    polygonArea.subtract(pentagonArea);

                    List<Polygon> clippedPolygons = areaToPolygons(polygonArea, polygon.isFilled(), polygon.getFillMode());

                    for (Polygon clippedPolygon : clippedPolygons) {
                        clippedPolygon.getHoles().addAll(polygon.getHoles());
                    }

                    polygonsToRemove.add(polygon);
                    polygonsToAdd.addAll(clippedPolygons);
                }
            }
        }

        completedPolygons.removeAll(polygonsToRemove);
        completedPolygons.addAll(polygonsToAdd);

        redrawScene();
    }

    private boolean isPentagonInsideAndNotTouching(RegularPentagon pentagon, Polygon polygon) {
        Area polygonArea = polygonToArea(polygon);
        Area pentagonArea = polygonToArea(pentagon);

        if (!polygonArea.contains(pentagonArea.getBounds2D())) {
            return false;
        }

        for (Point vertex : pentagon.getVertices()) {
            if (isPointOnPolygonBoundary(vertex, polygon)) {
                return false;
            }
        }

        return true;
    }

    private boolean isPointOnPolygonBoundary(Point point, Polygon polygon) {
        List<Point> vertices = polygon.getVertices();
        for (int i = 0; i < vertices.size(); i++) {
            Point p1 = vertices.get(i);
            Point p2 = vertices.get((i + 1) % vertices.size());
            if (isPointOnLineSegment(point, p1, p2)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPointOnLineSegment(Point p, Point a, Point b) {
        return Math.abs((p.x - a.x) * (b.y - a.y) - (p.y - a.y) * (b.x - a.x)) < 1e-6 &&
                p.x >= Math.min(a.x, b.x) && p.x <= Math.max(a.x, b.x) &&
                p.y >= Math.min(a.y, b.y) && p.y <= Math.max(a.y, b.y);
    }

    private void clearInsidePentagon(Polygon pentagon, Raster raster) {
        Point seed = computeSeedPoint(pentagon);

        SeedFill seedFill = new SeedFill(raster, seed.x, seed.y, 0x000000, BOUNDARY_COLOR);
        seedFill.fill();

        drawPolygonOutline(pentagon, Color.BLACK);
    }

    private void drawPolygonOutline(Polygon polygon, Color color) {
        List<Point> vertices = polygon.getVertices();
        rasterizer.setColor(color);

        for (int i = 0; i < vertices.size() - 1; i++) {
            Point p1 = vertices.get(i);
            Point p2 = vertices.get(i + 1);
            rasterizer.rasterize(new Line(p1.x, p1.y, p2.x, p2.y, color.getRGB()));
        }

        if (polygon.isClosed() && vertices.size() > 2) {
            Point first = vertices.get(0);
            Point last = vertices.get(vertices.size() - 1);
            rasterizer.rasterize(new Line(last.x, last.y, first.x, first.y, color.getRGB()));
        }

        if (polygon.isHole()) {
            rasterizer.setColor(Color.BLACK);
            for (int i = 0; i < vertices.size() - 1; i++) {
                Point p1 = vertices.get(i);
                Point p2 = vertices.get(i + 1);
                rasterizer.rasterize(new Line(p1.x, p1.y, p2.x, p2.y, Color.BLACK.getRGB()));
            }
            if (polygon.isClosed() && vertices.size() > 2) {
                Point first = vertices.get(0);
                Point last = vertices.get(vertices.size() - 1);
                rasterizer.rasterize(new Line(last.x, last.y, first.x, first.y, Color.BLACK.getRGB()));
            }
        }
    }

    private void clearInsidePentagon(Area area) {
        PathIterator iterator = area.getPathIterator(null);
        double[] coords = new double[6];

        while (!iterator.isDone()) {
            int type = iterator.currentSegment(coords);

            if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
                int x = (int) coords[0];
                int y = (int) coords[1];

                if (x >= 0 && y >= 0 && x < panel.getRaster().getWidth() && y < panel.getRaster().getHeight()) {
                    panel.getRaster().setPixel(x, y, 0x000000);
                }
            }
            iterator.next();
        }
    }

    private Area polygonToArea(Polygon polygon) {
        java.awt.Polygon awtPolygon = new java.awt.Polygon();
        for (Point p : polygon.getVertices()) {
            awtPolygon.addPoint(p.x, p.y);
        }

        Area area = new Area(awtPolygon);

        for (Polygon hole : polygon.getHoles()) {
            java.awt.Polygon awtHole = new java.awt.Polygon();
            for (Point p : hole.getVertices()) {
                awtHole.addPoint(p.x, p.y);
            }
            area.subtract(new Area(awtHole));
        }

        return area;
    }

    private Area polygonToOutlineArea(Polygon polygon) {
        Area filledArea = polygonToArea(polygon);
        BasicStroke stroke = new BasicStroke(1);
        return new Area(stroke.createStrokedShape(filledArea));
    }

    private List<Polygon> areaToPolygons(Area area, boolean filled, FillMode fillMode) {
        List<Polygon> polygons = new ArrayList<>();
        PathIterator iterator = area.getPathIterator(null);
        double[] coords = new double[6];
        List<Point> points = new ArrayList<>();
        Polygon currentPolygon = null;

        while (!iterator.isDone()) {
            int type = iterator.currentSegment(coords);

            if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {

                if (currentPolygon == null) {
                    currentPolygon = new Polygon();
                }
                points.add(new Point((int) coords[0], (int) coords[1]));
            } else if (type == PathIterator.SEG_CLOSE) {

                if (currentPolygon != null) {
                    currentPolygon.getVertices().addAll(points);
                    currentPolygon.setClosed(true);
                    currentPolygon.setFilled(filled);
                    currentPolygon.setFillMode(fillMode);
                    polygons.add(currentPolygon);
                    points.clear();
                    currentPolygon = null;
                }
            }

            iterator.next();
        }

        return polygons;
    }

    private void performClipping() {
        if (subjectPolygon != null && clipperPolygon != null) {
            Area subjectArea = polygonToArea(subjectPolygon);
            Area clipperArea = polygonToArea(clipperPolygon);

            subjectArea.subtract(clipperArea);

            List<Polygon> clippedPolygons = areaToPolygons(subjectArea, subjectPolygon.isFilled(), subjectPolygon.getFillMode());

            if (!clippedPolygons.isEmpty()) {
                System.out.println("Clipping performed. Added " + clippedPolygons.size() + " polygons.");

                completedPolygons.remove(subjectPolygon);

                completedPolygons.addAll(clippedPolygons);
            } else {
                System.out.println("Clipping resulted in an empty polygon. Removing subject polygon.");
                completedPolygons.remove(subjectPolygon);
            }

            subjectPolygon = null;
            clipperPolygon = null;

            redrawScene();
        }
    }

    private void redrawScene() {
        panel.clear();

        for (Polygon polygon : completedPolygons) {
            drawPolygonOutline(polygon, Color.WHITE);

            for (Polygon hole : polygon.getHoles()) {
                drawPolygonOutline(hole, Color.WHITE);
            }
        }

        for (Polygon polygon : completedPolygons) {
            if (polygon.isFilled()) {
                fillPolygonWithHoles(polygon);
            }
        }

        drawPolygonOutline(currentPolygon, Color.WHITE);

        if (previewPentagon != null) {
            drawPolygonOutline(previewPentagon, Color.WHITE);
        }

        if (currentDrawMode == DrawMode.FREEFORM && !currentPolygon.getVertices().isEmpty() && !isClippingMode) {
            drawDynamicLines(mouseX, mouseY);
        }

        panel.repaint();
    }

    private void fillPolygonWithHoles(Polygon polygon) {
        Raster raster = panel.getRaster();

        Area area = polygonToArea(polygon);

        for (int y = 0; y < raster.getHeight(); y++) {
            for (int x = 0; x < raster.getWidth(); x++) {
                if (area.contains(x, y)) {

                    raster.setPixel(x, y, FILL_COLOR);
                }
            }
        }
    }

    private void drawPolygon(Polygon polygon) {

        List<Point> vertices = polygon.getVertices();
        Color color = Color.WHITE;

        for (int i = 0; i < vertices.size() - 1; i++) {
            Point p1 = vertices.get(i);
            Point p2 = vertices.get(i + 1);
            rasterizer.setColor(color);
            rasterizer.rasterize(new Line(p1.x, p1.y, p2.x, p2.y, color.getRGB()));
        }

        if (polygon.isClosed() && vertices.size() > 2) {
            Point first = vertices.get(0);
            Point last = vertices.get(vertices.size() - 1);
            rasterizer.setColor(color);
            rasterizer.rasterize(new Line(last.x, last.y, first.x, first.y, color.getRGB()));
        }

        if (polygon.isFilled()) {
            Raster raster = panel.getRaster();
            Point seed = computeSeedPoint(polygon);
            SeedFill seedFill = new SeedFill(raster, seed.x, seed.y, FILL_COLOR, BOUNDARY_COLOR);
            seedFill.fill();
        }

        for (Polygon hole : polygon.getHoles()) {
            drawPolygon(hole);
        }
    }

    private void drawDynamicLines(int mouseX, int mouseY) {
        if (currentDrawMode != DrawMode.FREEFORM || currentPolygon.getVertices().isEmpty()) return;

        Point last = currentPolygon.getVertices().get(currentPolygon.getVertices().size() - 1);
        rasterizer.setColor(Color.GREEN);
        rasterizer.rasterize(new Line(last.x, last.y, mouseX, mouseY, Color.GREEN.getRGB()));

        if (currentPolygon.getVertices().size() > 1) {
            Point first = currentPolygon.getVertices().get(0);
            rasterizer.rasterize(new Line(first.x, first.y, mouseX, mouseY, Color.GREEN.getRGB()));
        }
        rasterizer.setColor(Color.WHITE);
    }
}
