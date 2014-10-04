package fi.app.charts;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User-level settings.
 */
class UserSettings extends Properties {

    private static final String KEY_AUTH_KEY = "authKey";
    private static final String KEY_DEFAULT_DOC_ID = "default_document";
    private File confFile;

    public void save() {
        FileWriter fw = null;
        try {
            fw = new FileWriter(confFile);
            this.store(fw, "Application settings");
        } catch (IOException ex) {
            Logger.getLogger(UserSettings.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fw.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void loadFrom(File confFile) {
        this.confFile = confFile;
        FileReader fr = null;
        try {
            fr = new FileReader(confFile);
            this.load(fr);
        } catch (IOException ex) {
            Logger.getLogger(DesktopUI.class.getName()).log(Level.SEVERE, "Loading failed: " + confFile.getAbsolutePath(), ex);
            return;
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public String getDefaultDocumentId() {
        return getProperty(KEY_DEFAULT_DOC_ID);
    }

    public void setDefaultDocumentId(String spreadsheetId) {
        if (spreadsheetId != null) {
            this.setProperty(KEY_DEFAULT_DOC_ID, spreadsheetId);
        } else {
            this.remove(KEY_DEFAULT_DOC_ID);
        }
    }

    String getAuthoringKey() {
        String key = getProperty(KEY_AUTH_KEY);
        return key;
    }

    String getDocumentKey(String documentId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    File getFile() {
        return this.confFile;
    }
}
