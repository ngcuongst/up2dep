package util.remoteDb;

import com.intellij.openapi.components.ServiceManager;
import constants.Server;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import service.localService.LocalDataService;
import telemetry.LocalLog;
import telemetry.PluginHelper;
import util.FileHelper;
import util.ZipHelper;

import javax.net.ssl.HttpsURLConnection;


public class RemoteDbHelper {
    private static final String DB_SERVICE_URL = Server.SERVICE_URL + "database/";
    private static final LocalLog logger = LocalLog.getInstance();

    public static String checkDatabase() {

        String dataFolder = null;
        String servicePoint = DB_SERVICE_URL + "index";
        try {
            String pluginId = PluginHelper.getPluginId();
            final LocalDataService service = ServiceManager.getService(LocalDataService.class);
            int version = service.getDataVersion();
            String dataDir = service.getDataFolder();
            File dataDirFile;

            if (dataDir == null) {
                dataDirFile = FileHelper.createDataFolder();
                dataDir = dataDirFile.getAbsolutePath();
                service.setDataFolder(dataDir);
            } else {
                dataDirFile = new File(dataDir);
                // folder is removed manually by developers
                if (!dataDirFile.exists()){
                    dataDirFile.mkdirs();
                }
            }

            // check whether the data folder is empty to decide to download data.zip file from server
            int shouldDownload = 0;
            if (dataDirFile.listFiles().length == 0){
                shouldDownload = 1;
            }

            servicePoint = servicePoint + "/" + pluginId + "/" + version + "/" + shouldDownload;
            URL url = new URL(servicePoint);
            HttpURLConnection conn;
            conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                int newerVersion = Integer.parseInt(conn.getHeaderField("version"));
                if (newerVersion > version || shouldDownload == 1) {
                    boolean success = ZipHelper.unZip(conn.getInputStream(), dataDirFile);
                    if (success) {
                        service.setDataVersion(newerVersion);
                    }
                }

            } else if (status / 100 != 2){
                //TODO log connection error
                logger.writeLine("Error connection to web service url " + servicePoint + " with status " + status);
            }

            conn.disconnect();
        } catch (Exception e) {
            logger.writeLine(e);
        }

        return dataFolder;
    }
}
