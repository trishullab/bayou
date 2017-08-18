package edu.rice.cs.caper.bayou.core.lexer.ccll._1_0;

import org.junit.Assert;
import org.junit.Test;

public class TokenTypeLineCommentTests
{
    @Test
    public void testMatch()
    {
        boolean correct =  new TokenTypeLineComment().match(new TokenTypeCases<Boolean>()
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
        });

        Assert.assertTrue(correct);
    }
}
