package dsl;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DClassInstanceCreation extends DExpression {

    final String node = "DClassInstanceCreation";
    String constructor;
    final List<List<Refinement>> argRefinements;

    private DClassInstanceCreation(String constructor, List<List<Refinement>> argRefinements) {
        this.constructor = constructor;
        this.argRefinements = argRefinements;
    }

    @Override
    public String sketch() {
        return constructor;
    }

    @Override
    public void updateSequences(List<Sequence> soFar) {
        for (Sequence seq : soFar)
            seq.addCall(constructor);
    }

    public static class Handle extends Handler {
        ClassInstanceCreation creation;

        public Handle(ClassInstanceCreation creation, Visitor visitor) {
            super(visitor);
            this.creation = creation;
        }

        @Override
        public DClassInstanceCreation handle() {
            String className = checkAndGetClassName();
            if (className != null) {
                List<List<Refinement>> argRefinements = new ArrayList<>();
                for (Object arg : creation.arguments())
                    argRefinements.add(Refinement.getRefinements((Expression) arg, visitor));
                visitor.API = className;
                return new DClassInstanceCreation(className + "." + getSignature(creation.resolveConstructorBinding()), argRefinements);
            }
            return null;
        }

        private String getSignature(IMethodBinding constructor) {
            Stream<String> types = Arrays.stream(constructor.getParameterTypes()).map(t -> t.getQualifiedName());
            return constructor.getName() + "(" + String.join(",", types.collect(Collectors.toCollection(ArrayList::new))) + ")";
        }

        /* check if the class corresponding to this instance creation is in API_CLASSES, and return
         * the class name if so (return null if not).
         */
        private String checkAndGetClassName() {
            IMethodBinding binding = creation.resolveConstructorBinding();
            if (binding != null && binding.getDeclaringClass() != null) {
                String className = binding.getDeclaringClass().getQualifiedName();
                if (className.contains("<")) /* be agnostic to generic versions */
                    className = className.substring(0, className.indexOf("<"));
                if (visitor.options.API_CLASSES.contains(className))
                    return className;
            }
            return null;
        }
    }
}
