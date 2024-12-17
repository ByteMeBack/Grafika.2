package model;

public class RegularPentagon extends Polygon {
    public RegularPentagon(Point center, int radius) {
        super();

        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(90 + i * 72);
            int x = center.x + (int) (radius * Math.cos(angle));
            int y = center.y - (int) (radius * Math.sin(angle));
            addVertex(new Point(x, y));
        }
        setClosed(true);
    }
}
