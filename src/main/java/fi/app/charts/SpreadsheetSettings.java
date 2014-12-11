package fi.app.charts;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Settings for a single spreadsheet.
 */
public class SpreadsheetSettings extends ApplicationProperties {

    private static final String KEY_LINK_TITLE = "link.title";
    private static final String KEY_LINK_URL = "link.url";
    private static final String KEY_TYPES = "types";
    private static final String KEY_SORT = "sort";
    private static final String KEY_SCALE_DISPLAY = "scale_display";
    private static final String KEY_MULTISELECT = "multiselect";
    private static final String KEY_SORT_ORDER = "sort_order";
    private static final String KEY_TITLE = "display";
    private static final String KEY_DOCUMENT_TITLE = "document_title";
    private static final String KEY_PUBLIC = "published";
    private static final String KEY_ALLOW_FILTERING = "allow_filtering";
    private static final String KEY_DRIVE_DOC_KEY = "key";

    private SpreadsheetSettings(String docId) {
        super(docId);
    }

    public boolean isVisible(int col) {
        String key = typesKey(col);
        return this.containsKey(key) && !"".equals(this.getProperty(key).trim());
    }

    public String getTitle(int col) {
        return getProperty(titleKey(col)).trim();
    }

    public boolean isPublic() {
        return "true".equalsIgnoreCase(getProperty(KEY_PUBLIC));
    }

    void setPublic(boolean value) {
        setProperty(KEY_PUBLIC, "" + value);
    }

    public boolean isAllowFiltering() {
        return "true".equalsIgnoreCase(getProperty(KEY_ALLOW_FILTERING));
    }

    void setAllowFiltering(boolean value) {
        setProperty(KEY_ALLOW_FILTERING, "" + value);
    }

    public SortBy getSortBy(int col) {
        String value = getProperty(sortKey(col)).trim();
        if (value == null) {
            return SortBy.VALUE;
        }
        String[] v = value.split(",");
        if (v.length > 0) {
            return SortBy.valueOf(v[0].trim());
        }
        return SortBy.valueOf(value.trim());
    }

    public SortDirection getSortDirection(int col) {
        String value = getProperty(sortKey(col)).trim();
        String[] v = value.split(",");
        if (v.length > 1) {
            return SortDirection.valueOf(v[1].trim());
        }
        return SortDirection.ASC;
    }

    public ScaleDisplay getScaleDisplay(int col) {
        String value = getProperty(scaleDisplayKey(col));
        if (value != null && value.trim().length() > 1) {
            return ScaleDisplay.valueOf(value.trim());
        }
        return ScaleDisplay.VALUES;
    }

    public boolean isMultiselect(int col) {
        String value = getProperty(multiselectKey(col));
        return (value != null && value.trim().length() > 1 && "true".equals(value.trim().toLowerCase()));
    }

    public Comparator<SpreadSheet.AggregateValue> getCustomSort(int col) {
        final List<String> sortOrder = new ArrayList<String>();
        String sorts = getProperty(sortOrderKey(col));
        if (sorts != null && sorts.length() > 0) {
            sortOrder.addAll(Arrays.asList(sorts.split(";")));
        }

        return new Comparator<SpreadSheet.AggregateValue>() {

            @Override
            public int compare(SpreadSheet.AggregateValue v1, SpreadSheet.AggregateValue v2) {
                return sortOrder.indexOf(v1.title) - sortOrder.indexOf(v2.title);
            }

        };
    }

    public ColumnDisplayType[] getDisplayTypes(int col) {
        String value = getProperty(typesKey(col));
        if (value != null) {
            String[] types = value.split(",");
            List<ColumnDisplayType> res = new ArrayList<>();
            for (int i = 0; i < types.length; i++) {
                try {
                    res.add(ColumnDisplayType.valueOf(types[i].trim()));
                } catch (IllegalArgumentException e) {
                    // we simply ignore invalid values
                }
            }
            return res.toArray(new ColumnDisplayType[0]);
        } else {
            return ALL_DISPLAY_TYPES;
        }
    }

    /*    private String fmt(String colName) {
     return colName.replaceAll(" ", "_").replaceAll(",", "_").replaceAll("\\?", "").replaceAll("\\!", "");
     }*/
    public static String settingsToString(SpreadsheetSettings settings) {
        StringBuilder b = new StringBuilder();
        List<String> keys = new ArrayList<>();
        keys.addAll(settings.stringPropertyNames());
        Collections.sort(keys);
        String colId = "";
        for (String key : keys) {
            if (key.length() > 7 && !colId.equals(key.substring(0, 7))) {
                b.append("\n# ");
                b.append(key.substring(0, 7));
                b.append("\n");
                colId = key.substring(0, 7);
            }
            b.append(key);
            b.append("=");
            b.append(settings.getProperty(key));
            b.append("\n");

        }
        return b.toString();
    }

    public static SpreadsheetSettings stringToSettings(String documentId, String string) {
        try {
            SpreadsheetSettings settings = new SpreadsheetSettings(documentId);
            StringReader r = new StringReader(string);
            settings.load(r);
            return settings;
        } catch (IOException ex) {
            Logger.getLogger(SettingsWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static SpreadsheetSettings createNewSettings(File confDir, String documentKey) {
        SpreadsheetSettings newSettings = new SpreadsheetSettings(documentKey);
        newSettings.setProperty(KEY_PUBLIC, "false");
        newSettings.setProperty(KEY_ALLOW_FILTERING, "false");
        newSettings.setFile(new File(confDir, newSettings.getConfFileName()));

        return newSettings;
    }

    public static void copyValues(SpreadsheetSettings to, SpreadsheetSettings from) {
        if (from != null) {
            for (String key : from.stringPropertyNames()) {
                to.setProperty(key, from.getProperty(key));
            }
        }
    }

    public static void copyValuesFromSpreadsheet(SpreadsheetSettings settings, SpreadSheet s) {
        settings.setProperty(KEY_DRIVE_DOC_KEY, s.getDocumentKey());
        settings.setProperty(KEY_DOCUMENT_TITLE, s.getTitle());
        for (int i = 0; i < s.colCount(); i++) {
            settings.setProperty("col_" + String.format("%03d", i) + "_" + KEY_TYPES, listAllTypes());
            settings.setProperty("col_" + String.format("%03d", i) + "_" + KEY_TITLE, s.getColName(i));
            settings.setProperty("col_" + String.format("%03d", i) + "_" + KEY_SORT, SortBy.VALUE.name() + "," + SortDirection.DESC.name());
        }
    }

    static SpreadsheetSettings load(File confDir, String documentKey, boolean createIfNotExist) {

        SpreadsheetSettings storedSettings = new SpreadsheetSettings(documentKey);
        if (storedSettings.loadFrom(new File(confDir, storedSettings.getConfFileName()))) {
            return storedSettings;
        } else if (createIfNotExist) {
            Logger.getLogger(DesktopUI.class.getName()).log(Level.INFO, "Creating configuration for  " + documentKey);
            SpreadsheetSettings settings = createNewSettings(confDir, documentKey);
            return settings;
        }
        return null;
    }

    private String typesKey(int col) {
        return "col_" + String.format("%03d", col) + "_" + KEY_TYPES;
    }

    private String sortKey(int col) {
        return "col_" + String.format("%03d", col) + "_" + KEY_SORT;
    }

    private String scaleDisplayKey(int col) {
        return "col_" + String.format("%03d", col) + "_" + KEY_SCALE_DISPLAY;
    }

    private String multiselectKey(int col) {
        return "col_" + String.format("%03d", col) + "_" + KEY_MULTISELECT;
    }

    private String sortOrderKey(int col) {
        return "col_" + String.format("%03d", col) + "_" + KEY_SORT_ORDER;
    }

    private String titleKey(int col) {
        return "col_" + String.format("%03d", col) + "_" + KEY_TITLE;
    }

    public String getDocumentKey() {
        return getProperty(KEY_DRIVE_DOC_KEY);
    }

    public void setDocumentKey(String docKey) {
        setProperty(KEY_DRIVE_DOC_KEY, docKey);
    }

    public String getDocumentTitle() {
        return getProperty(KEY_DOCUMENT_TITLE);
    }

    public void setDocumentTitle(String docTitle) {
        setProperty(KEY_DOCUMENT_TITLE, docTitle);
    }

    public String getExternalLinkTitle() {
        return getProperty(KEY_LINK_TITLE);
    }

    public void setExternalLinkTitle(String linkTitle) {
        setProperty(KEY_LINK_TITLE, linkTitle);
    }

    public String getExternalLinkUrl() {
        return getProperty(KEY_LINK_URL);
    }

    public void setExternalLinkUrl(String linkUrl) {
        setProperty(KEY_LINK_URL, linkUrl);
    }

    public boolean isInitialized() {
        return containsKey(titleKey(0));
    }

    public enum ScaleDisplay {

        VALUES,
        PERCENTAGE;
    }

    public enum ColumnDisplayType {

        BAR,
        PIE,
        LINE;

        public String userString() {
            return super.name();
        }

    };

    public static ColumnDisplayType[] ALL_DISPLAY_TYPES = new ColumnDisplayType[]{ColumnDisplayType.BAR, ColumnDisplayType.PIE, ColumnDisplayType.LINE};

    public static String listAllTypes() {
        StringBuilder s = new StringBuilder();
        for (ColumnDisplayType type : ALL_DISPLAY_TYPES) {
            s.append(", ");
            s.append(type.name());
        }
        return s.substring(1);
    }

    public enum SortBy {

        VALUE,
        NAME,
        CUSTOM
    }

    public enum SortDirection {

        ASC, DESC
    }

}
