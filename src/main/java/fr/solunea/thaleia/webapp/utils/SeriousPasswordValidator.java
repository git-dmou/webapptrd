package fr.solunea.thaleia.webapp.utils;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.EmailAddressValidator;

@SuppressWarnings("serial")
public class SeriousPasswordValidator implements IValidator<String> {

    @Override
    public void validate(IValidatable<String> validatable) {

        // Le mot de passe renseigné dans le formulaire
        final String password = validatable.getValue();

        // Si pas assez grand
        if (password == null || password.length() < 8) {
            ValidationError error = new ValidationError();
            error.addKey("toosmall");
            validatable.error(error);
        }

    }

}
