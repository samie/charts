package fi.app.charts;

import com.vaadin.addon.charts.Chart;
import com.vaadin.addon.charts.PointClickEvent;
import com.vaadin.addon.charts.PointClickListener;
import com.vaadin.addon.charts.model.AbstractPlotOptions;
import com.vaadin.addon.charts.model.AxisType;
import com.vaadin.addon.charts.model.ChartType;
import com.vaadin.addon.charts.model.Configuration;
import com.vaadin.addon.charts.model.Credits;
import com.vaadin.addon.charts.model.Cursor;
import com.vaadin.addon.charts.model.DataSeries;
import com.vaadin.addon.charts.model.DataSeriesItem;
import com.vaadin.addon.charts.model.Labels;
import com.vaadin.addon.charts.model.Marker;
import com.vaadin.addon.charts.model.PlotOptionsBar;
import com.vaadin.addon.charts.model.PlotOptionsLine;
import com.vaadin.addon.charts.model.PlotOptionsPie;
import com.vaadin.addon.charts.model.SubTitle;
import com.vaadin.addon.charts.model.Title;
import com.vaadin.addon.charts.model.Tooltip;
import com.vaadin.addon.charts.model.XAxis;
import com.vaadin.addon.charts.model.YAxis;
import com.vaadin.addon.charts.model.style.Color;
import com.vaadin.addon.charts.model.style.SolidColor;
import com.vaadin.addon.charts.model.style.Style;
import com.vaadin.data.Property;
import com.vaadin.server.ExternalResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

public class ChartView extends AbsoluteLayout {

    private static Color FILTER_COLOR = new SolidColor(230, 30, 110);
    private static Color MAIN_COLOR = new SolidColor(0, 180, 240);
    private static Color LABEL_COLOR = new SolidColor(10, 10, 10);

    private static Color[] relativeDarkerColors(int baseR, int baseG, int baseB, int numColors, boolean reverse) {
        Color[] result = new Color[numColors];
        for (int i = 0; i < numColors; i++) {
            double level = (1 - (double) i / numColors);
            result[i] = new SolidColor((int) Math.round(baseR * level),
                    (int) Math.round(baseG * level), (int) Math.round(baseB * level));
        }
        if (reverse) {
            ArrayUtils.reverse(result);
        }
        return result;
    }

    private static Color[] relativeLighterColors(int baseR, int baseG, int baseB, int numColors, boolean reverse) {
        Color[] result = new Color[numColors];
        int invR = 255 - baseR, invG = 255 - baseG, invB = 255 - baseB;
        for (int i = 0; i < numColors; i++) {
            double level = (double) i / numColors;
            result[i] = new SolidColor((int) Math.round(baseR + invR * level), (int) Math.round(baseG + invG * level), (int) Math.round(baseB + invB * level));
        }
        if (reverse) {
            ArrayUtils.reverse(result);
        }
        return result;
    }

    private static Tooltip createTooltip(String format, int decimals) {
        Tooltip tooltip = new Tooltip();
        tooltip.setPointFormat(format);
        tooltip.setValueDecimals(decimals);
        tooltip.setEnabled(false);
        return tooltip;
    }

    private static Title createChartTitle(String title) {
        return new Title(""); //FIXME: null should be enough
    }

    private static Chart createChart(ChartType type) {
        Chart chart = new Chart(type);
        chart.addStyleName("fixedchart");
        chart.setSizeFull();

        // No legend please
        chart.getConfiguration().getLegend().setEnabled(false);

        // Set chart margins / colors
        chart.getConfiguration().getChart().setPlotBackgroundColor(new SolidColor(255, 255, 255, 0));

        // Credits
        Credits c = new Credits("");
        chart.getConfiguration().setCredits(c);

        return chart;
    }

    private static Style createLabelStyle() {
        Style s = new Style();
        s.setFontFamily("Helvetica W01 Light");
        s.setFontSize("13px");
        s.setColor(LABEL_COLOR);
        return s;
    }

    private static Color[] getVaadinColors(int numOfColors, boolean darker) {
        if (!darker) {
            return relativeLighterColors(0, 180, 240, numOfColors, false);
        } else {
            return relativeDarkerColors(0, 180, 240, numOfColors, true);
        }

    }

    private static void configureBarAxis(XAxis xAxis, YAxis yAxis) {
        xAxis.setType(AxisType.CATEGORY);
        yAxis.setTitle(""); //FIXME: Setting "null" defaults to "Values"
        yAxis.setAlternateGridColor(SolidColor.WHITE);
        xAxis.setTickColor(SolidColor.WHITE); //FIXME: How to disable ticks?
        xAxis.setAllowDecimals(false);
        yAxis.setAllowDecimals(false);
        yAxis.setGridLineColor(SolidColor.WHITE);
        xAxis.setGridLineColor(SolidColor.WHITE);
        xAxis.setLineColor(SolidColor.WHITE);
        xAxis.setTickColor(SolidColor.WHITE);
        yAxis.setLineColor(SolidColor.WHITE);
        yAxis.setTickColor(SolidColor.WHITE);

    }

    private static AbstractPlotOptions createBarPlotOptions() {
        PlotOptionsBar plotOptions = new PlotOptionsBar();
        plotOptions.setAllowPointSelect(true);
        plotOptions.setCursor(Cursor.POINTER);
        plotOptions.setShowInLegend(false);
        plotOptions.setShadow(false);
        plotOptions.setPointStart(0);
        plotOptions.setAnimation(true);
        plotOptions.setPointPadding(10);

        plotOptions.setDataLabels(new Labels()); //FIXME               
        plotOptions.getDataLabels().setStyle(createLabelStyle());

        plotOptions.setPointPadding(0);
        plotOptions.setGroupPadding(0.1);

        return plotOptions;
    }

    private static SubTitle createSubTitle(SpreadSheet s, int col, SpreadsheetFilters filters) {
        return null; //No subtitle
    }

    static DecimalFormat df = new DecimalFormat("##");

    private static String percentageFormat(Number percentage) {
        return df.format(percentage);
    }

    private final SpreadSheet spreadSheet;
    private final SpreadsheetSettings settings;
    private final SpreadsheetFilters filters;
    private final int index;
    private Chart chart;
    private ButtonList displayTypes;
    private SpreadsheetSettings.ColumnDisplayType type;
    private boolean allowFiltering;

    ChartView(SpreadSheet spreadSheet, SpreadsheetSettings settings, int col, SpreadsheetFilters filters, SpreadsheetSettings.ColumnDisplayType displayType, boolean authoringMode) {
        setStyleName("chart");
        this.filters = filters;
        this.settings = settings;
        this.spreadSheet = spreadSheet;
        this.index = col;
        this.allowFiltering = authoringMode || settings.isAllowFiltering();

        // Sanitize type
        SpreadsheetSettings.ColumnDisplayType[] types = settings.getDisplayTypes(col);
        if (displayType != null && types != null && Arrays.asList(types).contains(displayType)) {
            type = displayType;
        } else if (types.length > 0) {
            type = types[0];
        } else {
            type = SpreadsheetSettings.ColumnDisplayType.BAR;
        }

        createUI();
    }

    public SpreadsheetSettings.ColumnDisplayType getDisplayType() {
        return (SpreadsheetSettings.ColumnDisplayType) displayTypes.getValue();
    }

    @Override
    public String getCaption() {
        return spreadSheet.getColName(index);
    }

    private Chart getChart() {
        if (chart == null) {

            switch (type) {
                case BAR:
                    chart = createBarChart(spreadSheet, settings, index, filters, allowFiltering);
                    break;
                case LINE:
                    chart = createLineChart(spreadSheet, settings, index, filters, allowFiltering);
                    break;
                default:
                    chart = createPieChart(spreadSheet, settings, index, filters, allowFiltering);
                    break;
            }
        }
        return chart;
    }

    public static Chart createPieChart(final SpreadSheet s, final SpreadsheetSettings settings, final int col, final SpreadsheetFilters filters, boolean allowFiltering) {
        final Chart chart = createChart(ChartType.PIE);
        final Configuration conf = chart.getConfiguration();

        conf.setTitle(createChartTitle(settings.getTitle(col)));
        conf.setSubTitle(createSubTitle(s, col, filters));
        conf.setTooltip(createTooltip("{series.name}: {point.percentage}%", 2));

        PlotOptionsPie plotOptions = new PlotOptionsPie();
        plotOptions.setAllowPointSelect(true);
        plotOptions.setCursor(Cursor.POINTER);
        plotOptions.setShowInLegend(true);
        Labels dataLabels = new Labels();
        dataLabels.setEnabled(true);
        conf.setPlotOptions(plotOptions);

        plotOptions.setShadow(false);
        plotOptions.setInnerSize("45%");

        final DataSeries series = new DataSeries();
        plotOptions.setDataLabels(new Labels()); //FIXME     
        plotOptions.getDataLabels().setStyle(createLabelStyle());

        List<SpreadSheet.AggregateValue> vals = null;
        if (settings.isMultiselect(col)) {
            vals = s.aggretageMultiselectGroups(col, filters);
        } else {
            vals = s.aggretageGroups(col, filters);
        }
        Collections.sort(vals, getComparator(settings, col));

        Color[] colors = getVaadinColors(vals.size(), false);

        int i = 0;
        for (SpreadSheet.AggregateValue a : vals) {
            final DataSeriesItem dataSeriesItem = new DataSeriesItem(a.title, a.value);
            dataSeriesItem.setName(dataSeriesItem.getName() + " - " + percentageFormat(a.percentage) + "%");
            series.add(dataSeriesItem);
            dataSeriesItem.setColor(colors[i++]);
        }
        conf.setSeries(series);
        addFilterClickListener(chart, series, filters, col, conf, s, allowFiltering);

        chart.drawChart(conf);

        return chart;
    }

    private static Comparator<SpreadSheet.AggregateValue> NAME_COMPARATOR = new Comparator<SpreadSheet.AggregateValue>() {

        @Override
        public int compare(SpreadSheet.AggregateValue o1, SpreadSheet.AggregateValue o2) {
            return o1.title.compareTo(o2.title);
        }

    };

    private static Comparator<SpreadSheet.AggregateValue> VALUE_COMPARATOR = new Comparator<SpreadSheet.AggregateValue>() {

        @Override
        public int compare(SpreadSheet.AggregateValue o1, SpreadSheet.AggregateValue o2) {
            return (int) ((int) o2.value.doubleValue() - o1.value.doubleValue());
        }

    };

    public static Comparator<SpreadSheet.AggregateValue> getComparator(SpreadsheetSettings settings, int col) {
        Comparator c = null;
        if (settings.getSortBy(col) == SpreadsheetSettings.SortBy.CUSTOM) {
            return settings.getCustomSort(col);
        } else if (settings.getSortBy(col) == SpreadsheetSettings.SortBy.NAME) {
            return settings.getSortDirection(col) == SpreadsheetSettings.SortDirection.ASC
                    ? NAME_COMPARATOR : Collections.reverseOrder(NAME_COMPARATOR);
        } else {
            return settings.getSortDirection(col) == SpreadsheetSettings.SortDirection.ASC
                    ? VALUE_COMPARATOR : Collections.reverseOrder(VALUE_COMPARATOR);
        }
    }

    public static SpreadsheetSettings.ScaleDisplay getScaleDisplay(SpreadsheetSettings settings, int col) {
        SpreadsheetSettings.ScaleDisplay d = settings.getScaleDisplay(col);
        if (d != null) {
            return d;
        }
        return SpreadsheetSettings.ScaleDisplay.VALUES;
    }

    private static void addFilterClickListener(final Chart chart, final DataSeries series, final SpreadsheetFilters filters, final int col, final Configuration conf, final SpreadSheet s, final boolean allowFiltering) {
        if (!allowFiltering) {
            return;
        }
        chart.addPointClickListener(new PointClickListener() {

            @Override
            public void onClick(PointClickEvent event) {
                DataSeriesItem item = series.get(event.getPointIndex());
                String value = item.getName();
                if (filters.hasFilter(col, value)) {
                    //remove filter
                    filters.removeFilter(col, value);

                } else {
                    // add filter
                    if (value.indexOf(" - ") > 0 && value.indexOf("%") > value.indexOf(" - ")) {
                        value = value.substring(0, value.indexOf(" - ")).trim();
                    }
                    filters.setFilter(col, value);

                }
                chart.drawChart(conf);
            }
        });
    }

    public static Chart createBarChart(SpreadSheet s, final SpreadsheetSettings settings, int col, SpreadsheetFilters filters, boolean allowFiltering) {
        Chart chart = createChart(ChartType.BAR);
        Configuration conf = chart.getConfiguration();

        conf.setTitle(createChartTitle(settings.getTitle(col)));
        conf.setSubTitle(createSubTitle(s, col, filters));
        conf.setTooltip(createTooltip("{series.name}: {point.percentage}%", 1));

        configureBarAxis(conf.getxAxis(), conf.getyAxis());

        conf.setPlotOptions(createBarPlotOptions());
        XAxis x = conf.getxAxis();
        x.setAllowDecimals(false);

        final DataSeries series = new DataSeries();

        // Do not filter if we already have filter for this column (use empty filter instead)
        List<SpreadSheet.AggregateValue> vals = null;
        if (settings.isMultiselect(col)) {
            vals = s.aggretageMultiselectGroups(col, filters);
        } else {
            vals = s.aggretageGroups(col, filters);
        }

        Collections.sort(vals, Collections.reverseOrder(getComparator(settings, col)));

        Color[] colors = getVaadinColors(vals.size(), false);

        int i = 0;
        for (SpreadSheet.AggregateValue a : vals) {
            final DataSeriesItem dataSeriesItem = new DataSeriesItem(a.title, a.value);
            series.add(dataSeriesItem);
            dataSeriesItem.setColor(colors[i++]);
        }
        conf.setSeries(series);

        SpreadsheetSettings.ScaleDisplay d = getScaleDisplay(settings, col);
        if (d == SpreadsheetSettings.ScaleDisplay.PERCENTAGE) {
            //TODO: configure label display style      
        }

        addFilterClickListener(chart, series, filters, col, conf, s, allowFiltering);

        chart.drawChart(conf);

        return chart;
    }

    public static Chart createLineChart(SpreadSheet s, final SpreadsheetSettings settings, int col, SpreadsheetFilters filters, boolean allowFiltering) {
        Chart chart = createChart(ChartType.SPLINE);
        Configuration conf = chart.getConfiguration();

        conf.setTitle(createChartTitle(settings.getTitle(col)));
        conf.setSubTitle(createSubTitle(s, col, filters));

        conf.setTooltip(createTooltip("{point.name}: {point.value}%", 1));

        PlotOptionsLine plotOptions = new PlotOptionsLine();
        plotOptions.setAllowPointSelect(false);
        plotOptions.setShowInLegend(false);
        Labels dataLabels = new Labels();
        dataLabels.setEnabled(true);
        conf.setPlotOptions(plotOptions);
        conf.getyAxis().setTitle("Responses");

        plotOptions.setShadow(false);
        plotOptions.setLineWidth(5);
        plotOptions.setColor(new SolidColor(0, 180, 240));
        conf.getxAxis().setLineColor(new SolidColor(0, 180, 240));
        plotOptions.setMarker(new Marker(false));

        final DataSeries series = new DataSeries();
        plotOptions.setDataLabels(new Labels()); //FIXME     
        plotOptions.getDataLabels().setStyle(createLabelStyle());

        // Do not filter if we already have filter for this column (use empty filter instead)
        List<SpreadSheet.AggregateValue> vals = s.aggretageGroups(col, filters);

        Color[] colors = getVaadinColors(vals.size(), false);

        int i = 0;
        for (SpreadSheet.AggregateValue a : vals) {
            final DataSeriesItem dataSeriesItem = new DataSeriesItem(a.title, a.value);
            series.add(dataSeriesItem);
            dataSeriesItem.setColor(colors[i++]);
        }
        conf.setSeries(series);

        addFilterClickListener(chart, series, filters, col, conf, s, allowFiltering);

        chart.drawChart(conf);

        return chart;
    }

    public static final String HELVETICA_ROUNDED_BOLD = "HelveticaW01-RoundedBd";
    public static final String HELVETICA_LIGHT = "Helvetica W01 Light";

    private void createUI() {
        removeAllComponents();
        setSizeFull();
        Chart aChart = getChart();
        String positionString = "left:0;bottom:0;top:0;right:40%;";
        if (aChart.getConfiguration().getChart().getType() == ChartType.PIE) {
            positionString = "left:0;bottom:0;top:0;right:40%;";
        }
        addComponent(aChart, positionString);

        addComponent(getDisplayTypes(), "bottom:10px;right:210px;");

        Label f = createTitle(settings, filters, index);
        if (filters.hasFilters()) {
            addComponent(f, "left:57%;bottom:50px;right:30px;top:10px;");
        } else {
            addComponent(f, "left:57%;bottom:30%;right:30px;top:20%;");
        }

        Link f3 = createCreditsLink();
        addComponent(f3, "bottom:10px;right:30px;");

    }

    private Component getDisplayTypes() {

        if (displayTypes == null) {
            displayTypes = new ButtonList();
            displayTypes.addStyleName("types");

            SpreadsheetSettings.ColumnDisplayType[] types = settings.getDisplayTypes(this.index);
            for (SpreadsheetSettings.ColumnDisplayType colDisplay : types) {
                displayTypes.addItem(colDisplay, colDisplay.userString());
            }
            displayTypes.setValue(type); // current one

            displayTypes
                    .addValueChangeListener(new Property.ValueChangeListener() {

                        @Override
                        public void valueChange(Property.ValueChangeEvent event) {
                            type = (SpreadsheetSettings.ColumnDisplayType) event.getProperty().getValue();
                            chart = null;
                            createUI();
                        }
                    });

            displayTypes.setVisible(types.length > 1);
        }
        return displayTypes;
    }

    private Label createTitle(SpreadsheetSettings settings, SpreadsheetFilters filters, int col) {
        String t = settings.getTitle(col);
        t = t.replaceAll("Vaadin", "<b>Vaadin</b>");

        String filterText = filters.getText(true, settings);
        if (filterText != null && !"".equals(filterText.trim())) {
            t = "<div class=\"filter-text\">" + filterText + "</div><br />" + t;
        }
        Label l = new Label(t, ContentMode.HTML);
        l.setSizeUndefined();
        l.setStyleName("big-title");
        l.setSizeFull();

        // We scale the font to fit the actual size
        FontScaleToFit.label(l, 20, 100);

        return l;
    }

    private Link createCreditsLink() {
        Link l = new Link("made with vaadin.com/charts", new ExternalResource("https://vaadin.com/charts"));
        l.setStyleName("credits");
        return l;
    }

}
