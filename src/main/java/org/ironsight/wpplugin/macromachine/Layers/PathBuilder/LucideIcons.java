package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Lucide icons (https://lucide.dev, ISC license) from
 * {@code /icons/<name>.svg}, drawn in any colour: the icons stroke with
 * {@code currentColor} on a 24 x 24 grid, which is replaced before parsing.
 * Parsed documents are cached per colour and stroke width.
 */
public final class LucideIcons
{
    private static final Map<String, SVGDocument> DOCUMENTS = new ConcurrentHashMap<>();
    private static final double GRID = 24;

    private LucideIcons() {
    }

    private static SVGDocument document(String name, Color colour, double strokeWidth) {
        String key = name + '|' + colour.getRGB() + '|' + strokeWidth;
        return DOCUMENTS.computeIfAbsent(key, ignored -> {
            String svg;
            try (var stream = Objects.requireNonNull(LucideIcons.class.getResourceAsStream("/icons/" + name + ".svg"),
                    name)) {
                svg = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
            svg = svg.replace("currentColor", String.format(Locale.ROOT, "#%06x", colour.getRGB() & 0xffffff))
                    .replace("stroke-width=\"2\"", String.format(Locale.ROOT, "stroke-width=\"%.2f\"", strokeWidth));
            if (colour.getAlpha() < 255)
                svg = svg.replace("<svg",
                        String.format(Locale.ROOT, "<svg opacity=\"%.3f\"", colour.getAlpha() / 255.0));
            return Objects
                    .requireNonNull(new SVGLoader().load(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)),
                            null, LoaderContext.createDefault()), name);
        });
    }

    /**
     * Draws icon {@code name} centred on {@code centre}, {@code size} pixels wide,
     * turned by {@code degrees} (counter-clockwise on screen). With a {@code halo}
     * colour it gets an outline of about 1.5 px, so it stays readable on any map.
     */
    public static void paint(Graphics2D g, String name, Point2D centre, double size, double degrees, Color colour,
            Color halo) {
        Graphics2D target = (Graphics2D) g.create();
        try {
            target.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            target.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            target.translate(centre.getX(), centre.getY());
            target.rotate(-Math.toRadians(degrees));
            double scale = size / GRID;
            target.scale(scale, scale);
            target.translate(-GRID / 2, -GRID / 2);
            if (halo != null)
                document(name, halo, 2 + 3 / scale).render(null, target);
            document(name, colour, 2).render(null, target);
        } finally {
            target.dispose();
        }
    }

    /** The icon as a Swing icon, e.g. for menus and buttons. */
    public static Icon icon(String name, int size, Color colour) {
        var image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            paint(g, name, new Point2D.Double(size / 2.0, size / 2.0), size, 0, colour, null);
        } finally {
            g.dispose();
        }
        return new ImageIcon(image);
    }
}
