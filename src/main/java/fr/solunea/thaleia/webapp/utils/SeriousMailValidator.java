package fr.solunea.thaleia.webapp.utils;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.EmailAddressValidator;

@SuppressWarnings("serial")
public class SeriousMailValidator extends EmailAddressValidator {

	@Override
	public void validate(IValidatable<String> validatable) {
		// On vérifie que le format correspond à une adresse mail.
		super.validate(validatable);

		// On refuse certaines adresses
		if (validatable.getValue().toLowerCase().endsWith("yopmail.com")) {
			error(validatable);
		}
	}

	private void error(IValidatable<String> validatable) {
		ValidationError error = new ValidationError(this);
		validatable.error(decorate(error, validatable));
	}

}
