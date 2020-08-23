package telemetry;

import com.intellij.openapi.components.ServiceManager;
import service.localService.LocalDataService;

import java.math.BigInteger;
import java.security.SecureRandom;

public class PluginHelper {
    private static final SecureRandom random = new SecureRandom();
//    private static final LocalLog log = LocalLog.getInstance();
    private static String pluginId;

    public static String getRandomString() {
        return new BigInteger(130, random).toString(32);
    }


    public static void  checkPluginId() {
        try {
            final LocalDataService localDataService = ServiceManager.getService(LocalDataService.class);
            if (localDataService != null) {
                pluginId = localDataService.getPluginId();
                if (pluginId == null) {
                    pluginId = getRandomString();
                    localDataService.setPluginId(pluginId);
                }


            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String getPluginId() {
        return pluginId;
    }
}
