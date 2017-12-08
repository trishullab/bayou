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
package edu.rice.cs.caper.programming.numbers;

/**
 * An integer in the range [1...Integer.MAX_VALUE]
 */
public class NatNum32
{
    /**
     * The value of the NatNum32 in int form.
     */
    public final int AsInt;

    /**
     * Creates an integer with the same value as value.
     * @param value the integer value.
     * @throws IllegalArgumentException if value is <= 0.
     */
    public NatNum32(int value)
    {
        if(value <= 0)
            throw new IllegalArgumentException("A NatNum32 may not be zero or negative");

        AsInt = value;
    }

    /**
     * Creates an integer with the same value as value.
     * @param value the integer value i string form
     * @throws IllegalArgumentException if value is <= 0 or not a number.
     */
    public NatNum32(String value)
    {
        int valueInt;
        try
        {
            valueInt = Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException(e);
        }

        if(valueInt <= 0)
            throw new IllegalArgumentException("A NatNum32 may not be zero or negative");

        AsInt = valueInt;
    }

    /**
     * Compares this object to the specified object.  The result is
     * {@code true} if and only if the argument is not
     * {@code null} and is an {@code Integer} object that
     * contains the same {@code int} value as this object.
     *
     * @param   obj   the object to compare with.
     * @return  {@code true} if the objects are the same;
     *          {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj)
    {
        return new Integer(AsInt).equals(obj);
    }

    @Override
    public int hashCode()
    {
        return AsInt;
    }

    @Override
    public String toString()
    {
        return AsInt + "";
    }

    /**
     * @param value the string representation of an integer.
     * @return a NatNum32 representation of the given value.
     * @throws  NumberFormatException  if the string does not contain a parsable integer.
     * @throws IllegalArgumentException if value is <= 0.
     */
    public static NatNum32 parse(String value)
    {
        return new NatNum32(Integer.parseInt(value));
    }
}
