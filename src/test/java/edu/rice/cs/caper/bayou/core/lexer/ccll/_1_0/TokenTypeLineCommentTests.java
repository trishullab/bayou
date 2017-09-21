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
package edu.rice.cs.caper.bayou.core.lexer.ccll._1_0;

import org.junit.Assert;
import org.junit.Test;

public class TokenTypeLineCommentTests
{
    @Test
    public void testMatch()
    {
        boolean correct =  new TokenTypeLineComment().match(new TokenTypeCases<Boolean,RuntimeException>()
        {
            @Override
            public Boolean forLineComment(TokenTypeLineComment lineComment)
            {
                return true;
            }

            @Override
            public Boolean forOther(TokenTypeOther outher)
            {
                return false;
            }

            @Override
            public Boolean forString(TokenTypeString string)
            {
                return false;
            }

            @Override
            public Boolean forBlockComment(TokenTypeBlockComment blockComment)
            {
                return false;
            }
        });

        Assert.assertTrue(correct);
    }
}
