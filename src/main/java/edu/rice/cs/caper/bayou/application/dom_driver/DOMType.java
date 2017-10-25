/*
Copyright 2017 Rice University

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package edu.rice.cs.caper.bayou.application.dom_driver;

import com.google.gson.annotations.Expose;
import edu.rice.cs.caper.bayou.core.dsl.Sequence;
import org.eclipse.jdt.core.dom.*;

import java.util.*;
import java.util.stream.Collectors;

public class DOMType extends DOMNode {

    @Expose
    String node = "DOMType";

    @Expose
    String type;

    @Expose
    List<DOMType> parameters; // will be of size > 0 if type is generic

    public DOMType() {
        this.node = "DOMType";
    }

    static Map<PrimitiveType.Code,String> primitiveMap;
    static {
        Map<PrimitiveType.Code,String> map = new HashMap<>();
        map.put(PrimitiveType.INT, "int");
        map.put(PrimitiveType.LONG, "long");
        map.put(PrimitiveType.DOUBLE, "double");
        map.put(PrimitiveType.FLOAT, "float");
        map.put(PrimitiveType.BOOLEAN, "boolean");
        map.put(PrimitiveType.CHAR, "char");
        map.put(PrimitiveType.BYTE, "byte");
        map.put(PrimitiveType.VOID, "void");
        map.put(PrimitiveType.SHORT, "short");
        primitiveMap = Collections.unmodifiableMap(map);
    }

    public DOMType(Type type) {
        this.type = resolveType(type);
        this.parameters = new ArrayList<>();
        if (type.isParameterizedType()) {
            for (Object o : ((ParameterizedType) type).typeArguments())
                this.parameters.add(new DOMType((Type) o).handleAML());
        }
    }

    private String resolveType(Type t) {
        if (t.isPrimitiveType()) {
            return primitiveMap.get(((PrimitiveType) t).getPrimitiveTypeCode());
        }
        else if (t.isSimpleType()) {
            return ((SimpleType) t).getName().getFullyQualifiedName();
        }
        else if (t.isQualifiedType()) {
            QualifiedType q = (QualifiedType) t;
            String s = resolveType(q.getQualifier());
            return s + "." + q.getName().getIdentifier();
        }
        else if (t.isParameterizedType()) {
            return resolveType(((ParameterizedType) t).getType());
        }
        else throw new IllegalArgumentException("Invalid kind of type: " + t);
    }

    @Override
    public DOMType handleAML() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof DOMType))
            return false;
        DOMType d = (DOMType) o;
        if (! type.equals(d.type) || parameters.size() != d.parameters.size())
            return false;
        for (int i = 0; i < parameters.size(); i++)
            if (! parameters.get(i).equals(d.parameters.get(i)))
                return false;
        return true;
    }

    @Override
    public int hashCode() {
        return 7* type.hashCode() + 17* parameters.hashCode();
    }

    @Override
    public Set<String> bagOfAPICalls() {
        Set<String> calls = new HashSet<>();
        return calls;
    }

    @Override
    public void updateSequences(List<Sequence> soFar, int max, int max_length)
            throws TooManySequencesException, TooLongSequenceException {
    }

    @Override
    public int numStatements() {
        return 0;
    }

    @Override
    public int numLoops() {
        return 0;
    }

    @Override
    public int numBranches() {
        return 0;
    }

    @Override
    public int numExcepts() {
        return 0;
    }

    @Override
    public String toAML() {
        List<String> params = parameters.stream().map(p -> p.toAML()).collect(Collectors.toList());
        return params.isEmpty()? type : String.format("%s<%s>", type, String.join(",", params));
    }
}
