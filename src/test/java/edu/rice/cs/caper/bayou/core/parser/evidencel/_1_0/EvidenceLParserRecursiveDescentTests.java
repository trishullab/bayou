package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

public class EvidenceLParserRecursiveDescentTests extends EvidenceLParserTests
{
    @Override
    protected EvidenceLParser makeParser()
    {
        return new EvidenceLParserRecursiveDescent();
    }
}
