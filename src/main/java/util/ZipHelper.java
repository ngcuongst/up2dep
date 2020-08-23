package util;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipHelper {    
    private static final int BUFFER_SIZE = 4096;

    public static void unZip(String zipFilePath, String destDirectory) {
        InputStream resourceAsStream = ZipHelper.class.getResourceAsStream("/" + zipFilePath + ".zip");
        File destDir = new File(destDirectory);
        if (destDir.exists()) {
            try {
                FileUtils.deleteDirectory(destDir);
            } catch (IOException e) {

            }
        }
        destDir.mkdirs();
        unZip(resourceAsStream, destDir);
    }

    public static boolean unZip(InputStream inputStream, File destDirectory) {
        boolean isSuccessful = false;
        try {

            if (destDirectory.exists()) {
                destDirectory.delete();
            }

            destDirectory.mkdirs();

            ZipInputStream zipIn = new ZipInputStream(inputStream);
            ZipEntry entry = zipIn.getNextEntry();
            // iterates over entries in the zip file
            while (entry != null) {
                String desFilePath = destDirectory + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    // if the entry is a file, extracts it
                    extractFile(zipIn, desFilePath);

                } else {
                    // if the entry is a directory, make the directory
                    File dir = new File(desFilePath);
                    dir.mkdir();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
            zipIn.close();
            isSuccessful = true;
        } catch (Exception ex) {
            
        }
        return isSuccessful;
    }

    private static boolean extractFile(ZipInputStream zipIn, String filePath) {
        boolean isSuccessful = false;
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
            byte[] bytesIn = new byte[BUFFER_SIZE];
            int read = 0;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
            bos.close();
            isSuccessful = true;
        } catch (Exception ex) {

        }
        return isSuccessful;
    }

}
