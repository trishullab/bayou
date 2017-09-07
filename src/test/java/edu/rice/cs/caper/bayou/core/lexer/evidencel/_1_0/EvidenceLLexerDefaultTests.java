package edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0;

import edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0.EvidenceLLexer;
import edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0.EvidenceLLexerDefault;

public class EvidenceLLexerDefaultTests extends EvidenceLLexerTests
{
    @Override
    EvidenceLLexer makeLexer()
    {
        return new EvidenceLLexerDefault();
    }
}
