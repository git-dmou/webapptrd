package fr.solunea.thaleia.webapp.utils;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.EmailAddressValidator;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public abstract class DomainConstraintMailValidator extends EmailAddressValidator {

    List<String> domains = new ArrayList<>();

    public DomainConstraintMailValidator(List<String> domains) {
        super();
        if (domains != null) {
            this.domains = domains;
        }
    }

    @Override
    public void validate(IValidatable<String> validatable) {
        // On vérifie que le format correspond à une adresse mail.
        super.validate(validatable);

        // On n'accepte que les adresses des domaines en liste blanche
        boolean whitelisted = false;
        for (String domain : domains) {
            if (validatable.getValue().toLowerCase().endsWith("@" + domain)) {
                whitelisted = true;
            }
        }
        if (!whitelisted) {
            error(validatable);
        }
    }

    private void error(IValidatable<String> validatable) {
        ValidationError error = new ValidationError(this);
        error.setKeys(new ArrayList<>());
        error.setMessage(getErrorMessage());
        validatable.error(decorate(error, validatable));
    }

    public abstract String getErrorMessage();

}
