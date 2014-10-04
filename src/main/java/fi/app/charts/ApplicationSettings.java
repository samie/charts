package fi.app.charts;

public class ApplicationSettings extends ApplicationProperties {

    private static final String KEY_GOOGLE_ANALYTICS_TRACKER_ID = "ga.trackerid";
    private static final String KEY_GOOGLE_DOCS_CLIENT_ID = "gdocs.clientid";
    private static final String KEY_GOOGLE_DOCS_API_KEY = "gdocs.apikey";

    public ApplicationSettings(String settingId) {
        super(settingId);
    }

    public String getGATrackerId() {
        return getProperty(KEY_GOOGLE_ANALYTICS_TRACKER_ID);
    }

    public void setGATrackerId(String newValue) {
        setOrRemove(KEY_GOOGLE_ANALYTICS_TRACKER_ID, newValue);
    }

    public String getGoogleDocsAPIKey() {
        return getProperty(KEY_GOOGLE_DOCS_API_KEY);
    }

    public void setGoogleDocsAPIKey(String newValue) {
        setOrRemove(KEY_GOOGLE_DOCS_API_KEY, newValue);
    }

    public String getGoogleDocsClientId() {
        return getProperty(KEY_GOOGLE_DOCS_CLIENT_ID);
    }

    public void setGoogleDocsClientId(String newValue) {
        setOrRemove(KEY_GOOGLE_DOCS_CLIENT_ID, newValue);
    }

}
