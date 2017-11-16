package edu.rice.cs.caper.floodgage.application.floodgage.model.directive;

public interface Evidence
{
    String getType();

    String getContent();

    static Evidence make(String type, String content)
    {
        return new Evidence()
        {
            @Override
            public String getType()
            {
                return type;
            }

            @Override
            public String getContent()
            {
                return content;
            }
        };
    }
}
