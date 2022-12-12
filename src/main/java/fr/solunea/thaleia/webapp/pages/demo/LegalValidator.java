package fr.solunea.thaleia.webapp.pages.demo;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

@SuppressWarnings("serial")
public class LegalValidator implements IValidator<Boolean> {

	String errorMessageKey;

	public LegalValidator() {
		this.errorMessageKey = "legal.required";
	}

	public LegalValidator(String errorMessageKey) {
		this.errorMessageKey = errorMessageKey;
	}

	@Override
	public void validate(IValidatable<Boolean> validatable) {

		// La valeur booléenne sotckée dans le champ du formulaire.
		final Boolean value = validatable.getValue();

		// Si l'identifiant existe déjà en base, on déclenche l'erreur sur
		// le champ.
		if (!value) {
			ValidationError error = new ValidationError();
			error.addKey(errorMessageKey);
			validatable.error(error);
		}

	}
}
