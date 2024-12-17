package rasterize;

import model.Point;
import model.Polygon;

import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

public class PolygonClipper {

    public Polygon clip(Polygon subject, Polygon clipper) {

        Area subjectArea = polygonWithHolesToArea(subject);
        Area clipperArea = polygonToArea(clipper);

        subjectArea.subtract(clipperArea);

        return areaToPolygon(subjectArea);
    }

    private Area polygonWithHolesToArea(Polygon polygon) {
        Area mainArea = polygonToArea(polygon);

        for (Polygon hole : polygon.getHoles()) {
            mainArea.subtract(polygonToArea(hole));
        }

        return mainArea;
    }

    private Area polygonToArea(Polygon polygon) {
        java.awt.Polygon awtPolygon = new java.awt.Polygon();
        for (Point p : polygon.getVertices()) {
            awtPolygon.addPoint(p.x, p.y);
        }
        return new Area(awtPolygon);
    }

    private Polygon areaToPolygon(Area area) {
        List<Polygon> polygons = new ArrayList<>();
        PathIterator iterator = area.getPathIterator(null);
        double[] coords = new double[6];
        List<Point> points = new ArrayList<>();

        while (!iterator.isDone()) {
            int type = iterator.currentSegment(coords);
            if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
                points.add(new Point((int) coords[0], (int) coords[1]));
            } else if (type == PathIterator.SEG_CLOSE) {
                Polygon polygon = new Polygon();
                polygon.getVertices().addAll(points);
                polygon.setClosed(true);

                if (determineOrientation(points) < 0) {
                    polygon.setHole(true);
                }

                polygons.add(polygon);
                points.clear();
            }
            iterator.next();
        }

        if (polygons.isEmpty()) {
            return new Polygon();
        }

        Polygon mainPolygon = polygons.get(0);
        for (int i = 1; i < polygons.size(); i++) {
            if (polygons.get(i).isHole()) {
                mainPolygon.addHole(polygons.get(i));
            }
        }
        return mainPolygon;
    }

    private int determineOrientation(List<Point> vertices) {
        if (vertices.size() < 3) {
            throw new IllegalArgumentException("Polygon musí mít alespoò 3 vrcholy.");
        }

        double sum = 0;
        int n = vertices.size();

        for (int i = 0; i < n; i++) {
            Point current = vertices.get(i);
            Point next = vertices.get((i + 1) % n);

            sum += (next.x - current.x) * (next.y + current.y);
        }

        return sum > 0 ? 1 : -1;
    }
}
