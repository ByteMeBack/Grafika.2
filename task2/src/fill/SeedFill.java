package fill;

import rasterize.Raster;

import java.util.LinkedList;
import java.util.Queue;

public class SeedFill implements Filler {

    private final Raster raster;
    private final int startX;
    private final int startY;
    private final int fillColor;
    private final int boundaryColor;

    public SeedFill(Raster raster, int startX, int startY, int fillColor, int boundaryColor) {
        this.raster = raster;
        this.startX = startX;
        this.startY = startY;
        this.fillColor = fillColor;
        this.boundaryColor = boundaryColor;
    }

    @Override
    public void fill() {
        int width = raster.getWidth();
        int height = raster.getHeight();
        if (startX < 0 || startX >= width || startY < 0 || startY >= height) {
            return;
        }

        int targetColor = raster.getPixel(startX, startY);
        if (targetColor == fillColor || targetColor == boundaryColor) {
            return;
        }

        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(startX, startY));

        while (!queue.isEmpty()) {
            Point p = queue.remove();
            int x = p.x;
            int y = p.y;

            if (x < 0 || x >= width || y < 0 || y >= height) {
                continue;
            }

            int currentColor = raster.getPixel(x, y);
            if (currentColor == targetColor) {
                raster.setPixel(x, y, fillColor);

                queue.add(new Point(x + 1, y));
                queue.add(new Point(x - 1, y));
                queue.add(new Point(x, y + 1));
                queue.add(new Point(x, y - 1));
                // queue.add(new Point(x + 1, y + 1));
                // queue.add(new Point(x - 1, y - 1));
                // queue.add(new Point(x + 1, y - 1));
                // queue.add(new Point(x - 1, y + 1));
            }
        }
    }

    private static class Point {
        int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
