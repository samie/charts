package fi.app.charts;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

class ApplicationProperties extends Properties {

    private File settingFile;
    private String settingId;

    protected ApplicationProperties(String settingId) {
        this.settingId = settingId;
    }

    public File getFile() {
        return settingFile;
    }

    public void setFile(File f) {
        settingFile = f;
    }

    public String getSettingId() {
        return settingId;
    }

    public String getConfFileName() {
        if (this.settingFile != null) {
            return settingFile.getName();
        } else {
            return settingId + ".properties";
        }
    }

    protected void setOrRemove(String key, String newValue) {
        if (key == null) {
            return;
        }
        if (newValue != null) {
            this.setProperty(key, newValue);
        } else {
            this.remove(key);
        }
    }

    protected boolean save() {
        FileWriter fw = null;
        try {
            if (!settingFile.exists()) {
                Logger.getLogger(DesktopUI.class.getName()).log(Level.INFO, "Configuration created: " + settingFile.getAbsolutePath());
                settingFile.createNewFile();
            }
            fw = new FileWriter(settingFile);
            this.store(fw, "Settings for " + settingId);
            return true;
        } catch (IOException ex) {
            Logger.getLogger(ApplicationSettings.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException ignored) {
                }
            }
        }
        return false;
    }

    protected boolean loadFrom(File confFile) {
        this.settingFile = confFile;
        if (!confFile.exists()) {
            Logger.getLogger(DesktopUI.class.getName()).log(Level.INFO, "Configuration not found: " + settingFile.getAbsolutePath());
            return false;
        }

        FileReader fr = null;
        try {
            fr = new FileReader(confFile);
            this.load(fr);
            return true;
        } catch (IOException ex) {
            Logger.getLogger(DesktopUI.class.getName()).log(Level.SEVERE, "Loading failed: " + confFile.getAbsolutePath(), ex);
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException ignored) {
                }
            }
        }
        return false;

    }
}
