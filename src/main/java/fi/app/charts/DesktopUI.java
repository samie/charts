package fi.app.charts;

import com.vaadin.annotations.StyleSheet;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.Property;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.vaadin.addon.googlepicker.GooglePicker;
import org.vaadin.addon.googlepicker.auth.GoogleAuthorizer.AuthorizationListener;
import org.vaadin.addons.idle.Idle;
import org.vaadin.dialogs.ConfirmDialog;
import org.vaadin.dialogs.DefaultConfirmDialogFactory;
import org.vaadin.googleanalytics.tracking.GoogleAnalyticsTracker;
import org.vaadin.se.utils.SimpleFileCache;

/**
 * This the main UI class.
 *
 */
@SuppressWarnings("serial")
@Widgetset("fi.app.charts.gwt.DesktopWidgetSet")
@Theme("desktop")
@StyleSheet("http://fast.fonts.net/cssapi/e52198f4-e2bf-4f47-913d-d6a2747493f7.css")
public class DesktopUI extends UI implements AuthorizationListener {

    private ApplicationSettings appSettings;
    private String singleChartMode;
    private String userId;

    @SuppressWarnings("serial")
    @WebServlet(urlPatterns = {"/*"}, initParams = {
        @WebInitParam(name = "productionMode", value = "false"),
        @WebInitParam(name = "ui", value = "fi.app.charts.DesktopUI")})
    public static class SpreadsheetServlet extends VaadinServlet {

        /**
         * Handle CORS requests
         */
        @Override
        protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

            // Origin is needed for all CORS requests
            String origin = request.getHeader("Origin");
            if (origin != null && isAllowedRequestOrigin(origin)) {

                // Handle a preflight "option" requests
                if ("options".equalsIgnoreCase(request.getMethod())) {
                    response.addHeader("Access-Control-Allow-Origin", origin);
                    response.setHeader("Allow", "GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS");

                    // allow the requested method
                    String method = request.getHeader("Access-Control-Request-Method");
                    response.addHeader("Access-Control-Allow-Methods", method);

                    // allow the requested headers
                    String headers = request.getHeader("Access-Control-Request-Headers");
                    response.addHeader("Access-Control-Allow-Headers", headers);

                    response.addHeader("Access-Control-Allow-Credentials", "true");
                    response.setContentType("text/plain");
                    response.setCharacterEncoding("utf-8");
                    response.getWriter().flush();
                    return;
                } // Handle UIDL post requests
                else if ("post".equalsIgnoreCase(request.getMethod())) {
                    response.addHeader("Access-Control-Allow-Origin", origin);
                    response.addHeader("Access-Control-Allow-Credentials", "true");
                    super.service(request, response);
                    return;
                }
            }

            // All the other requests nothing to do with CORS 
            super.service(request, response);

        }

        /**
         * Check that the page Origin header is allowed.
         */
        private boolean isAllowedRequestOrigin(String origin) {
            return origin.matches(".*");
        }

    }

    private static final String PARAM_SINGLE_CHART = "singlechart";
    private static final String PARAM_AUTHORING = "authKey";
    private static final String GOOGLE_SPREADSHEET_SCOPE = "https://spreadsheets.google.com/feeds/";
    private static final String GOOGLDRIVE_URL_BASE = "https://docs.google.com/a/vaadin.com/spreadsheet/ccc?key=";
    private static final String APP_CONF_DIR = "spreadsheet-charts-conf/";
    private static final String APP_CONF_FILE_NAME = "application-config.properties";
    private static final String USER_CONF_FILE_NAME = "user-config.properties";

    private SimpleFileCache cache;

    private String accessToken;
    private GooglePicker picker;
    private boolean authorized;
    private VerticalLayout dataLayout;
    private boolean authoringMode;
    private Link docLink;
    private HorizontalLayout authoringLayout;
    private GoogleAnalyticsTracker tracker;
    private String applicationId;
    private ButtonList filterButtons;
    private Button pickFromDrive;
    private Button reloadButton;
    private Button settingsButton;
    private Button saveButton;
    private ButtonList dataButtons;
    private boolean ignoreUpdates;
    private CheckBox publishButton;

    private SpreadSheet spreadSheet;
    private SpreadsheetSettings documentSettings;
    private SpreadsheetFilters filters;

    private String documentId;
    private UserSettings userSettings;
    private String driveDocumentKey;

    Navigator navigator;

    static {
        ConfirmDialog.setFactory(new DefaultConfirmDialogFactory() {

            @Override
            public ConfirmDialog create(final String caption, final String message,
                    final String okCaption, final String cancelCaption) {
                ConfirmDialog d = super.create(caption, message, okCaption, cancelCaption);
                d.getOkButton().setStyleName("lst");
                d.getOkButton().addStyleName("selected");
                d.getCancelButton().setStyleName("lst");
                return d;
            }
        });
    }
    private Link thisLink;

    @Override
    protected void init(VaadinRequest request) {

        singleChartMode = request.getParameter(PARAM_SINGLE_CHART);

        // Application ID is the context path of the servlet
        applicationId = "app" + request.getContextPath().replaceAll("/", "_");

        // Application settings
        appSettings = loadAppSettings(applicationId);

        // User id the is the first path of request uri
        String pathInfo = null;
        if (singleChartMode != null) {
            pathInfo = singleChartMode;
        } else {
            pathInfo = request.getPathInfo() + "/" + Page.getCurrent().getUriFragment();
        }

        userId = parseUserId(pathInfo);
        documentId = parseDocumentId(pathInfo);

        // If we found user, we check for auth mode
        userSettings = loadUserSettings(userId);
        if (userSettings != null) {
            authoringMode = isAuthMode(request, userSettings);
        }

        // Google analytics tracking
        tracker = new GoogleAnalyticsTracker(appSettings.getGATrackerId());
        tracker.setAllowAnchor(true);
        tracker.setDomainName("none");
        addExtension(tracker);

        // This changes the CSS classname based on 4s user inactivity
        Idle.track(this, 4000);

        VaadinSession.getCurrent().addRequestHandler(
                new RequestHandler() {
                    @Override

                    public boolean handleRequest(VaadinSession session,
                            VaadinRequest request,
                            VaadinResponse response)
                    throws IOException {

                        //TODO: Do we need this?
                        //authoringMode = isAuthMode(request, userSettings);
                        return false; // No response was written
                    }

                });

        final VerticalLayout baseLayout = new VerticalLayout();
        baseLayout.setStyleName("base");
        baseLayout.setMargin(true);
        baseLayout.setSpacing(true);
        baseLayout.setSizeFull();

        authoringLayout = new HorizontalLayout();
        authoringLayout.setStyleName("authoringbar");
        authoringLayout.setSpacing(true);
        authoringLayout.setMargin(true);
        authoringLayout.setWidth("100%");
        authoringLayout.setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);

        // Google picker for choosing the file
        picker = new GooglePicker(appSettings.getGoogleDocsAPIKey(), appSettings.getGoogleDocsClientId());
        addExtension(picker);
        pickFromDrive = new Button("Choose from Drive", new Button.ClickListener() {

            @Override
            public void buttonClick(Button.ClickEvent event) {
                picker.pickDocument(GooglePicker.Type.SPREADSHEETS);
            }
        });
        pickFromDrive.setStyleName("lst selected");
        authoringLayout.addComponent(pickFromDrive);
        picker.addSelectionListener(new GooglePicker.SelectionListener() {

            @Override
            public void documentSelected(GooglePicker.Document document) {
                documentSettings
                        = loadDocumentSettings(userSettings, documentSettings.getSettingId(), true);
                documentSettings.setDocumentTitle(document.getName());
                documentSettings.setDocumentKey(document.getId());
                loadDocument(documentSettings);
            }
        });

        CssLayout links = new CssLayout();
        links.setSizeFull();
        authoringLayout.addComponent(links);
        authoringLayout.setExpandRatio(links, 1f);

        thisLink = new Link();
        links.addComponent(thisLink);

        docLink = new Link();
        links.addComponent(docLink);

        authoringLayout.addComponent(reloadButton = new Button("reload", new Button.ClickListener() {

            @Override
            public void buttonClick(Button.ClickEvent event) {
                if (documentSettings.getDocumentKey() != null) {
                    if (!authorized) {
                        authorizeGoogleSreadsheetScope();
                    } else {
                        loadDocumentFromDrive();
                    }
                }
                updateUI();
            }

        }));
        reloadButton.setStyleName("lst");
        authoringLayout.addComponent(settingsButton = new Button("data settings", new Button.ClickListener() {

            @Override
            public void buttonClick(Button.ClickEvent event) {
                addWindow(new SettingsWindow(documentSettings, new SettingsWindow.SaveCallback() {

                    @Override
                    public void closed(SpreadsheetSettings s) {
                        documentSettings = s;
                        updateUI();
                    }

                }));
            }
        }));
        settingsButton.setStyleName("lst");

        authoringLayout.addComponent(saveButton = new Button("save", new Button.ClickListener() {

            @Override
            public void buttonClick(Button.ClickEvent event) {
                if (documentSettings.isPublic()) {
                    ConfirmDialog.show(DesktopUI.this, "This will make document public. Continue?", new ConfirmDialog.Listener() {

                        @Override
                        public void onClose(ConfirmDialog dialog) {
                            if (dialog.isConfirmed()) {
                                documentSettings.save();
                                Notification.show("Setting saved.");
                            }
                        }
                    });
                } else {
                    documentSettings.save();
                    Notification.show("Setting saved.");

                }

            }
        }));
        saveButton.setStyleName("lst");

        publishButton = new CheckBox("Published");
        publishButton.addValueChangeListener(new Property.ValueChangeListener() {

            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                documentSettings.setPublic(publishButton.getValue());
            }
        });
        authoringLayout.addComponent(publishButton);

        baseLayout.addComponent(authoringLayout);
        authoringLayout.setVisible(authoringMode);

        dataLayout = new VerticalLayout();
        dataLayout.setSizeFull();
        baseLayout.addComponent(dataLayout);
        baseLayout.setExpandRatio(dataLayout, 1f);

        setContent(baseLayout);
        setSizeFull();

        // Enable authoring mode if permitted
        authoringLayout.setVisible(authoringMode);

        if (documentId == null && userSettings != null) {
            // Default document serving
            documentId = userSettings.getDefaultDocumentId();
        }

        if (!authoringMode) {
            documentSettings = loadDocumentSettings(userSettings, documentId, false);
            loadDocument(documentSettings);
        } else {
            // We have authoring permissions
            documentSettings = loadDocumentSettings(userSettings, documentId, true);
            loadDocument(documentSettings);

        }

    }

    private void loadDocument(SpreadsheetSettings settings) {
        if (settings == null) {
            Logger.getLogger(DesktopUI.class.getName()).log(Level.INFO, "Tried to load null document");
            updateUI();
            return;
        }

        String cacheKey = settings.getDocumentKey();
        try {
            if (cacheKey != null) {
                this.spreadSheet = (SpreadSheet) cache.get(cacheKey);
            }
        } catch (RuntimeException e) {
            Logger.getLogger(DesktopUI.class.getName()).log(Level.SEVERE, "Data file error " + cacheKey, e);
        }

        if (this.spreadSheet == null) {
            Logger.getLogger(DesktopUI.class.getName()).log(Level.INFO, "Not found in cache : " + cacheKey);
            authorizeGoogleSreadsheetScope();
        } else {
            Logger.getLogger(DesktopUI.class.getName()).log(Level.INFO, "Loaded from cache: " + cacheKey);
            if (!this.documentSettings.isInitialized()) {
                SpreadsheetSettings.copyValuesFromSpreadsheet(documentSettings, this.spreadSheet);
            }
            this.filters = new SpreadsheetFilters(spreadSheet);
            updateUI();
        }
    }

    public static boolean isAuthMode(VaadinRequest request, UserSettings userSettings) {
        String authoringKey = userSettings.getAuthoringKey();
        String reqKey = request.getParameter(PARAM_AUTHORING);
        if (reqKey != null) {
            boolean authMode = authoringKey != null && authoringKey.equals(request.getParameter(PARAM_AUTHORING));
            if (authMode) {
                Logger.getLogger(DesktopUI.class.getName()).log(Level.INFO, "Authoring mode: " + authMode);
            } else {
                Logger.getLogger(DesktopUI.class.getName()).log(Level.INFO, "Invalid authoring key: '" + reqKey + "'");
            }
            return authMode;
        }
        return false;
    }

    private void authorizeGoogleSreadsheetScope() {
        if (!authoringMode) {
            Logger.getLogger(DesktopUI.class.getName()).log(Level.INFO, "Cannot load from Drive when not in authoring mode ");
            return;
        }
        if (documentSettings == null || documentSettings.getDocumentKey() == null) {
            Logger.getLogger(DesktopUI.class.getName()).log(Level.INFO, " Cannot load from Drive when no document key defined.");
            return;
        }
        if (!this.authorized) {
            Logger.getLogger(DesktopUI.class.getName()).log(Level.INFO, "Requesting authorization for scope " + GOOGLE_SPREADSHEET_SCOPE + " with token " + accessToken);
            this.picker.authorize(GOOGLE_SPREADSHEET_SCOPE, this);
        }
    }

    /**
     * Called automatically after authorizeGoogleSreadsheetScope
     */
    public void authorized(String scope, String accessToken) {
        Logger.getLogger(DesktopUI.class.getName()).log(Level.INFO, "Authorized to scope: " + scope);
        this.accessToken = accessToken;
        this.authorized = true;
        loadDocumentFromDrive();
        updateUI();
    }

    private void loadDocumentFromDrive() {
        if (documentSettings == null || documentSettings.getDocumentKey() == null) {
            Logger.getLogger(DesktopUI.class.getName()).log(Level.INFO, " Cannot load from Drive when no document key defined.");
            return;
        }

        this.spreadSheet = SpreadSheet.loadSpreadsheetFromDrive(documentSettings.getDocumentKey(), this.accessToken);
        if (this.spreadSheet != null) {
            cache.store(documentSettings.getDocumentKey(), this.spreadSheet);
            Logger.getLogger(DesktopUI.class.getName()).log(Level.INFO, "Stored to cache: " + documentSettings.getDocumentKey());

            if (!this.documentSettings.isInitialized()) {
                SpreadsheetSettings.copyValuesFromSpreadsheet(documentSettings, this.spreadSheet);
            }
            this.filters = new SpreadsheetFilters(spreadSheet);
        }
    }

    private static SpreadsheetSettings loadDocumentSettings(UserSettings userSettings, String docId, boolean create) {
        if (userSettings == null || docId == null) {
            return null;
        }

        File confDir = getDocumentConfigDirectory(userSettings);

        if (!confDir.exists()) {
            Logger.getLogger(DesktopUI.class.getName()).log(Level.INFO, "Creating new configuration directory: " + confDir.getAbsolutePath());
            confDir.mkdirs();
        }

        return SpreadsheetSettings.load(confDir, docId, create);
    }

    private static File getDocumentConfigDirectory(UserSettings userSettings1) {
        File confDir;
        confDir = new File(userSettings1.getFile().getParent(), "conf");
        return confDir;
    }

    private void updateUI() {

        authoringLayout.setVisible(authoringMode);
        dataLayout.removeAllComponents();

        if (spreadSheet == null) {
            docLink.setResource(null);
            docLink.setCaption("<no document selected>");
            Logger.getLogger(DesktopUI.class.getName()).log(Level.SEVERE, "No spreadsheet loaded.");
            return;
        }

        if (authoringMode) {
            publishButton.setValue(documentSettings.isPublic());
            docLink.setResource(new ExternalResource(GOOGLDRIVE_URL_BASE + spreadSheet.getDocumentKey()));
            docLink.setCaption((GOOGLDRIVE_URL_BASE + spreadSheet.getDocumentKey()).substring(8));
            String publicUri = Page.getCurrent().getLocation().toASCIIString();
            publicUri = publicUri.substring(0, publicUri.indexOf("?"));
            thisLink.setResource(new ExternalResource(publicUri));
            thisLink.setCaption((documentSettings.isPublic() ? "Public Chart URL: " : "Private Chart URL: ") + publicUri.substring(publicUri.indexOf("/") + 2));
            reloadButton.setVisible(spreadSheet != null);
            settingsButton.setVisible(spreadSheet != null);
            saveButton.setVisible(spreadSheet != null);
        } else if (!documentSettings.isPublic()) {
            dataLayout.addComponent(new Label("This data is not public. Please ask the author to see it."));
            Logger.getLogger(DesktopUI.class.getName()).log(Level.SEVERE, "Requested unpublic document '" + spreadSheet.getDocumentKey() + "'");
            return;
        }

        Page.getCurrent().setTitle(documentSettings.getDocumentTitle());

        // External Link
        if (documentSettings.getExternalLinkUrl() != null
                && documentSettings.getExternalLinkTitle() != null) {
            Link externalLink = new Link(documentSettings.getExternalLinkTitle(),
                    new ExternalResource(documentSettings.getExternalLinkUrl()));
            externalLink.setStyleName("external");
            dataLayout.addComponent(externalLink);
            dataLayout.setComponentAlignment(externalLink, Alignment.TOP_RIGHT);
        }

        dataButtons = new ButtonList();
        dataButtons.addStyleName("dataselect");
        dataButtons.addValueChangeListener(new Property.ValueChangeListener() {
            private ChartView current;

            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                SpreadsheetSettings.ColumnDisplayType displayType = null;
                if (current != null) {
                    //displayType = current.getDisplayType(); //Preserve display
                    dataLayout.removeComponent(current);
                }
                int dataColumn = (Integer) event.getProperty().getValue();
                dataLayout.addComponent(current = new ChartView(spreadSheet, documentSettings, dataColumn, filters, displayType, authoringMode));
                dataLayout.setExpandRatio(current, 1f);
                tracker.trackPageview("/chart/" + documentId + "/" + dataColumn);
                if (singleChartMode == null) {
                    Page.getCurrent().setUriFragment("" + dataColumn);
                }
            }
        });
        dataLayout.addComponent(dataButtons);

        filterButtons = new ButtonList();
        filterButtons.addStyleName("filters");
        filterButtons.addValueChangeListener(new Property.ValueChangeListener() {

            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                Object v = event.getProperty().getValue();
                if ("clear".equals(v)) {
                    ConfirmDialog.show(DesktopUI.this, "Remove all filters?", new ConfirmDialog.Listener() {

                        @Override
                        public void onClose(ConfirmDialog dialog) {
                            if (dialog.isConfirmed()) {
                                filters.clearAll();
                            }
                        }
                    });
                } else {
                    SpreadsheetFilters.Filter f = (SpreadsheetFilters.Filter) v;
                    f.setActive(!f.isActive());
                }
            }
        });
        dataLayout.addComponent(filterButtons);
        filterButtons.setVisible(authoringMode || documentSettings.isAllowFiltering());

        // Apply the first visible data
        int firstVisible = -1;
        for (int i = 0; i < spreadSheet.colCount(); i++) {
            if (documentSettings.isVisible(i)) {
                if (firstVisible < 0) {
                    firstVisible = i;
                }
                dataButtons.addItem(i, documentSettings.getTitle(i));
            }
        }

        // Single chart mode (no navigation / filtering)
        boolean singleChart = singleChartMode != null;
        if (singleChart) {
            dataButtons.setVisible(false);
        }

        int requestedData = parseDataId(singleChartMode != null ? singleChartMode : Page.getCurrent().getUriFragment());
        if (requestedData >= 0 && documentSettings.isVisible(requestedData)) {
            dataButtons.setValue(requestedData);
        } else if (firstVisible >= 0) {
            dataButtons.setValue(firstVisible);
        }

        filters.setListener(new SpreadsheetFilters.Listener() {

            @Override
            public void filterChandged(SpreadsheetFilters filters) {
                updateFilterButtons();

                // Reload the charts
                dataButtons.setValue(dataButtons.getValue());
            }

        });
    }

    private void updateFilterButtons() {
        if (!ignoreUpdates) {
            try {
                ignoreUpdates = true;
                filterButtons.removeAllComponents();
                for (SpreadsheetFilters.Filter f : filters.getAllFilters()) {
                    filterButtons.addItem(f, f.getValue(), f.getColumnName());
                    filterButtons.setSelected(f, f.isActive());
                }
                filterButtons.setVisible(filters.hasFilters());
                updateFilterHighlights();
                Button clear;
                filterButtons.addComponent(clear = new Button(null, new Button.ClickListener() {

                    @Override
                    public void buttonClick(Button.ClickEvent event) {
                        filters.clearAll();

                    }
                }));
                clear.setStyleName("clear");
                clear.setIcon(FontAwesome.EJECT);
                clear.setDescription("Clear filters");

            } finally {
                ignoreUpdates = false;
            }
        }

    }

    private void updateFilterHighlights() {
        dataButtons.removeHighlights();
        for (SpreadsheetFilters.Filter f : filters.getAllFilters()) {
            dataButtons.setHighlighted(f.getColumn(), filterButtons.isVisible() && f.isActive());
        }
    }

    private ApplicationSettings loadAppSettings(String applicationId) {
        ApplicationSettings res = new ApplicationSettings(applicationId);
        File confDir = new File(APP_CONF_DIR + applicationId + "/");
        File confFile = new File(confDir, APP_CONF_FILE_NAME);

        if (!confDir.exists()) {
            Logger.getLogger(DesktopUI.class.getName()).log(Level.INFO, "Creating new application directory: " + confDir.getAbsolutePath());
            confDir.mkdirs();
        }

        if (!confFile.exists()) {
            Logger.getLogger(DesktopUI.class.getName()).log(Level.INFO, "Creating new conf file: " + confFile.getAbsolutePath());
            try {
                confFile.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(DesktopUI.class.getName()).log(Level.SEVERE, "Failed to initialize configuration file", ex);
                return null;
            }
        }
        Logger.getLogger(DesktopUI.class.getName()).log(Level.INFO, "Loading application configuration: " + confFile.getAbsolutePath());
        res.loadFrom(confFile);

        return res;
    }

    private UserSettings loadUserSettings(String userId) {

        if (userId == null || "".equals(userId.trim())) {
            return null;
        }

        UserSettings res = new UserSettings();
        File confDir = new File(APP_CONF_DIR + applicationId + "/" + userId + "/");
        File confFile = new File(confDir, USER_CONF_FILE_NAME);
        File documentDir = new File(confDir, "docs");

        if (!confDir.exists()) {
            Logger.getLogger(DesktopUI.class.getName()).log(Level.INFO, "Creating new user directory: " + confDir.getAbsolutePath());
            confDir.mkdirs();
        }

        if (!documentDir.exists()) {
            Logger.getLogger(DesktopUI.class.getName()).log(Level.INFO, "Creating new document directory: " + documentDir.getAbsolutePath());
            documentDir.mkdirs();
        }
        cache = new SimpleFileCache(documentDir, false);
        cache.setCacheTime(SimpleFileCache.DEFAULT_CACHE_TIME * 356 * 100); // 100 years :)

        if (!confFile.exists() || !confFile.canRead()) {
            // For security reasons, we don't create files
            Logger.getLogger(DesktopUI.class.getName()).log(Level.INFO, "No user configuration found: " + confFile.getAbsolutePath());
            return null;
        }

        Logger.getLogger(DesktopUI.class.getName()).log(Level.INFO, "Loading user configuration: " + confFile.getAbsolutePath());
        res.loadFrom(confFile);

        return res;
    }

    private String parseUserId(String pathInfo) {

        if (pathInfo != null) {
            if (pathInfo.startsWith("/")) {
                pathInfo = pathInfo.substring(1);
            }
            String[] path = pathInfo.split("/");
            if (path.length > 0 && !"".equals(path[0])) {
                return path[0];
            }
        }

        return null;
    }

    private String parseDocumentId(String pathInfo) {

        if (pathInfo != null) {
            if (pathInfo.startsWith("/")) {
                pathInfo = pathInfo.substring(1);
            }
            String[] path = pathInfo.split("/");
            if (path.length >= 2 && !"".equals(path[1])) {
                return path[1];
            }
        }

        return null;

    }

    private int parseDataId(String pathInfo) {

        try {
            if (pathInfo != null) {
                if (!pathInfo.contains("/")) {
                    return Integer.parseInt(pathInfo);
                } else if (pathInfo.startsWith("/")) {
                    pathInfo = pathInfo.substring(1);
                }
                String[] path = pathInfo.split("/");
                if (path.length > 2 && !"".equals(path[2])) {
                    return Integer.parseInt(path[2]);
                }
            }
        } catch (NumberFormatException ignored) {
        }

        return -1;
    }

}
