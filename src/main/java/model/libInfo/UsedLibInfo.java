package model.libInfo;

import com.intellij.psi.PsiFile;
import model.CryptoIssue;
import model.gradle.GradleCoordinate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class UsedLibInfo {
    private GradleCoordinate libDetail;
    private HashMap<PsiFile, HashSet<String>> fileAppUsedAPI;
    private Set<PsiFile> usedLibFiles;
    private HashMap<String, CryptoIssue> usedAPIs;

    public UsedLibInfo(GradleCoordinate libDetail, HashMap<PsiFile, HashSet<String>> fileAppUsedAPI, Set<PsiFile> usedLibFiles, HashSet<String> usedAPIs, HashMap<String, CryptoIssue> cryptoIssues) {
        this.libDetail = libDetail;
        this.fileAppUsedAPI = fileAppUsedAPI;
        this.usedLibFiles = usedLibFiles;
        this.usedAPIs = setupUsedAPIs(usedAPIs, cryptoIssues);
    }

    private HashMap<String, CryptoIssue> setupUsedAPIs(HashSet<String> usedAPIs, HashMap<String, CryptoIssue> cryptoIssues){
        HashMap<String, CryptoIssue> results = new HashMap<>();
        for(String usedAPI: usedAPIs){
            if(cryptoIssues.containsKey(usedAPI)){
                results.put(usedAPI, cryptoIssues.get(usedAPI));
            }else{
                results.put(usedAPI, null);
            }
        }
        return results;
    }
    public GradleCoordinate getLibDetail(){
        return this.libDetail;
    }
    public HashMap<PsiFile, HashSet<String>> getFileAppUsedAPI() {
        return fileAppUsedAPI;
    }

    public Set<PsiFile> getUsedLibFiles() {
        return usedLibFiles;
    }

    public HashMap<String, CryptoIssue> getUsedAPIs() {
        return usedAPIs;
    }
}
