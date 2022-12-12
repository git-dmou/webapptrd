package fr.solunea.thaleia.webapp.panels.confirmation;

import java.io.Serializable;

@SuppressWarnings("serial")
public class ConfirmationAnswer implements Serializable {

	private boolean answer;

	public ConfirmationAnswer(boolean answer) {
		this.answer = answer;
	}

	public boolean isAnswer() {
		return answer;
	}

	public void setAnswer(boolean answer) {
		this.answer = answer;
	}
}
