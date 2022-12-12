package fr.solunea.thaleia.webapp.utils;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.Model;

/* Supprime une classe CSS sur un composant */

@SuppressWarnings("serial")
public class CSSClassRemover extends AttributeModifier {

	public CSSClassRemover(String cssClass) {
		super("class", new Model<String>(cssClass));
	}

	@Override
	protected String newValue(String currentValue, String valueToRemove) {
		return currentValue.replaceAll(valueToRemove, "");
	}
}