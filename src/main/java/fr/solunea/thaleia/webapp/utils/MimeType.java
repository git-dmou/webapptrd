package fr.solunea.thaleia.webapp.utils;

import java.net.URLConnection;

public class MimeType {

    public static String parseMimeType(String filename) {
        String result;

        result = URLConnection.guessContentTypeFromName(filename);

        // Si pas trouvé
        if (result == null) {
            if (filename.toLowerCase().endsWith(".swf")) {
                result = "application/x-shockwave-flash";
            }
        }

        // On corrige le type MIME pour Chrome
        if (filename.toLowerCase().endsWith(".svg")) {
            result = "image/svg+xml";
        }
        if (filename.toLowerCase().endsWith(".js")) {
            result = "application/javascript";
        }
        if (filename.toLowerCase().endsWith(".css")) {
            result = "text/css";
        }
        if (filename.toLowerCase().endsWith(".mp4")) {
            result = "video/mp4";
        }

        return result;
    }
}
