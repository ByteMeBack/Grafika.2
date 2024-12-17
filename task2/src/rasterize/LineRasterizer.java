package rasterize;

import java.awt.*;

public abstract class LineRasterizer {
    protected Raster raster;
    protected Color color = Color.WHITE;

    public LineRasterizer(Raster raster) {
        this.raster = raster;
    }
/*
    public void setColor(Color color) {
        this.color = color;
    }
*/
    public void setColor(int color) {
        this.color = new Color(color);
    }
/*
    public void rasterize(Line line) {
        rasterize(line.getX1(), line.getY1(), line.getX2(), line.getY2(), line.getColor());
    }
*/
    public void rasterize(int x1, int y1, int x2, int y2, int color) {
        setColor(color);
        drawLine(x1, y1, x2, y2);
    }

    protected abstract void drawLine(int x1, int y1, int x2, int y2);
}
