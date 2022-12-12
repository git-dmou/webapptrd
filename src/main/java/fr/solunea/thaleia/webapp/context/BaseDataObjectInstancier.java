package fr.solunea.thaleia.webapp.context;

import fr.solunea.thaleia.model.dao.CayenneDao;
import org.apache.cayenne.BaseDataObject;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.lang.reflect.Constructor;

public class BaseDataObjectInstancier<T extends BaseDataObject> implements Serializable {

    private static final Logger logger = Logger.getLogger(BaseDataObjectInstancier.class);

    /**
     * @return une instance de cet objet dans un nouveau contexte, spécifique à cet objet, pour pouvoir l'éditer et le commiter en base indépendamment des autres objets en cours d'édition dans le contexte principal.
     */
    public T getInNewContext(BaseDataObject object, ObjectContext context) {
        String daoClassName = getDaoClassName(object.getClass());

        try {
            CayenneDao<T> dao = getCayenneDao(daoClassName, context);

            if (object.getObjectId() != null && !object.getObjectId().isTemporary()) {
                // Si l'objet existe en base, on obtient une nouvelle instance, dans ce contexte, pour cet id
                return dao.get(object.getObjectId());
            } else {
                throw new Exception("Impossible de créer un objet dans un nouveau contexte !");
            }

        } catch (Exception e) {
            logger.warn(e);
            return null;
        }
    }

    protected String getDaoClassName(Class<? extends BaseDataObject> aClass) {
        // On en extrait le nom de la classe de l'objet, par exemple ApplicationParameter
        String objectClassName = aClass.getSimpleName();
        // Par convention dans le code Thaleia :
        return "fr.solunea.thaleia.model.dao."+objectClassName+"Dao";
    }

    private CayenneDao<T> getCayenneDao(String daoClassName, ObjectContext context) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException {
        Class<? extends CayenneDao<T>> clazz = (Class<? extends CayenneDao<T>>) Class.forName(daoClassName, false, Thread.currentThread().getContextClassLoader());
        // Recherche du constructeur
        Constructor<? extends CayenneDao<T>> constructor = clazz.getDeclaredConstructor(ObjectContext.class);
        return constructor.newInstance(context);
    }

    /**
     * @param classToInstanciate la classe de l'objet à instancier.
     * @return un nouvel objet de cette classe, attaché dans un nouveau contexte, pour pouvoir l'éditer et le commiter en base indépendamment des autres objets en cours d'édition dans le contexte principal.
     */
    public T getInNewContext(Class<? extends BaseDataObject> classToInstanciate, ObjectContext context) {
        // Récupération du DAO correspondant à cet objet
        String daoClassName = getDaoClassName(classToInstanciate);
        try {
            CayenneDao<T> dao = getCayenneDao(daoClassName, context);
            return dao.get(true);
        } catch (Exception e) {
            logger.warn(e);
            return null;
        }
    }
}
