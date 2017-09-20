package edu.rice.cs.caper.bayou.core.lexer.ccll._1_0;

import org.junit.Assert;
import org.junit.Test;

public class TokenTypeOtherTests
{
    @Test
    public void testMatch()
    {
        boolean correct =  new TokenTypeOther().match(new TokenTypeCases<Boolean,RuntimeException>()
        {
            @Override
            public Boolean forLineComment(TokenTypeLineComment lineComment)
            {
                return false;
            }

            @Override
            public Boolean forOther(TokenTypeOther outher)
            {
                return true;
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
