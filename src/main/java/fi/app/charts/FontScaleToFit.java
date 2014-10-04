package fi.app.charts;

import com.vaadin.annotations.JavaScript;
import com.vaadin.server.AbstractJavaScriptExtension;
import com.vaadin.ui.Label;

/* Extension for Label to scale the font so that the text fits into the label.  */
@JavaScript("fontscaletofit.js")
public class FontScaleToFit extends AbstractJavaScriptExtension {

    public static FontScaleToFit label(Label label, int minFontPx, int maxFontPx) {
        return new FontScaleToFit(label, minFontPx, maxFontPx);
    }

    private FontScaleToFit(Label label, int minFontPx, int maxFontPx) {
        extend(label);
        callFunction("scaleFontToFit", minFontPx, maxFontPx);
    }

}
