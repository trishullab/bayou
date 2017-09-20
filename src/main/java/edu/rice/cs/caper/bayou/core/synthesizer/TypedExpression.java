package edu.rice.cs.caper.bayou.core.synthesizer;

import org.eclipse.jdt.core.dom.Expression;

public class TypedExpression {

    private Expression expression;
    private Type type;

    public TypedExpression(Expression expression, Type type) {
        this.expression = expression;
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public Expression getExpression() {
        return expression;
    }

}
