package fill;

import model.Point;
import model.Polygon;
import rasterize.Raster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScanLine implements Filler {

    private final Raster raster;
    private final Polygon polygon;
    private final int fillColor;
    private final int boundaryColor;

    public ScanLine(Raster raster, Polygon polygon, int fillColor, int boundaryColor) {
        this.raster = raster;
        this.polygon = polygon;
        this.fillColor = fillColor;
        this.boundaryColor = boundaryColor;
    }

    @Override
    public void fill() {

        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Point p : polygon.getVertices()) {
            if (p.y < minY) minY = p.y;
            if (p.y > maxY) maxY = p.y;
        }

        for (int y = minY; y <= maxY; y++) {
            List<Integer> intersections = new ArrayList<>();

            List<Point> vertices = polygon.getVertices();
            int n = vertices.size();
            for (int i = 0; i < n; i++) {
                Point p1 = vertices.get(i);
                Point p2 = vertices.get((i + 1) % n);

                if (p1.y == p2.y) {
                    continue;
                }

                if ((y >= p1.y && y < p2.y) || (y >= p2.y && y < p1.y)) {
                    double xIntersect = p1.x + (double) (y - p1.y) * (p2.x - p1.x) / (p2.y - p1.y);
                    intersections.add((int) Math.round(xIntersect));
                }
            }

            Collections.sort(intersections);

            for (int i = 0; i < intersections.size(); i += 2) {
                if (i + 1 < intersections.size()) {
                    int startX = intersections.get(i);
                    int endX = intersections.get(i + 1);
                    for (int x = startX; x <= endX; x++) {
                        if (raster.getPixel(x, y) != boundaryColor) {
                            raster.setPixel(x, y, fillColor);
                        }
                    }
                }
            }
        }
    }
}
