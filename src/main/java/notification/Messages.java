package notification;

import constants.Server;

public class Messages {
    public static final String OUTDATED = "There is a newer version (%s), consider upgrading this dependency";
    public static final String VULNERABLE_OUTDATED = "<html><body>This version contains security vulnerability, more details <a href='VULNERABILITY_DETAILS'>here</a>. Consider upgrading it to a newer version or avoid using it.</body></html>";
    public static final String INSECURE_API_USED = "<html><body>You are using an insecure (cryptographic misuse) API from this library, this makes your app vulnerable, check our <a href='" + Server.HOME_SITE + "report/LIBRARY_NAME.html'>website</a> for details.</body></html>";

    public static String getMessage(String libName, WarningType warningType, String latestVersion, String url) {
        switch (warningType) {
            case INSECURE_API_USED:
                return Messages.INSECURE_API_USED.replace("LIBRARY_NAME", libName);
            case OUTDATED:
                return String.format(Messages.OUTDATED, latestVersion);
            case VULNERABLE_OUTDATED:
                return Messages.VULNERABLE_OUTDATED.replace("VULNERABILITY_DETAILS", url);
            default:
                return null;
        }
    }
}
