package dev.donut.client.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

import java.util.Arrays;

/**
 * Float vector geometry with a one-framebuffer-pixel coverage fringe. The regular GUI
 * pipeline interpolates alpha across that fringe, so edges stay smooth at fractional
 * menu scales without textures, shader changes, or one draw submission per pixel.
 */
final class SmoothGeometry {
    private SmoothGeometry() { }

    static void rounded(GuiGraphics g, float x, float y, float w, float h, float radius,
                        float stroke, int color) {
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) return;
        Mesh mesh = new Mesh(g);
        float r = Math.clamp(radius, 0, Math.min(w, h) / 2);
        int segments = segments(r, mesh.pixel);
        float aa = mesh.pixel / 2;
        if (stroke <= 0 || stroke * 2 >= Math.min(w, h)) {
            float inset = Math.min(aa, Math.min(w, h) / 2);
            int covered = opacity(color, Math.min(1, Math.min(w, h) / mesh.pixel));
            float[] inner = rectangle(x, y, w, h, r, inset, segments);
            mesh.fill(inner, x + w / 2, y + h / 2, covered);
            mesh.band(inner, covered, rectangle(x, y, w, h, r, -aa, segments), color & 0xFFFFFF);
        } else {
            float middle = Math.min(stroke / 2, aa);
            int covered = opacity(color, Math.min(1, stroke / mesh.pixel));
            float[] outer = rectangle(x, y, w, h, r, middle, segments);
            float[] inner = rectangle(x, y, w, h, r, stroke - middle, segments);
            mesh.band(outer, covered, rectangle(x, y, w, h, r, -aa, segments), color & 0xFFFFFF);
            mesh.band(inner, covered, outer, covered);
            mesh.band(rectangle(x, y, w, h, r, stroke + aa, segments), color & 0xFFFFFF, inner, covered);
        }
        mesh.submit(g);
    }

    static void shadow(GuiGraphics g, float x, float y, float w, float h, float radius, int color) {
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) return;
        Mesh mesh = new Mesh(g);
        float blur = Math.max(6, radius * 1.5f);
        float r = Math.clamp(radius, 0, Math.min(w, h) / 2);
        float innerInset = Math.min(blur * .45f, Math.min(w, h) * .4f);
        int segments = segments(r + blur, mesh.pixel);
        float[] previous = rectangle(x, y, w, h, r, innerInset, segments);
        mesh.fill(previous, x + w / 2, y + h / 2, color);
        int previousColor = color;
        // Non-overlapping bands avoid dark seams and preserve the requested peak opacity.
        for (int i = 1; i <= 12; i++) {
            float t = i / 12f;
            float fade = 1 - t * t * (3 - 2 * t);
            int nextColor = opacity(color, fade);
            float[] next = rectangle(x, y, w, h, r, innerInset - (innerInset + blur) * t, segments);
            mesh.band(previous, previousColor, next, nextColor);
            previous = next;
            previousColor = nextColor;
        }
        mesh.submit(g);
    }

    static void line(GuiGraphics g, float x1, float y1, float x2, float y2, float width, int color) {
        if (width <= 0 || (color >>> 24) == 0) return;
        Mesh mesh = new Mesh(g);
        float radius = width / 2;
        float aa = mesh.pixel / 2;
        float angle = (float) Math.atan2(y2 - y1, x2 - x1);
        int segments = segments(radius + aa, mesh.pixel) * 2;
        float[] inner = capsule(x1, y1, x2, y2, Math.max(0, radius - aa), angle, segments);
        float[] outer = capsule(x1, y1, x2, y2, radius + aa, angle, segments);
        int covered = opacity(color, Math.min(1, width / mesh.pixel));
        mesh.fill(inner, (x1 + x2) / 2, (y1 + y2) / 2, covered);
        mesh.band(inner, covered, outer, color & 0xFFFFFF);
        mesh.submit(g);
    }

    static void closedStroke(GuiGraphics g, float[] path, float width, int color) {
        if (path.length < 6 || width <= 0 || (color >>> 24) == 0) return;
        Mesh mesh = new Mesh(g);
        float half = width / 2;
        float aa = Math.min(mesh.pixel / 2, half);
        int covered = opacity(color, Math.min(1, width / mesh.pixel));
        float[] outer = offset(path, half - aa);
        float[] inner = offset(path, -half + aa);
        mesh.band(outer, covered, offset(path, half + mesh.pixel / 2), color & 0xFFFFFF);
        mesh.band(inner, covered, outer, covered);
        mesh.band(offset(path, -half - mesh.pixel / 2), color & 0xFFFFFF, inner, covered);
        mesh.submit(g);
    }

    private static int opacity(int color, float coverage) {
        return Math.round((color >>> 24) * coverage) << 24 | color & 0xFFFFFF;
    }

    private static int segments(float radius, float pixel) {
        return Math.clamp((int) Math.ceil(Math.sqrt(radius / pixel) * 1.4), 4, 32);
    }

    /** Clockwise contour, with matching vertex counts for all inset distances. */
    private static float[] rectangle(float x, float y, float w, float h, float radius,
                                     float inset, int segments) {
        inset = Math.min(inset, Math.min(w, h) / 2);
        float left = x + inset, top = y + inset, right = x + w - inset, bottom = y + h - inset;
        float r = Math.max(0, radius - inset);
        float[] points = new float[(segments + 1) * 8];
        int p = 0;
        for (int corner = 0; corner < 4; corner++) {
            float cx = corner == 0 || corner == 1 ? right - r : left + r;
            float cy = corner == 0 || corner == 3 ? top + r : bottom - r;
            for (int i = 0; i <= segments; i++) {
                double angle = (corner - 1 + i / (double) segments) * Math.PI / 2;
                points[p++] = cx + (float) Math.cos(angle) * r;
                points[p++] = cy + (float) Math.sin(angle) * r;
            }
        }
        return points;
    }

    private static float[] capsule(float x1, float y1, float x2, float y2, float radius,
                                   float angle, int segments) {
        float[] points = new float[(segments + 1) * 4];
        int p = 0;
        for (int end = 0; end < 2; end++) {
            float x = end == 0 ? x2 : x1, y = end == 0 ? y2 : y1;
            for (int i = 0; i <= segments; i++) {
                double theta = angle - Math.PI / 2 + Math.PI * (end + i / (double) segments);
                points[p++] = x + (float) Math.cos(theta) * radius;
                points[p++] = y + (float) Math.sin(theta) * radius;
            }
        }
        return points;
    }

    /** Offset a clockwise closed icon path, joining edges without overlapping alpha. */
    private static float[] offset(float[] points, float distance) {
        float[] result = new float[points.length];
        for (int i = 0; i < points.length; i += 2) {
            int previous = (i + points.length - 2) % points.length, next = (i + 2) % points.length;
            float ax = points[i] - points[previous], ay = points[i + 1] - points[previous + 1];
            float bx = points[next] - points[i], by = points[next + 1] - points[i + 1];
            float al = (float) Math.hypot(ax, ay), bl = (float) Math.hypot(bx, by);
            float nx = ay / al + by / bl, ny = -ax / al - bx / bl;
            float factor = distance / Math.max(.25f, nx * by / bl - ny * bx / bl);
            result[i] = points[i] + nx * factor;
            result[i + 1] = points[i + 1] + ny * factor;
        }
        return result;
    }

    private static final class Mesh implements GuiElementRenderState {
        private final Matrix3x2fc pose;
        private final ScreenRectangle scissor;
        private final float pixel;
        private float[] positions = new float[1024];
        private int[] colors = new int[512];
        private int size;
        private float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        private ScreenRectangle bounds;

        private Mesh(GuiGraphics g) {
            pose = new Matrix3x2f(g.pose());
            scissor = g.scissorStack.peek();
            double scaleX = Math.hypot(pose.m00(), pose.m01());
            double scaleY = Math.hypot(pose.m10(), pose.m11());
            double physicalScale = Math.min(scaleX, scaleY) * Minecraft.getInstance().getWindow().getGuiScale();
            pixel = (float) (1.0 / Math.max(.01, physicalScale));
        }

        private void vertex(float x, float y, int color) {
            if (size == colors.length) {
                colors = Arrays.copyOf(colors, colors.length * 2);
                positions = Arrays.copyOf(positions, positions.length * 2);
            }
            positions[size * 2] = x;
            positions[size * 2 + 1] = y;
            colors[size++] = color;
            minX = Math.min(minX, x); minY = Math.min(minY, y);
            maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
        }

        private void fill(float[] path, float cx, float cy, int color) {
            for (int i = 0; i < path.length; i += 2) {
                int next = (i + 2) % path.length;
                // A triangle in the GUI pipeline's quad format; last vertex is repeated.
                vertex(cx, cy, color);
                vertex(path[next], path[next + 1], color);
                vertex(path[i], path[i + 1], color);
                vertex(path[i], path[i + 1], color);
            }
        }

        private void band(float[] inner, int innerColor, float[] outer, int outerColor) {
            for (int i = 0; i < inner.length; i += 2) {
                int next = (i + 2) % inner.length;
                vertex(inner[i], inner[i + 1], innerColor);
                vertex(inner[next], inner[next + 1], innerColor);
                vertex(outer[next], outer[next + 1], outerColor);
                vertex(outer[i], outer[i + 1], outerColor);
            }
        }

        private void submit(GuiGraphics g) {
            int x = (int) Math.floor(minX), y = (int) Math.floor(minY);
            bounds = new ScreenRectangle(x, y, (int) Math.ceil(maxX) - x, (int) Math.ceil(maxY) - y)
                    .transformMaxBounds(pose);
            if (scissor != null) bounds = scissor.intersection(bounds);
            if (bounds != null && bounds.width() > 0 && bounds.height() > 0) g.guiRenderState.submitGuiElement(this);
        }

        @Override
        public void buildVertices(VertexConsumer consumer) {
            for (int i = 0; i < size; i++) {
                consumer.addVertexWith2DPose(pose, positions[i * 2], positions[i * 2 + 1]).setColor(colors[i]);
            }
        }

        @Override public RenderPipeline pipeline() { return RenderPipelines.GUI; }
        @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        @Override public ScreenRectangle scissorArea() { return scissor; }
        @Override public ScreenRectangle bounds() { return bounds; }
    }
}
