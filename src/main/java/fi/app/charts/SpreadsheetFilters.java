/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.app.charts;

import java.util.ArrayList;
import java.util.List;

/** Filters for spreadsheet.
 *
 */
public class SpreadsheetFilters {

    interface Listener {

        public void filterChandged(SpreadsheetFilters source);

    }

    public class Filter {

        private int column = -1;
        private String matchValue;
        private boolean active = true; // by default

        private Filter(int column, String matchValue) {
            this.column = column;
            this.matchValue = matchValue;
        }

        public int getColumn() {
            return column;
        }

        public String getValue() {
            return matchValue;
        }

        private boolean match(String value) {
            if (matchValue == null) {
                return false;
            }
            // TODO: multislection detection does not work correctly
            return (value != null && value.indexOf(matchValue) >= 0);
        }

        public boolean match(SpreadSheet spreadsheet, int row) {
            if (!isActive()) {
                return true;
            }
            String value = spreadsheet.getStringValue(row, column);
            return match(value);
        }

        public String getColumnName() {
            return spreadsheet.getColName(column);
        }

        @Override
        public int hashCode() {
            return column + matchValue.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Filter) {
                return column == ((Filter) other).column && matchValue.equals(((Filter) other).matchValue);
            }
            return false;

        }

        public void setActive(boolean active) {
            this.active = active;
            fireFiltersChanged();
        }

        public boolean isActive() {
            return this.active;
        }

    }

    private SpreadSheet spreadsheet;
    private Listener listener;
    List<Filter> filters = new ArrayList<>();

    public SpreadsheetFilters(SpreadSheet s) {
        this.spreadsheet = s;
    }

    public String getText(SpreadsheetSettings settings) {
        return getText(false, settings);
    }

    public String getText(boolean html, SpreadsheetSettings settings) {

        String s = "";
        if (hasActiveFilters()) {
            s = "When ";
            for (Filter filter : filters) {
                if (!filter.isActive()) {
                    continue;
                }

                if (s.length() > 6) {
                    s += " and ";
                }
                s += "" + (html ? "<span class=\"name\">" : "'") + settings.getTitle(filter.getColumn()) + (html ? "</span>" : "'") + " is " + (html ? "<span class=\"value\">" : "'") + filter.getValue() + (html ? "</span>" : "'");
            }
        }
        return s;
    }

    public boolean hasFilter(int col, String value) {
        return this.filters.contains(new Filter(col, value));
    }

    public void removeFilter(int col, String value) {
        this.filters.remove(new Filter(col, value));
        fireFiltersChanged();
    }

    public List<Filter> getAllFilters() {
        return filters;
    }

    public List<Filter> getActiveFilters() {
        ArrayList<Filter> res = new ArrayList<>(filters.size());
        for (Filter filter : this.filters) {
            if (filter.isActive()) {
                res.add(filter);
            }
        }
        return res;
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void setFilter(int colIndex, String filterValue) {

        filters.add(new Filter(colIndex, filterValue));
        fireFiltersChanged();
    }

    public boolean filterMatch(int row) {
        if (!hasActiveFilters()) {
            return true;
        }
        boolean matchAll = true;
        for (Filter filter : filters) {
            matchAll &= filter.match(this.spreadsheet, row);
        }
        return matchAll;
    }

    public boolean hasActiveFilters() {
        return getActiveFilters().size() > 0;
    }

    void clearAll() {
        this.filters.clear();
        this.fireFiltersChanged();

    }

    void fireFiltersChanged() {
        if (listener != null) {
            listener.filterChandged(this);
        }

    }

    public boolean hasFilters() {
        return getAllFilters().size() > 0;
    }

}
