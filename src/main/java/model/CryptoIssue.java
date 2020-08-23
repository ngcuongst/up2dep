package model;

public class CryptoIssue {
    private final String version;
    private final String clazz;
    private final String method;
    private final String ruleName;
    private final String statement;
    private final String details;

    public CryptoIssue(String version, String clazz, String method, String ruleName, String statement, String details){
        this.version = version;
        this.clazz = clazz;
        this.method = method;
        this.ruleName = ruleName;
        this.statement = statement;
        this.details = details;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getStatement() {
        return statement;
    }

    public String getMethod() {
        return method;
    }

    public String getDetails() {
        return details;
    }
}
