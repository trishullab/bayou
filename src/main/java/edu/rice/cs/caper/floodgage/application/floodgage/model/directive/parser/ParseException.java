package edu.rice.cs.caper.floodgage.application.floodgage.model.directive.parser;

public class ParseException extends Exception
{
    public ParseException(String msg)
    {
        super(msg);
    }

    public ParseException(Exception cause)
    {
        super(cause);
    }
}
