package telemetry;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import service.localService.LocalDataService;
import util.FileHelper;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;

public class LocalLog {
    private static Logger logger = Logger.getLogger(LocalLog.class.getName());
    private static FileHandler fileHandler;
    private static LocalLog instance;

    private LocalLog() {
        try {
            String filePath = getLogFile();
            fileHandler = new FileHandler(filePath, true);
            SimpleFormatter simpleFormatter = new SimpleFormatter();
            fileHandler.setFormatter(simpleFormatter);
            fileHandler.setLevel(Level.ALL);


            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static LocalLog getInstance() {
        if (instance == null) {
            instance = new LocalLog();
        }
        return instance;
    }

    public String getLogFile() {
        final LocalDataService service = ServiceManager.getService(LocalDataService.class);
        String logFolder = service.getLogFolder();
        if (logFolder == null) {
            File dataFolder = FileHelper.createDataFolder();
            logFolder = dataFolder.getPath();
            service.setLogFolder(logFolder);
        } else {
            File file = new File(logFolder);
            if (!file.exists()) {
                file.mkdir();
            }
        }
        String FILE_NAME = "Up2Dep.log";
        return String.format("%s%s%s", logFolder, File.separator, FILE_NAME);
    }

    public void writeLine(Exception exception) {
        if (exception instanceof ProcessCanceledException) {
            return;
        }
        logger.log(Level.SEVERE, exception.getMessage(), exception);
    }

    public void writeLine(String message) {
        logger.info(message);
    }

    public void writeLine(Level level, String message) {
        logger.log(level, message);
    }


}
