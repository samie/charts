package fi.app.charts;

import com.google.gdata.client.spreadsheet.FeedURLFactory;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.Cell;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.util.ServiceException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Spreadsheet class for easier random access to data.
 */
public class SpreadSheet implements Serializable {

    private String[] colHeaders;
    private ColType[] colTypes;
    private Object[][] dataFields;
    private String title;
    private int realColCount = -1;
    private int realRowCount = -1;
    private String documentKey;

    enum ColType {
        NUMERIC, STRING, TIMESTAMP, MULTISELECT
    }

    private SpreadSheet() {
    }

    public SpreadSheet(String documentKey, String title) {
        this.documentKey = documentKey;
        this.title = title;
    }

    String getDocumentKey() {
        return documentKey;
    }

    public double[] aggregate(int col) {
        if (!isNumericColumn(col)) {
            return null;
        }
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        double sum = 0, avg = 0;
        int count = dataFields.length - 1;
        for (int i = 1; i < dataFields.length; i++) {
            Number v = (Number) dataFields[i][col];
            double d = v != null ? v.doubleValue() : 0;
            sum += d;
            if (d < min) {
                min = d;
            }
            if (d > max) {
                max = d;
            }
        }
        avg = sum / count;
        return new double[]{count, sum, min, max, avg};
    }

    int colCount() {
        if (realColCount < 0) {
            realColCount = countCols();
        }
        return realColCount;
    }

    public boolean isNumericColumn(int col) {
        return colTypes[col] == ColType.NUMERIC;
    }

    public boolean isTimestampColumn(int col) {
        return colTypes[col] == ColType.TIMESTAMP;
    }

    public boolean isMultiselect(int col) {
        return colTypes[col] == ColType.MULTISELECT;
    }

    String getColName(int col) {
        return colHeaders[col];
    }

    int rowCount() {
        if (realRowCount < 0) {
            realRowCount = countRows();
        }
        return realRowCount;
    }

    String getStringValue(int row, int col) {
        Object v = dataFields[row][col];
        return v != null ? v.toString() : null;
    }

    String getTitle() {
        return this.title;
    }

    String[] getColumnHeaders() {
        return Arrays.copyOf(colHeaders, colCount());
    }

    private int countCols() {
        for (int i = 0; i < colHeaders.length; i++) {
            if (colHeaders[i] == null || "".equals(colHeaders[i])) {
                return i;
            }
        }
        return colHeaders.length;
    }

    private int countRows() {
        for (int i = 0; i < dataFields.length; i++) {
            if (dataFields[i][0] == null || "".equals(dataFields[i][0])) {
                return i;
            }
        }
        return dataFields.length;
    }

    /**
     * Count aggregate
     */
    List<AggregateValue> aggretageGroups(int col, SpreadsheetFilters filters) {
        Map<String, Integer> map = new HashMap<String, Integer>();
        int totalCount = 0;
        for (int i = 0; i < rowCount(); i++) {

            // Apply filter
            if (!filters.filterMatch(i)) {
                continue;
            }

            String key = null;
            if (isTimestampColumn(col)) {
                key = "" + getStringValue(i, col).split(" ")[0];

            } else {
                key = (""+getStringValue(i, col)).trim();
            }

            Number v = map.get(key);
            int count = v == null ? 1 : v.intValue() + 1;
            map.put(key, count);
            totalCount++;
        }
        List<AggregateValue> res = new ArrayList<AggregateValue>();
        for (String t : map.keySet()) {
            res.add(new AggregateValue(t, map.get(t), ((double)map.get(t))/(double)totalCount*100d));
        }
        return res;
    }

    List<AggregateValue> aggretageMultiselectGroups(int col, SpreadsheetFilters filters) {
        Map<String, Integer> map = new HashMap<String, Integer>();
        
        int totalCount = 0;
        for (int i = 0; i < rowCount(); i++) {

            // Apply filter
            if (!filters.filterMatch(i)) {
                continue;
            }
            
            String selection = (""+getStringValue(i, col)).trim();
            String[] keys = selection.split(",");
            for (String key : keys) {
                key = key.trim();
                Number v = map.get(key);
                int count = v == null ? 1 : v.intValue() + 1;
                map.put(key, count);
                totalCount++;
            }
        }
        List<AggregateValue> res = new ArrayList<AggregateValue>();
        for (String t : map.keySet()) {
            res.add(new AggregateValue(t, map.get(t), ((double)map.get(t))/(double)totalCount*100d));
        }
        return res;
    }

    public static class AggregateValue implements Comparable<AggregateValue> {

        String title;
        Number value;
        Number percentage;

        private AggregateValue(String t, int value, double percentage) {
            this.title = t;
            this.value = value;
            this.percentage = percentage;
        }

        @Override
        public int compareTo(AggregateValue o) {
            return value.doubleValue() - o.value.doubleValue() < 0 ? 1 : -1;
        }

    }

    static SpreadSheet loadSpreadsheetFromDrive(String documentKey, String accessToken) {
        Logger.getLogger(DesktopUI.class.getName()).log(Level.INFO, "Loading spreadsheet from Google Drive '" + documentKey + "'");
        try {

            SpreadsheetService service = new SpreadsheetService("Google Spreadsheet Visualizer");
            service.setHeader("Authorization", "Bearer " + accessToken);

            FeedURLFactory f = FeedURLFactory.getDefault();
            URL worksheetsUrl = f.getWorksheetFeedUrl(documentKey, "private", "full");

            WorksheetFeed wsFeed = service.getFeed(worksheetsUrl, WorksheetFeed.class);
            WorksheetEntry ws = wsFeed.getEntries().get(0);

            // Fetch the cell feed of the worksheet.
            URL cellFeedUrl = ws.getCellFeedUrl();
            CellFeed cellFeed = service.getFeed(cellFeedUrl, CellFeed.class);

            // Init Spreadsheet object
            SpreadSheet s = new SpreadSheet(documentKey,wsFeed.getTitle().getPlainText());
            s.colHeaders = new String[cellFeed.getColCount()];
            s.colTypes = new SpreadSheet.ColType[cellFeed.getColCount()];
            s.dataFields = new Object[cellFeed.getRowCount()][cellFeed.getColCount()];

            // Iterate through each cell
            for (CellEntry cellEntry : cellFeed.getEntries()) {
                Cell c = cellEntry.getCell();
                int col = c.getCol() - 1;
                int row = c.getRow() - 1;
                if (row == 0) {
                    s.colHeaders[col] = c.getValue();
                } else {

                    // Find out the type
                    if (row == 1) {
                        if (col == 0) {
                            s.colTypes[col] = SpreadSheet.ColType.TIMESTAMP;
                        } else {
                            s.colTypes[col] = c.getNumericValue() == null ? SpreadSheet.ColType.STRING : SpreadSheet.ColType.NUMERIC;

                        }
                    }

                    // Store the value
                    if (s.colTypes[col] == SpreadSheet.ColType.NUMERIC) {
                        s.dataFields[row - 1][col] = c.getNumericValue();
                    } else {
                        s.dataFields[row - 1][col] = c.getValue();
                    }
                }

            }
            return s;
        } catch (IOException ex) {
            Logger.getLogger(DesktopUI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ServiceException ex) {
            Logger.getLogger(DesktopUI.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }
}
