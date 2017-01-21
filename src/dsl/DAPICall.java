package dsl;

import org.eclipse.jdt.core.dom.IMethodBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DAPICall extends DASTNode {

    final String node = "DAPICall";
    final String call;
    final transient IMethodBinding method;

    /* TODO: Add refinement types (predicates) here */

    public DAPICall(IMethodBinding method) {
        this.method = method;
        this.call = getClassName() + "." + getSignature();
    }

    @Override
    public void updateSequences(List<Sequence> soFar) {
        for (Sequence sequence : soFar)
            sequence.addCall(call);
    }

    private String getClassName() {
        String className = method.getDeclaringClass().getQualifiedName();
        if (className.contains("<")) /* be agnostic to generic versions */
            className = className.substring(0, className.indexOf("<"));
        return className;
    }

    private String getSignature() {
        Stream<String> types = Arrays.stream(method.getParameterTypes()).map(t -> t.getQualifiedName());
        return method.getName() + "(" + String.join(",", types.collect(Collectors.toCollection(ArrayList::new))) + ")";
    }

    public String getCall() {
        return call;
    }

    public IMethodBinding getMethod() {
        return method;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || ! (o instanceof DAPICall))
            return false;
        DAPICall apiCall = (DAPICall) o;
        return call.equals(apiCall.call);
    }

    @Override
    public int hashCode() {
        return call.hashCode();
    }
}
