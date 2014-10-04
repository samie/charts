package fi.app.charts;

import com.vaadin.data.Property;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import java.util.ArrayList;
import java.util.List;

public class ButtonList extends CssLayout implements Property {
    
    private ValueChangeListener listener;
    private Object value;
    private List<Button> buttons = new ArrayList<Button>();
    
    public ButtonList() {
        addStyleName("fadable");
    }
    
    public void addValueChangeListener(ValueChangeListener valueChangeListener) {
        this.listener = valueChangeListener;
    }
    
    public void addItem(Object i, String title, String description) {
        Button b = new Button(title);
        b.setData(i);
        b.setStyleName("lst");
        if (description != null) {
            b.setDescription(description);
        }
        b.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                Object newValue = event.getButton().getData();
                internalSetValue(newValue);
                fireChangeEvent();
            }
        });
        buttons.add(b);
        this.addComponent(b);
    }
    
    @Override
    public Object getValue() {
        return value;
    }
    
    @Override
    public void setValue(Object newValue) throws ReadOnlyException {
        this.value = newValue;
        internalSetValue(newValue);
        fireChangeEvent();
    }
    
    public void removeHighlights() {
        for (Button b : this.buttons) {
            b.removeStyleName("highlight");
        }
    }
    
    public void setHighlighted(Object newValue, boolean highlighted) {
        for (Button b : this.buttons) {
            if (b.getData().equals(newValue)) {
                if (highlighted) {
                    b.addStyleName("highlight");
                } else {
                    b.removeStyleName("highlight");
                }
            }
        }
    }
    
    @Override
    public Class getType() {
        return Object.class;
    }
    
    private void internalSetValue(Object newValue) {
        this.value = newValue;
        removeSelections();
        setSelected(newValue, true);
    }
    
    public void removeSelections() {
        for (Button b : this.buttons) {
            b.removeStyleName("selected");
        }
    }
    
    void setSelected(Object newValue, boolean selected) {
        for (Button b : this.buttons) {
            if (b.getData().equals(newValue)) {
                value = b.getData();
                if (selected) {
                    b.addStyleName("selected");
                } else {
                    b.removeStyleName("selected");
                }
            }
        }
    }
    
    private void fireChangeEvent() {
        if (listener != null) {
            listener.valueChange(new Property.ValueChangeEvent() {
                @Override
                public Property getProperty() {
                    return ButtonList.this;
                }
            });
        }
    }
    
    void addItem(Object obj, String value) {
        addItem(obj, value, null);
        
    }
    
}
