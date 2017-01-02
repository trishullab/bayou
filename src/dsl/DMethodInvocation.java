package dsl;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DMethodInvocation extends DExpression {

    final String node = "DMethodInvocation";
    final String methodName;
    final List<List<Refinement>> argRefinements;

    private DMethodInvocation(String methodName, List<List<Refinement>> argRefinements) {
        this.methodName = methodName;
        this.argRefinements = argRefinements;
    }

    public static class Handle extends Handler {
        MethodInvocation invocation;

        public Handle(MethodInvocation invocation, Visitor visitor) {
            super(visitor);
            this.invocation = invocation;
        }

        @Override
        public DMethodInvocation handle() {
            String className = checkAndGetClassName();
            if (className != null) {
                List<List<Refinement>> argRefinements = new ArrayList<>();
                for (Object arg : invocation.arguments())
                    argRefinements.add(Refinement.getRefinements((Expression) arg));
                return new DMethodInvocation(className + "." + getSignature(invocation.resolveMethodBinding()), argRefinements);
            }
            return null;
        }

        @Override
        public void updateSequences(List<Sequence> soFar) {
            String className = checkAndGetClassName();
            if (className != null)
                for (Sequence seq : soFar)
                    seq.addCall(className + "." + getSignature(invocation.resolveMethodBinding()));
        }

        /* check if the class corresponding to this method invocation is in API_CLASSES, and return
         * the class name if so (return null if not).
         */
        private String checkAndGetClassName() {
            IMethodBinding binding = invocation.resolveMethodBinding();
            if (binding != null && binding.getDeclaringClass() != null) {
                String className = binding.getDeclaringClass().getQualifiedName();
                if (className.contains("<")) /* be agnostic to generic versions */
                    className = className.substring(0, className.indexOf("<"));
                if (visitor.options.API_CLASSES.contains(className))
                    return className;
            }
            return null;
        }

        private String getSignature(IMethodBinding method) {
            Stream<String> types = Arrays.stream(method.getParameterTypes()).map(t -> t.getQualifiedName());
            return method.getName() + "(" + String.join(",", types.collect(Collectors.toCollection(ArrayList::new))) + ")";
        }
    }
}
