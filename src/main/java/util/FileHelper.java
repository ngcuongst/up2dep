package util;

import com.google.common.io.Files;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtFile;

import java.io.File;

public class FileHelper {
    private static final Logger LOG = Logger.getInstance(FileHelper.class);

    //TODO data folder may be removed?
    public static File createDataFolder() {
        try {
            File dataDir = Files.createTempDir();
            if (dataDir.exists()) {
                dataDir.delete();
            }
            dataDir.mkdirs();
            return dataDir;
        } catch (Exception ex) {
            LOG.warn(ex.getMessage());
        }
        return null;
    }

    public static boolean isKotlinOrJavaFile(PsiFile file) {
        boolean isKotlinOrJava = false;
        if (file instanceof PsiJavaFile || file instanceof KtFile)
            isKotlinOrJava = true;
        return isKotlinOrJava;
    }

    public static boolean isKotlinOrJavaFile(@NotNull VirtualFileEvent event){
        boolean isKotlinOrJava = false;
        if (event.getFile().getName().endsWith(".java") || event.getFile().getName().endsWith(".kt"))
            isKotlinOrJava = true;
        return isKotlinOrJava;
    }

}
