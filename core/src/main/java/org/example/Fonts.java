package org.example;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

public class Fonts {

    // Built from integer code points - immune to source-file encoding issues.
    private static final String EXTRA_CHARS = buildExtraChars();

    private static String buildExtraChars() {
        int[] codePoints = {
            0x2192, // right arrow  ->
            0x2190, // left arrow   <-
            0x2191, // up arrow
            0x2193, // down arrow
            0x2022, // bullet       *
            0x00B7, // middle dot   .
            0x2026, // ellipsis     ...
            0x2605, // black star
            0x2606, // white star
            0x2713, // check mark
            0x2717, // cross mark
        };
        StringBuilder sb = new StringBuilder();
        for (int cp : codePoints) sb.appendCodePoint(cp);
        return sb.toString();
    }

    /** 15px body text / table rows - drop-in for the default BitmapFont. */
    public static BitmapFont ui;

    public static void load() {
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("font.ttf"));

        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size       = 15;
        p.characters = FreeTypeFontGenerator.DEFAULT_CHARS + EXTRA_CHARS;

        ui = gen.generateFont(p);
        ui.getData().markupEnabled = false;

        gen.dispose();
    }

    public static void dispose() {
        if (ui != null) { ui.dispose(); ui = null; }
    }
}
