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
package edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0;

public interface Token
{
    /**
     * @return the contents of the token
     */
    String getLexeme();

    /**
     * @return the type of the token
     */
    TokenType getType();

    /**
     * Creates a Token.
     *
     * @param lexeme the contents of the token
     * @param type the type of the token
     * @return the token
     */
    static Token make(String lexeme, TokenType type)
    {
        return new Token()
        {
            @Override
            public String getLexeme()
            {
                return lexeme;
            }

            @Override
            public TokenType getType()
            {
                return type;
            }

        };
    }
}
