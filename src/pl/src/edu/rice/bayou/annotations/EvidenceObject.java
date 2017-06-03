package edu.rice.bayou.annotations;

import java.util.List;
import java.util.ArrayList;
import org.apache.commons.cli.*;

public class EvidenceObject {
    protected String type;

    protected List<String> elements;

    public EvidenceObject(String evidStr) {
	try {
            parseString(evidStr);
        } catch (ParseException e) {
            System.out.println("The bayou evidence should use such format: '" + "@key : value1, value2, ... '");
        }
    }

    protected void parseString(String input) throws ParseException {
	if (input.equals("")) {
	    throw new ParseException("Can not parse evidence");
	} 

	int start = input.indexOf("@");
	if (start > 0) {
	    checkBlank(input.substring(0, start));
	}

	// Get type name
	String content = parseType(input.substring(start + 1, input.length()));
	while (content != null) 
	    content = parseValue(content);
    }

    // Parse the type name
    protected String parseType(String input) throws ParseException {
	int stop = input.indexOf(":");
	int stop_ = input.indexOf(" ");
	if (stop == 0 || stop_ == 0)
	    throw new ParseException("Wrong type");

	if (stop > stop_) {
	    this.type = input.substring(0, stop_);
	} else 
	    this.type = input.substring(0, stop);

	return input.substring(stop + 1, input.length());
    }
    
    // Parse the values
    protected String parseValue(String input) throws ParseException {
	// Remove blank space
	if (input.length() == 0)
	    return null;

	while (input.indexOf(" ") == 0) {
	    if (input.length() == 1)
		return null;

	    input = input.substring(1, input.length());
	}

	int stop = input.indexOf(",");
	int stop_ = input.indexOf(" ");
	if (stop == 0)
	    throw new ParseException("Wrong value");

	if (stop > stop_) {
	    addValue(input.substring(0, stop_));
	    return input.substring(stop_, input.length());
	} else if (stop > 0) {
            addValue(this.type = input.substring(0, stop));
	    return input.substring(stop, input.length());
	}

	throw new ParseException("Wrong value");
    }

    protected void checkBlank(String blankString) throws ParseException {
	int idx = 0; 
	while (idx < blankString.length()) {
	    if (!blankString.substring(idx, idx + 1).equals(" "))
		throw new ParseException("Not blank space?");
	    idx += 1;
	}
    }

    protected void addValue(String value) {
	if (this.elements == null)
	    this.elements = new ArrayList<String>();

	this.elements.add(value);
    }

    public String getType() {
	return this.type;
    }

    public List<String> getElements() {
	return this.elements;
    }
}