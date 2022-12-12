package fr.solunea.thaleia.webapp.utils;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import java.util.regex.Pattern;

@SuppressWarnings("serial")
public class PhoneNumberValidator implements IValidator<String> {

    @Override
    public void validate(IValidatable<String> validatable) {

        //        Description
        //        A regular expression to match phone numbers, allowing for an international dialing code at the
        // start and hyphenation and spaces that are sometimes entered.
        //                Matches
        //                        (+44)(0)20-12341234 | 02012341234 | +44 (0) 1234-1234
        //        Non-Matches
        //                (44+)020-12341234 | 12341234(+020)

        final Pattern pattern = Pattern.compile("\\+?[0-9 \\(\\)\\-]*");
//        final Pattern pattern = Pattern.compile("^(\\(?\\+?[0-9]*\\)?)?[0-9_\\- \\(\\)]*$");

        if (!pattern.matcher(validatable.toString()).matches()) {
            ValidationError error = new ValidationError();
            error.addKey("not.a.phone.number");
            validatable.error(error);
        }
    }

}
