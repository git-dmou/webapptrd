package fr.solunea.thaleia.webapp.api.transform;

import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.utils.DetailedException;

import java.io.File;
import java.util.Locale;

public interface ITransformTreatment<T> {

    T transform(Object input, User user, Locale locale) throws DetailedException;

}
