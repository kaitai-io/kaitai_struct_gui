package io.github.proto4j.kaitai.vis.util; //@date 28.07.2023

import java.awt.*;

// Simple way to generate a wide range of colors
public final class ColorSpec {
    public static final int[] COLOR_SPEC = {
            0xCC2929, // 6: i*0x1C00 (+)
            0xC7CC29, // 6: i*0x1C0000 (-)
            0x29CC32, // 6: i*0x1C (+)
            0x29BECC, // 6: i*0x1C00 (-)
            0x3B29CC, // 6: i*0x1C0000 (+)
            0xCC29B5, // 6: i*0x1C (-)
    };
    public static final int[] COLOR_OFFSETS = {
            0x1C00,
            0x1C0000,
            0x1C
    };
    public static final Color[] COLORS = new Color[6 * 6];

    static {
        int index = 0;
        for (int i = 0; i < COLOR_SPEC.length; i++) {
            int start = COLOR_SPEC[i];
            int offset = COLOR_OFFSETS[i % COLOR_OFFSETS.length];
            boolean negative = i % 2 != 0;

            for (int j = 0; j < 6; j++) {
                int value;
                if (negative) {
                    value = start - (j * offset);
                } else {
                    value = start + (j * offset);
                }
                COLORS[index] = new Color((value << 8) | 0xD7, true);
                index++;
            }
        }
    }

    private ColorSpec() {
    }

    public static Color random() {
        return COLORS[(int) (Math.random() * COLORS.length)];
    }
}
