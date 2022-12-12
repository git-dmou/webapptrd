package fr.solunea.thaleia.webapp.utils;

import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.apache.wicket.resource.loader.ClassStringResourceLoader;

import java.text.MessageFormat;
import java.util.Locale;

public class MessagesUtils {

    /**
     * Recherche la valeur de la chaîne localisée dans le fichier
     * Class.properties (où XX est la locale de la session, et Class la classe
     * localisée).
     * <p>
     * Cette méthode peut être utilisée pour utiliser des fichiers de
     * localisation par des des classes non Wicket.
     *
     * @param resourceKey le code du message à rechercher
     * @param parameters  si besoin, des paramètres pour appliquer des interprétations de valeurs dans la chaîne de
     *                    caractères ${0}, ${1}...
     * @return le message d'erreur localisé
     */
    public static String getLocalizedMessage(String resourceKey, Class<?> clazz, Object... parameters) {

        // On recherche la locale en session
        Locale locale = Session.exists() ? ThaleiaSession.get().getLocale() : Locale.getDefault();

        return getLocalizedMessage(resourceKey, clazz, locale, parameters);
    }

    /**
     * Recherche la valeur de la chaîne localisée dans le fichier
     * Class.properties (où XX est la locale de la session, et Class la classe
     * localisée).
     * <p>
     * Cette méthode peut être utilisée pour utiliser des fichiers de
     * localisation par des des classes non Wicket.
     *
     * @param resourceKey le code du message à rechercher
     * @param locale      La locale dans laquelle rechercher la valeur
     * @param parameters  si besoin, des paramètres pour appliquer des interprétations de valeurs dans la chaîne de
     *                    caractères ${0}, ${1}...
     * @return le message d'erreur localisé
     */
    public static String getLocalizedMessage(String resourceKey, Class<?> clazz, Locale locale, Object... parameters) {

        // L'objet de recherche des fichiers localisés dans le classloader
        ClassStringResourceLoader loader = new ClassStringResourceLoader(clazz);

        String value;
        String defaultValue = "";

        // On récupère la valeur dans le fichier de properties qui correspond à resourceKey
        value = loader.loadStringResource(clazz, resourceKey, locale, ThaleiaSession.get().getStyle(), "");
        if (value == null) {
            value = defaultValue;
        }

        // On effectue les remplacements de {0}, {1}... par les paramètres
        // transmis. Si ces paramètres sont des modèles Wicket, alors on
        // récupère leur valeur.
        value = MessagesUtils.formatMessage(value, locale, parameters);

        return value;

    }

    /**
     * Formatte un message en remplaçant les éventuels ${0}, ${1} par les
     * valeurs des paramètres transmis. Si ces paramètres sont des Modèles
     * Wicket, alors on recherche les objets portés par ces modèles.
     */
    public static String formatMessage(String message, Locale locale, Object... parameters) {

        String result = message;

        if (parameters != null && parameters.length != 0) {
            if (result != null) {
                Object[] realParams = new Object[parameters.length];
                for (int i = 0; i < parameters.length; i++) {
                    if (parameters[i] instanceof IModel<?>) {
                        realParams[i] = ((IModel<?>) parameters[i]).getObject();
                    } else {
                        realParams[i] = parameters[i];
                    }
                }

                if (result.indexOf('\'') != -1) {
                    result = escapeQuotes(result);
                }

                final MessageFormat format = new MessageFormat(result, locale);
                result = format.format(realParams);
            }
        }

        return result;
    }

    /**
     * Remplace les "'" par des "''" hors accolades : "{..}"
     *
     * @param string la chaîne
     * @return la chaîne echappée.
     */
    private static String escapeQuotes(final String string) {
        StringBuilder newValue = new StringBuilder(string.length() + 10);
        int count = 0;
        for (int i = 0; i < string.length(); i++) {
            char ch = string.charAt(i);
            if (ch == '{') {
                count += 1;
            } else if (ch == '}') {
                count -= 1;
            }

            newValue.append(ch);
            if ((ch == '\'') && (count == 0)) {
                // On échappe "'"
                newValue.append(ch);
            }
        }

        return newValue.toString();
    }

}
