package fr.solunea.thaleia.webapp.utils;

import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

/**
 * Vérifie que l'identifiant de cet utilisateur est unique.
 */
@SuppressWarnings("serial")
public class UniqueLoginValidator implements IValidator<String> {

    /**
     * Cet utilisateur n'est pas pris en compte dans la recherche des
     * utilisateurs existants.
     */
    private IModel<User> excludeUserModel;

    /**
     * @param userModel le modèle qui contient le compte utilisateur qui sera exclu
     *                  lors de la vérification de l'unicité.
     */
    public UniqueLoginValidator(IModel<User> userModel) {
        this.excludeUserModel = userModel;
    }

    public UniqueLoginValidator() {
        // On n'excluera aucun compte pour la vérification de l'unicité.
        this.excludeUserModel = Model.of(new User());
    }

    @Override
    public void validate(IValidatable<String> validatable) {

        // L'identifiant renseigné dans le formulaire
        final String name = validatable.getValue();

        // Si l'identifiant existe déjà en base, on déclenche l'erreur sur le
        // champ.
        if (ThaleiaSession.get().getUserService().isUserNameExists(name, excludeUserModel.getObject())) {
            error(validatable);
        }

    }

    private void error(IValidatable<String> validatable) {
        ValidationError error = new ValidationError();
        error.addKey("user.name.exists");
        validatable.error(error);
    }
}