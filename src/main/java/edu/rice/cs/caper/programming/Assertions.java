package edu.rice.cs.caper.programming;

public class Assertions
{
    /**
     * If argumentValue is non-null, returns argumentValue. Otherwise throws a NullPointerException with paramName
     * as the message argument.
     *
     * @param paramName the parameterName that corresponds to argumentValue
     * @param argumentValue the value of the argument
     * @param <T> the type of argumentValue
     * @return argumentValue if argumentValue is non-null
     */
    public static <T> T assertArgumentNonNull(String paramName, T argumentValue)
    {
        if (argumentValue == null)
            throw new NullPointerException(paramName);

        return argumentValue;
    }
}
