package fr.solunea.thaleia.webapp.utils;

import fr.solunea.thaleia.utils.DetailedException;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TempFileMagicBasket {

    private List<File> tempFileAndDirBasket = new ArrayList<>();

    public TempFileMagicBasket() {
        super();
    }


    public File addFileFromPath(String path) {
        File tempFile = new File(path);
        tempFileAndDirBasket.add(tempFile);
        return tempFile;
    }

    public File addFile(File tempFile) {
        tempFileAndDirBasket.add(tempFile);
        return tempFile;

    }

    public File makeTempFileFromFile(File file) throws DetailedException {
        File tempFile;
        try {
            InputStream is = new FileInputStream(file);

            tempFile = File.createTempFile("/customTempFile", "");
            FileUtils.copyInputStreamToFile(is, tempFile);
            tempFileAndDirBasket.add(tempFile);
            return tempFile;
        } catch (IOException e) {
            throw new DetailedException(e).addMessage("problème création fichier temporaire");
        }

    }


    public void clean() throws DetailedException {

        Iterator<File> tempFilesIterator = tempFileAndDirBasket.iterator();
        while (tempFilesIterator.hasNext()) {
            File fileToDelete = tempFilesIterator.next();
            try {
                FileUtils.forceDelete(fileToDelete);
            } catch (FileNotFoundException e) {
                // parfait : il n'existe pas, pas besoin de le supprimer !
            }catch (IOException npe) {
                String msg = "Impossible de supprimer " + fileToDelete + " : " + npe.toString();
                throw new DetailedException(msg);
            }
        }
    }


}
