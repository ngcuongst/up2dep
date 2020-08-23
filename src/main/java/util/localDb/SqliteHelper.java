package util.localDb;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import constants.FileConstants;
import model.ApiInfo;
import model.CryptoIssue;
import model.gradle.GradleCoordinate;
import model.gradle.LocalGradleCoordinate;
import model.gradle.TransitiveLocalGradleCoordinate;
import service.localService.LocalDataService;

import util.FileHelper;

import java.io.*;
import java.sql.*;
import java.util.*;

public class SqliteHelper {
    private static final String MAIN_DB_FILE = "main.sqlite";
    private static final String JDBC_PREFIX = "jdbc:sqlite:";
    private static final String API_SEPARATOR = "|";
    private static SqliteHelper instance;
    private static Logger LOG = Logger.getInstance(SqliteHelper.class);
    


    public static SqliteHelper getInstance() {
        if (instance == null) {
            instance = new SqliteHelper();
        }

        return instance;
    }

    public SqliteHelper() {
        try {
            
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            LOG.warn(e.getMessage());
        }

        final LocalDataService service = ServiceManager.getService(LocalDataService.class);
        String dataDir = service.getDataFolder();
        boolean needToSetup = false;
        if (dataDir == null) {
            needToSetup = true;
        } else {
            File dataFolder = new File(dataDir);
            if (dataFolder.exists() && dataFolder.listFiles().length == 0) {
                needToSetup = true;
            }
        }
        if (needToSetup) {
            setUpDataFolder();
        }
    }

    public String getSqlFile(String libName) {
        try {
            final LocalDataService service = ServiceManager.getService(LocalDataService.class);
            String dataFolder = service.getDataFolder();
            File dataDir = new File(dataFolder);
            if (!dataDir.exists()) {
                dataFolder = setUpDataFolder();
            }
            String filePath = dataFolder + File.separator + "data" + File.separator + libName;
            File dbFile = new File(filePath);
            if (dbFile.exists()) {
                return JDBC_PREFIX + dataFolder + File.separator + "data" + File.separator + libName;
            }

        } catch (Exception ex) {
            
        }

        return null;
    }


    public String isVulnerableVersion(GradleCoordinate currentCoordinate) {
        String url = null;
        try {
            String libName = currentCoordinate.getName();
            String sqliteFile = getSqlFile(libName);
            if (sqliteFile != null) {
                Connection connection = DriverManager.getConnection(sqliteFile);
                String query = "select is_vulnerable, url from lib_details where version = ?";
                PreparedStatement statement = connection.prepareStatement(query);
                statement.setString(1, currentCoordinate.getRevision());
                java.sql.ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    int vulnerable = resultSet.getInt("is_vulnerable");
                    if (vulnerable == 1) {
                        url = resultSet.getString("url");
                    }
                }
            }

        } catch (Exception ex) {
            
        }

        return url;
    }


    public ArrayList<LocalGradleCoordinate> getLaterVersions(String libName, String currentVersion) {
        ArrayList<LocalGradleCoordinate> laterVersions = new ArrayList<>();
        Set<LocalGradleCoordinate> tmpLaterVersions = new HashSet<>();
        int currentVersionOrder = -1;

        try {
            String sqliteFile = getSqlFile(libName);
            if (sqliteFile != null) {
                Connection connection = DriverManager.getConnection(sqliteFile);
                String query = "select version, version_order from lib_details where is_vulnerable = 0";
                PreparedStatement statement = connection.prepareStatement(query);
                java.sql.ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    int versionOrder = resultSet.getInt("version_order");
                    String version = resultSet.getString("version");
                    if (version.equals(currentVersion)) {
                        currentVersionOrder = versionOrder;

                    }
                    String[] libNameParts = libName.split(FileConstants.NEW_GROUP_ARTIFACT_SEPARATOR);
                    String groupId = libNameParts[0];
                    String artifactId = libNameParts[1];

                    LocalGradleCoordinate laterVersion = new LocalGradleCoordinate(groupId, artifactId, version, versionOrder);
                    tmpLaterVersions.add(laterVersion);


                }
                connection.close();
            }
        } catch (Exception ex) {
            
        }

        for (LocalGradleCoordinate localGradleCoordinate : tmpLaterVersions) {
            if (localGradleCoordinate.getVersionOrder() > currentVersionOrder) {
                laterVersions.add(localGradleCoordinate);
            }
        }


        return laterVersions;
    }

    public LocalGradleCoordinate getLatestVersion(String libName, String currentVersion) {
        LocalGradleCoordinate latestVersion = null;
        String groupId = null;
        String artifactId = null;
        String maxVersion = currentVersion;
        int maxOrder = 0;
        try {
            String sqliteFile = getSqlFile(libName);
            if (sqliteFile != null) {
                Connection connection = DriverManager.getConnection(sqliteFile);
                String query = "select version, version_order from lib_details where is_vulnerable = 0";
                PreparedStatement statement = connection.prepareStatement(query);
                java.sql.ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    int version_order = resultSet.getInt("version_order");
                    String version = resultSet.getString("version");
                    if (maxOrder < version_order) {
                        maxOrder = version_order;
                        maxVersion = version;
                    }
                }
                connection.close();
            }
        } catch (Exception ex) {
            
        }
        String[] libNameParts = libName.split(FileConstants.NEW_GROUP_ARTIFACT_SEPARATOR);
        groupId = libNameParts[0];
        artifactId = libNameParts[1];

        latestVersion = new LocalGradleCoordinate(groupId, artifactId, maxVersion);

        return latestVersion;
    }

    public HashMap<String, LocalGradleCoordinate> getLatestVersion(GradleCoordinate currentCoordinate, Set<GradleCoordinate> dependingLibs) {
        HashMap<String, LocalGradleCoordinate> latestVersions = new HashMap<>();
        for (GradleCoordinate lib : dependingLibs) {
            LocalGradleCoordinate latestVersion = null;
            String groupId;
            String artifactId;
            String maxVersion = null;
            int maxOrder = 0;

            try {
                String sqliteFile = getSqlFile(lib.getName());
                if (sqliteFile != null) {
                    Connection connection = DriverManager.getConnection(sqliteFile);
                    // UPDATE
//                    String query1 = "select version, version_order from lib_details where is_vulnerable = 0 order by version_order limit 1";
//                    int currentOrder = retrieveVersionOrder(sqliteFile, currentCoordinate.getRevision());
//                    if (currentOrder == 0){
//                        // current version is not found in our DB, which means it is the latest version just being released
//
//                    }

                    String query = "select version, version_order from lib_details where is_vulnerable = 0";
                    PreparedStatement statement = connection.prepareStatement(query);
                    java.sql.ResultSet resultSet = statement.executeQuery();
                    while (resultSet.next()) {
                        int version_order = resultSet.getInt("version_order");
                        String version = resultSet.getString("version");
                        if (maxVersion == null) {
                            maxVersion = version;
                            maxOrder = version_order;
                        } else {
                            if (maxOrder < version_order) {
                                maxOrder = version_order;
                                maxVersion = version;
                            }
                        }
                    }
                    //TODO handle group_id, artifact_id change in newer versions
                    connection.close();
                }
            } catch (Exception ex) {
                
            }
//        if (groupId == null && artifactId == null) {
            groupId = currentCoordinate.getGroupId();
            artifactId = currentCoordinate.getArtifactId();
//        }

            if (maxVersion != null) {
                latestVersion = new LocalGradleCoordinate(groupId, artifactId, maxVersion.toString());
            }
            latestVersions.put(lib.getName(), latestVersion);
        }
        return latestVersions;
    }

    private static final String OBJECT_TYPE = "Ljava/lang/Object;";

    public String getAlternativeAPI(String libName, String api) {
        String alternativeApi = null;
        String clonedAPI = api;
        String condition = " = ? ";
        if (api.contains(OBJECT_TYPE)) {
            clonedAPI = api.replace(OBJECT_TYPE, "%");
            condition = " like ? ";
        }
        try {
            String sqliteFile = getSqlFile(libName);
            if (sqliteFile != null) {
                Connection connection = DriverManager.getConnection(sqliteFile);
                StringBuilder query = new StringBuilder();
                query.append("select alternative_api from lib_alternatives ");
                query.append("inner join lib_details on lib_details.version = lib_alternatives.version ");
                query.append("where lib_alternatives.removed_api " + condition);
                query.append("and lib_details.is_vulnerable= 0;");
                String apiQuery = query.toString();
                PreparedStatement statement = connection.prepareStatement(apiQuery);
                statement.setString(1, clonedAPI);
                java.sql.ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    alternativeApi = resultSet.getString("alternative_api");
                }
                connection.close();
            }

        } catch (Exception ex) {
            
        }

        return alternativeApi;
    }

    public HashMap<String, Set<TransitiveLocalGradleCoordinate>> getDependingLibs(LocalGradleCoordinate mainLib) {
        return getDependingLibs(mainLib, null, 0);
    }

    private static int RECURSION_LIMIT = 3;

    private HashMap<String, Set<TransitiveLocalGradleCoordinate>> getDependingLibs(LocalGradleCoordinate mainLib, LocalGradleCoordinate parent, int loopCount) {
        HashMap<String, Set<TransitiveLocalGradleCoordinate>> dependingLibs = new HashMap<>();
        // containing itself
        //dependingLibs.add(mainLib);
        try {
            String sqliteFile = getSqlFile(MAIN_DB_FILE);
            if (sqliteFile != null) {
                Connection connection = DriverManager.getConnection(sqliteFile);
                String apiQuery = "select version1, lib_name2, version2 from lib_dependencies_summary where lib_name1 = ?";
                PreparedStatement statement = connection.prepareStatement(apiQuery);
                statement.setString(1, mainLib.getName());
//                statement.setString(2, mainLib.getRevision());
                java.sql.ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    String version1 = resultSet.getString("version1");
                    String libName2 = resultSet.getString("lib_name2");
                    String[] libNameParts = libName2.split(FileConstants.NEW_GROUP_ARTIFACT_SEPARATOR);
                    String groupId = libNameParts[0];
                    String artifactId = libNameParts[1];
                    String version2 = resultSet.getString("version2");
                    //TODO currently version2 can be null
                    if (version2 == null) {
                        continue;
                    }
                    LocalGradleCoordinate mainLibCopy = new LocalGradleCoordinate(mainLib.getGroupId(), mainLib.getArtifactId(), version1);
                    TransitiveLocalGradleCoordinate gradleCoordinate = new TransitiveLocalGradleCoordinate(groupId, artifactId, version2, mainLibCopy);

                    if (dependingLibs.containsKey(gradleCoordinate.getName())) {
                        dependingLibs.get(gradleCoordinate.getName()).add(gradleCoordinate);
                    } else {
                        HashSet<TransitiveLocalGradleCoordinate> transitiveLocalGradleCoordinates = new HashSet<>();
                        transitiveLocalGradleCoordinates.add(gradleCoordinate);
                        dependingLibs.put(gradleCoordinate.getName(), transitiveLocalGradleCoordinates);
                    }

                    if (loopCount <= RECURSION_LIMIT) {
                        loopCount += 1;
                        HashMap<String, Set<TransitiveLocalGradleCoordinate>> nextDependingLibs = getDependingLibs(gradleCoordinate, mainLib, loopCount);
                        if (nextDependingLibs.size() > 0) {
                            dependingLibs.putAll(nextDependingLibs);
                        }
                    }


                }
            }
        } catch (Exception ex) {
            
        }

        return dependingLibs;
    }


    public HashMap<String, HashSet<String>> getLibAvailableAPIs(GradleCoordinate libDetail) {

        HashMap<String, HashSet<String>> apiVersions = new HashMap<>();
        String currentVersion = libDetail.getRevision();
        try {
            String sqliteFile = getSqlFile(libDetail.getName());
            if (sqliteFile == null) {
                return null;
            }
            Connection connection = DriverManager.getConnection(sqliteFile);

            String apiQuery = "select api, versions from lib_detail_apis";
            PreparedStatement statement = connection.prepareStatement(apiQuery);
            java.sql.ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String api = resultSet.getString("api");
                String versions = resultSet.getString("versions");
                String[] split = versions.split(",");
                HashSet<String> versionSet = new HashSet<>();
//                boolean startChecking = false;
                for (String checkingVersion : split) {
//                    if (startChecking) {
                    versionSet.add(checkingVersion);
//                    }
//                    if (checkingVersion.trim().equalsIgnoreCase(currentVersion.trim())) {
//                        startChecking = true;
//                    }
                }

                apiVersions.put(api.trim(), versionSet);
            }

            connection.close();
        } catch (Exception ex) {
            
        }

        return apiVersions;
    }

    /*
        HashMap<String, HashMap<String, HashSet<String>>> = <LibName,<API, Versions>>
     */
    public HashMap<String, HashMap<String, HashSet<String>>> getLibAvailableAPIs(HashMap<String, ?> dependingLibs) {
        HashMap<String, HashMap<String, HashSet<String>>> libApiVersions = new HashMap<>();
        for (Map.Entry entry : dependingLibs.entrySet()) {
            GradleCoordinate dependingLib = (GradleCoordinate) entry.getValue();
            HashMap<String, HashSet<String>> apiVersions = getLibAvailableAPIs(dependingLib);
            if (apiVersions != null) {
                libApiVersions.put(dependingLib.getName(), apiVersions);
            }
        }

        return libApiVersions;
    }

    public boolean isLibFileExist(String libName) {
        String sqliteFile = getSqlFile(libName);
        return sqliteFile != null;
    }

    public HashSet<String> getDependees(String libName, String version, String api) {
        HashSet<String> dependees = null;
        try {
            String sqliteFile = getSqlFile(libName);
            if (sqliteFile != null) {
                Connection connection = DriverManager.getConnection(sqliteFile);
                String query = "select dependee from lib_dependencies where lib_version = ? and api = ?";
                PreparedStatement statement = connection.prepareStatement(query);
                statement.setString(1, version);
                statement.setString(2, api);
                java.sql.ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    String depependee = resultSet.getString("dependee");
                    dependees.add(depependee);
                }
            }
        } catch (Exception ex) {
            
        }

        return dependees;
    }
    /*
    TODO 1. find out current lib version
    TODO 2. find out common versions among dependencies
     */

    public ApiInfo getApiInfo(Project project, Set<String> otherLibs, String api) {
        ApiInfo apiInfo = null;
        try {
            for (String libName : otherLibs) {
                String sqliteFile = getSqlFile(libName);
                if (sqliteFile != null) {
                    Connection connection = DriverManager.getConnection(sqliteFile);
                    String query = "select versions from lib_detail_apis "
                            + " where api = ?";
                    PreparedStatement statement = connection.prepareStatement(query);
                    statement.setString(1, api);
                    java.sql.ResultSet resultSet = statement.executeQuery();
                    HashMap<String, HashSet<String>> libVersions = new HashMap<>();
                    while (resultSet.next()) {
                        String versionsStr = resultSet.getString("versions");
                        String[] split = versionsStr.split(",");
                        HashSet<String> versions = new HashSet<>(Arrays.asList(split));
                        libVersions.put(libName, versions);
                    }
                    apiInfo = new ApiInfo(project, api, libVersions);
                    //TODO currently ignore the rest e.g may be 1 API belongs to multiple libraries
                    break;
                }
            }
        } catch (Exception ex) {
            
        }
        return apiInfo;

    }

    private String setUpDataFolder() {
        String dataFolderPath = null;
        try {
            File dataFolder = FileHelper.createDataFolder();
            dataFolderPath = dataFolder.getPath();

            final LocalDataService service = ServiceManager.getService(LocalDataService.class);
            service.setDataFolder(dataFolderPath);

        } catch (Exception ex) {
            
        }
        return dataFolderPath;

    }


    public HashMap<String, CryptoIssue> getIssues(GradleCoordinate libDetail) {
        HashMap<String, CryptoIssue> issues = new HashMap<>();
        try {
            String sqliteFile = getSqlFile(libDetail.getName());
            if (sqliteFile != null) {
                Connection connection = DriverManager.getConnection(sqliteFile);

                String tableExist = "SELECT name FROM sqlite_master WHERE type='table' AND name='lib_version_issue'";
                Statement nonParramStmt = connection.createStatement();
                ResultSet tableExistResult = nonParramStmt.executeQuery(tableExist);
                if (tableExistResult.next()) {
                    String query = "select clazz, method, rule_name, statement, details from lib_version_issue where version = ?";
                    PreparedStatement statement = connection.prepareStatement(query);
                    statement.setString(1, libDetail.getRevision());
                    java.sql.ResultSet resultSet = statement.executeQuery();
                    while (resultSet.next()) {
                        String iclazz = resultSet.getString("clazz");
                        String imethod = resultSet.getString("method");
                        String irule_name = resultSet.getString("rule_name");
                        String istatement = resultSet.getString("statement");
                        String idetails = resultSet.getString("details");
                        CryptoIssue issue = new CryptoIssue(libDetail.getRevision(), iclazz, imethod, irule_name, istatement, idetails);

                        issues.put(imethod, issue);
                    }
                }
            }

        } catch (Exception ex) {
            
        }

        return issues;
    }

    public HashMap<String, Integer> getVersionOrders(String libName) {
        HashMap<String, Integer> versionOders = new HashMap<>();
        try {

            String sqliteFile = getSqlFile(libName);
            if (sqliteFile != null) {
                Connection connection = DriverManager.getConnection(sqliteFile);
                String query = "select version, version_order from lib_details";
                PreparedStatement statement = connection.prepareStatement(query);
                java.sql.ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    String version = resultSet.getString("version");
                    int versionOder = resultSet.getInt("version_order");
                    versionOders.put(version, versionOder);
                }
                connection.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return versionOders;
    }

    public int compareVersions(String libName, String versionA, String versionB) {
        try {
            if (versionA.trim().equalsIgnoreCase(versionB.trim())) {
                return 0;
            } else {
                String sqliteFile = getSqlFile(libName);
                if (sqliteFile != null) {
                    Connection connection = DriverManager.getConnection(sqliteFile);
                    String query = "select version, version_order from lib_details where version = ? or version = ?";
                    PreparedStatement statement = connection.prepareStatement(query);
                    statement.setString(1, versionA);
                    statement.setString(2, versionB);
                    java.sql.ResultSet resultSet = statement.executeQuery();
                    while (resultSet.next()) {
                        String minVersion = resultSet.getString("version");
                        if (versionA.trim().equals(minVersion.trim())) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }
                    connection.close();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int retrieveVersionOrder(String sqliteFile, String currentVersion) {
        int version_order = 0;
        try {
            Connection connection = DriverManager.getConnection(sqliteFile);
            String query = "select version_order from lib_details where version = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, currentVersion);
            java.sql.ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                version_order = resultSet.getInt("version_order");
                break;
            }
            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return version_order;
    }


}
