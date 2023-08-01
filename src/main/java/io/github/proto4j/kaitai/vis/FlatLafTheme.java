package io.github.proto4j.kaitai.vis; //@date 29.07.2023

import cms.rendner.hexviewer.common.geom.HDimension;
import cms.rendner.hexviewer.common.utils.AsciiUtils;
import cms.rendner.hexviewer.view.JHexViewer;
import cms.rendner.hexviewer.view.components.areas.bytes.ByteArea;
import cms.rendner.hexviewer.view.components.areas.bytes.model.colors.IByteColorProvider;
import cms.rendner.hexviewer.view.components.areas.common.AreaComponent;
import cms.rendner.hexviewer.view.components.areas.common.painter.background.DefaultBackgroundPainter;
import cms.rendner.hexviewer.view.components.areas.offset.model.colors.IOffsetColorProvider;
import cms.rendner.hexviewer.view.components.highlighter.DefaultHighlighter;
import cms.rendner.hexviewer.view.themes.AbstractTheme;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Map;

public class FlatLafTheme extends AbstractTheme {

    public static String KEY_BYTE_NULL = "Label.disabledForeground";
    public static String KEY_FOREGROUND = "Label.foreground";
    public static String KEY_CARET = "TextArea.caretForeground";
    public static String KEY_SELECTION_FOREGROUND = "TextArea.selectionForeground";
    public static String KEY_SELECTION_BACKGROUND = "TextArea.selectionBackground";
    public static String KEY_BACKGROUND = "Panel.background";
    public static String KEY_BORDER = "Separator.foreground";

    public static String KEY_OFFSET_FOREGROUND = "List.foreground";
    public static String KEY_OFFSET_ACTIVE_FOREGROUND = "List.selectionForeground";
    public static String KEY_OFFSET_ACTIVE_BACKGROUND = "List.selectionBackground";

    public FlatLafTheme() {
    }


    @Override
    protected void adjustColorProviders(JHexViewer hexViewer) {
        super.adjustColorProviders(hexViewer);
        hexViewer.getOffsetArea().setColorProvider(new OffsetColorProvider(hexViewer));
        hexViewer.getHexArea().setColorProvider(new ByteAreaColorProvider(hexViewer, hexViewer.getHexArea()));
        hexViewer.getTextArea().setColorProvider(new ByteAreaColorProvider(hexViewer, hexViewer.getTextArea()));
    }

    @Override
    protected void adjustPainters(JHexViewer hexViewer) {
        super.adjustPainters(hexViewer);
        setAreaBackgroundPainter(hexViewer.getOffsetArea(), new BorderPainter());
        setAreaBackgroundPainter(hexViewer.getTextArea(), new BorderPainter());
        setAreaBackgroundPainter(hexViewer.getHexArea(), new BorderPainter());
    }

    @Override
    protected void adjustComponentDefaults(JHexViewer hexViewer) {
        super.adjustComponentDefaults(hexViewer);
        hexViewer.setBackground(UIManager.getColor(KEY_BACKGROUND));
    }

    private static final class OffsetColorProvider implements IOffsetColorProvider {
        private final JHexViewer hexViewer;

        private OffsetColorProvider(JHexViewer hexViewer) {
            this.hexViewer = hexViewer;
        }

        @Override
        public Color getRowElementForeground(int rowIndex) {
            return hexViewer.isShowOffsetCaretIndicator() && isCaretRowIndex(rowIndex)
                    ? UIManager.getColor(KEY_OFFSET_ACTIVE_FOREGROUND)
                    : UIManager.getColor(KEY_OFFSET_FOREGROUND);
        }

        @Override
        public Color getRowElementBackground(int rowIndex) {
            return hexViewer.isShowOffsetCaretIndicator() && isCaretRowIndex(rowIndex)
                    ? UIManager.getColor(KEY_OFFSET_ACTIVE_BACKGROUND)
                    : UIManager.getColor(KEY_BACKGROUND);
        }

        @Override
        public Color getBackground() {
            return UIManager.getColor(KEY_BACKGROUND);
        }

        private boolean isCaretRowIndex(final int rowIndex) {
            return hexViewer.getCaret().map(caret ->
            {
                final long caretIndex = caret.getDot();
                final int caretRowIndex = hexViewer.byteIndexToRowIndex(caretIndex);
                return rowIndex == caretRowIndex;
            }).orElse(Boolean.FALSE);
        }
    }

    private static final class ByteAreaColorProvider implements IByteColorProvider {

        private final JHexViewer hexViewer;
        private final ByteArea area;

        private ByteAreaColorProvider(JHexViewer hexViewer, ByteArea area) {
            this.hexViewer = hexViewer;
            this.area = area;
        }

        @Override
        public Color getCaret() {
            return UIManager.getColor(KEY_CARET);
        }

        @Override
        public Color getSelection() {
            return hexViewer.getCaretFocusedArea() == area
                    ? UIManager.getColor(KEY_SELECTION_BACKGROUND).brighter()
                    : UIManager.getColor(KEY_SELECTION_BACKGROUND);
        }

        @Override
        public Color getBackground() {
            return UIManager.getColor(KEY_BACKGROUND);
        }

        @Override
        public Color getRowElementForeground(int byteValue, long offset, int rowIndex, int elementInRowIndex) {
            if (AsciiUtils.NULL == byteValue) {
                return UIManager.getColor(KEY_BYTE_NULL);
            }
            return isSelected(offset) ? UIManager.getColor(KEY_SELECTION_FOREGROUND) : UIManager.getColor(KEY_FOREGROUND);
        }

        @Override
        public Color getDefaultHighlight() {
            return UIManager.getColor(KEY_SELECTION_BACKGROUND);
        }

        private boolean isSelected(final long offset) {
            return hexViewer.getCaret()
                    .map(caret -> caret.hasSelection() && caret.getSelectionStart() <= offset && offset <= caret.getSelectionEnd())
                    .orElse(Boolean.FALSE);
        }
    }

    private static final class BorderPainter extends DefaultBackgroundPainter {
        private final Border separator;

        public BorderPainter() {
            this(UIManager.getColor(KEY_BORDER));
        }

        public BorderPainter(final Color color) {
            this.separator = BorderFactory.createMatteBorder(0, 0, 0, 1, color);
        }

        @Override
        public void paint(Graphics2D g, JHexViewer hexViewer, AreaComponent component) {
            super.paint(g, hexViewer, component);
            separator.paintBorder(component, g, 0, 0, component.getWidth(), component.getHeight());
        }
    }

    public static class HighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {

        public HighlightPainter(Color fallbackColor) {
            super(fallbackColor);
        }

        @Override
        protected Color getColor(ByteArea area) {
            return this.fallbackColor;
        }

        @Override
        public void paint(Graphics2D g, JHexViewer hexViewer, ByteArea area, HDimension rowElementsHDimension, long byteStartIndex, long byteEndIndex) {
            super.paint(g, hexViewer, area, rowElementsHDimension, byteStartIndex, byteEndIndex);
            final Rectangle startByteRect = area.getByteRect(byteStartIndex);
            final Rectangle endByteRect = area.getByteRect(byteEndIndex);

            g.setColor(getColor(null).darker());
            if (startByteRect.y == endByteRect.y) {
                // same line
                final Rectangle r = startByteRect.union(endByteRect);
                g.drawRect(r.x - 1, r.y - 1, r.width + 1, r.height + 1);
            }
        }
    }
}
