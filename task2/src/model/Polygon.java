package model;

import control.FillMode;
import java.util.ArrayList;
import java.util.List;

public class Polygon {
    private List<Point> vertices;
    private boolean closed;
    private boolean selected;
    private boolean filled;
    private FillMode fillMode;

    private List<Polygon> holes;
    private boolean isHole;


    public Polygon() {
        this.vertices = new ArrayList<>();
        this.closed = false;
        this.selected = false;
        this.filled = false;
        this.fillMode = FillMode.SEED_FILL;
        this.holes = new ArrayList<>();
        this.isHole = false;
    }

    public void addVertex(Point p) {
        vertices.add(p);
    }

    public void clear() {
        vertices.clear();
        closed = false;
        selected = false;
        filled = false;
        fillMode = FillMode.SEED_FILL;
        holes.clear();
        isHole = false;
    }

    public List<Point> getVertices() {
        return vertices;
    }

    public List<Polygon> getHoles() {
        return holes;
    }

    public void addHole(Polygon hole) {
        if (!hole.isClosed()) {
            throw new IllegalArgumentException("Hole must be a closed polygon.");
        }
        if (!hole.isHole()) {
            hole.setHole(true);
        }

        if (!isPointInsidePolygon(hole.getVertices().get(0))) {
            throw new IllegalArgumentException("Hole must be inside the polygon.");
        }
        holes.add(hole);
    }

    public boolean isPointInsidePolygon(Point point) {
        boolean inside = false;
        int n = vertices.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            Point vi = vertices.get(i);
            Point vj = vertices.get(j);

            boolean intersect = ((vi.y > point.y) != (vj.y > point.y)) &&
                    (point.x < (vj.x - vi.x) * (point.y - vi.y) / (double) (vj.y - vi.y) + vi.x);
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }
/*
    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
*/
    public boolean isFilled() {
        return filled;
    }

    public void setFilled(boolean filled) {
        this.filled = filled;
    }

    public FillMode getFillMode() {
        return fillMode;
    }

    public void setFillMode(FillMode fillMode) {
        this.fillMode = fillMode;
    }

    public boolean isHole() {
        return isHole;
    }

    public void setHole(boolean isHole) {
        this.isHole = isHole;
    }
}
