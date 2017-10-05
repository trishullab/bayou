/*
Copyright 2017 Rice University

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package edu.rice.cs.caper.programming;

/**
 * A non-empty non-white-space-only string.
 */
public class ContentString
{
    /**
     * The String form of the object's value.
     */
    public final String AsString;

    /**
     * @param value the string value of the new object.  May not be null.
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is of length value or is composed only of whitespace characters.
     */
    public ContentString(String value)
    {
        if(value == null)
            throw new NullPointerException("value may not be null");

        if(value.length() == 0)
            throw new IllegalArgumentException("value must be non-empty");

        if(value.trim().length() == 0)
            throw new IllegalArgumentException("value may not be only whitespace characters");

        AsString = value;
    }

    /**
     * Compares this string to the specified object.  The result is {@code
     * true} if and only if the argument is not {@code null} and is a {@code
     * String} object or {@code ContentString} object that represents the same sequence of characters as this
     * object.
     *
     * @param  obj The object to compare this {@code String} against
     *
     * @return  {@code true} if the given object represents a {@code String}
     *          equivalent to this string, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj)
    {
        try
        {
            return equals((ContentString)obj);
        }
        catch (ClassCastException e)
        {
            return AsString.equals(obj);
        }
    }

    /**
     * @return  {@code true} if the given object represents a {@code String}
     *          equivalent to this string, {@code false} otherwise
     */
    public boolean equals(ContentString other)
    {
        if(other == null)
            return false;

        return other.AsString.equals(this.AsString);
    }

    @Override
    public int hashCode()
    {
        return AsString.hashCode();
    }

    @Override
    public String toString()
    {
        return AsString.toString();
    }
}
