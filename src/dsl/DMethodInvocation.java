package dsl;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DMethodInvocation extends DExpression {

    final String node = "DMethodInvocation";
    final String methodName;

    private DMethodInvocation(String methodName) {
        this.methodName = methodName;
    }

    public static class Handle extends Handler {
        MethodInvocation invocation;

        public Handle(MethodInvocation invocation, Visitor visitor) {
            super(visitor);
            this.invocation = invocation;
        }

        @Override
        public DMethodInvocation handle() {
            String className = invocation.resolveMethodBinding().getDeclaringClass().getQualifiedName();
            if (className.contains("<")) /* be agnostic to generic versions */
                className = className.substring(0, className.indexOf("<"));
            if (visitor.options.API_CLASSES.contains(className))
                return new DMethodInvocation(className + "." + getSignature(invocation.resolveMethodBinding()));
            else
                return null;
        }

        private String getSignature(IMethodBinding method) {
            Stream<String> types = Arrays.stream(method.getParameterTypes()).map(t -> t.getQualifiedName());
            return method.getName() + "(" + String.join(",", types.collect(Collectors.toCollection(ArrayList::new))) + ")";
        }
    }
}
