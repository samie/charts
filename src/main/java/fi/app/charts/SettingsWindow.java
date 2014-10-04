package fi.app.charts;

import com.vaadin.ui.Button;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class SettingsWindow extends Window {

    private final TextArea text;
    private SaveCallback cb;
    private SpreadsheetSettings settings;

    SettingsWindow(SpreadsheetSettings settings, SaveCallback closeCb) {
        setModal(true);
        setWidth("500px");
        setHeight("400px");
        this.cb = closeCb;
        this.settings = settings;
        VerticalLayout l = new VerticalLayout();
        l.setSpacing(true);
        l.setMargin(true);
        l.setSizeFull();
        text = new TextArea();
        text.setSizeFull();
        l.addComponent(text);
        l.setExpandRatio(text, 1);

        text.setValue(SpreadsheetSettings.settingsToString(settings));
        l.addComponent(new Button("Apply", new Button.ClickListener() {

            @Override
            public void buttonClick(Button.ClickEvent event) {
                SpreadsheetSettings s = SpreadsheetSettings.stringToSettings(SettingsWindow.this.settings.getSettingId(), text.getValue());
                s.setFile(SettingsWindow.this.settings.getFile());
                cb.closed(s);
                UI.getCurrent().removeWindow(SettingsWindow.this);
            }
        }));
        setContent(l);
    }

    public interface SaveCallback {

        public void closed(SpreadsheetSettings settings);
    }

}
