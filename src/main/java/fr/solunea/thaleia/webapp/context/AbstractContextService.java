package fr.solunea.thaleia.webapp.context;

import fr.solunea.thaleia.model.dao.ICayenneContextService;
import org.apache.cayenne.BaseDataObject;

public abstract class AbstractContextService implements ICayenneContextService {

    public <T extends BaseDataObject> T getNewInNewContext(Class<T> clazz) {
        return new BaseDataObjectInstancier<T>().getInNewContext(clazz, getNewContext());
    }

    public <T extends BaseDataObject> T getObjectInNewContext(T source) {
        return new BaseDataObjectInstancier<T>().getInNewContext(source, getNewContext());
    }

    public <T extends BaseDataObject> void safeDelete(T object) {
        T objectInNewContext = getObjectInNewContext(object);
        objectInNewContext.getObjectContext().deleteObject(objectInNewContext);
        objectInNewContext.getObjectContext().commitChanges();
    }
}
