package rasterize;

import model.Line;

import java.awt.Color;

public class LineRasterizerGraphics {

    private Raster raster;
    private Color currentColor = Color.WHITE;

    public LineRasterizerGraphics(Raster raster) {
        this.raster = raster;
    }

    public void rasterize(Line line) {

        int x0 = line.getX1();
        int y0 = line.getY1();
        int x1 = line.getX2();
        int y1 = line.getY2();

        drawLine(x0, y0, x1, y1, currentColor.getRGB());
    }

    public void setColor(Color color) {
        this.currentColor = color;
    }

    public Color getColor() {
        return this.currentColor;
    }

    private void drawLine(int x0, int y0, int x1, int y1, int color) {

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            raster.setPixel(x0, y0, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }
}
