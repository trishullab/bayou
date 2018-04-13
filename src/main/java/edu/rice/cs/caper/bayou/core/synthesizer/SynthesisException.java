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
package edu.rice.cs.caper.bayou.core.synthesizer;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SynthesisException extends RuntimeException {

    public static final int CouldNotResolveBinding = 1000;
    public static final int EvidenceNotInBlock = 1001;
    public static final int EvidenceMixedWithCode = 1002;
    public static final int MoreThanOneHole = 1003;
    public static final int InvalidEvidenceType = 1004;
    public static final int CouldNotEditDocument = 1005;
    public static final int ClassNotFoundInLoader = 1006;
    public static final int TypeNotFoundDuringSearch = 1007;
    public static final int MethodOrConstructorNotFound = 1008;
    public static final int GenericTypeVariableMismatch = 1009;
    public static final int InvalidKindOfType = 1010;
    public static final int MalformedASTFromNN = 1011;
    public static final int TypeParseException = 1012;
    public static final int IrrelevantCodeInBody = 1013;

    private static final Map<Integer,String> toMessage;
    static {
        Map<Integer,String> _toMessage = new HashMap<>();
        _toMessage.put(CouldNotResolveBinding,
                "Bayou could not resolve the reference of the method %s");
        _toMessage.put(EvidenceNotInBlock,
                "Please provide evidence in a block.");
        _toMessage.put(EvidenceMixedWithCode,
                "Please provide evidence in a separate empty block.");
        _toMessage.put(MoreThanOneHole,
                "More than one hole for synthesis not currently supported.");
        _toMessage.put(InvalidEvidenceType,
                "%s is an invalid kind of evidence. Only API calls, types and keywords are supported.");
        _toMessage.put(CouldNotEditDocument,
                "Bayou internal error: could not edit document for some reason.");
        _toMessage.put(ClassNotFoundInLoader,
                "Some programs required the use of the class %s which could not be found in the class loader.");
        _toMessage.put(TypeNotFoundDuringSearch,
                "Some programs required the use of the type %s which could not be found during search. " +
                "Please ensure that you provide at least one variable in your program.");
        _toMessage.put(MethodOrConstructorNotFound,
                "Some programs required the use of the method or constructor %s which could not be found.");
        _toMessage.put(GenericTypeVariableMismatch,
                "Some programs required the use of the type %s, which involves advanced generics such as wildcards.");
        _toMessage.put(InvalidKindOfType,
                "Some programs required the use of the type %s, which involves advanced generics such as wildcards.");
        _toMessage.put(MalformedASTFromNN,
                "Bayou internal error: Malformed AST predicted by neural network.");
        _toMessage.put(TypeParseException,
                "Bayou internal error: Malformed type from AST.");
        _toMessage.put(IrrelevantCodeInBody,
                "Please ensure that your method body only contains variable declarations and evidences.");
        toMessage = Collections.unmodifiableMap(_toMessage);
    }

    private final int id;

    public SynthesisException(int id, String arg) {
        super(String.format(toMessage.get(id), arg));
        this.id = id;
    }

    public SynthesisException(int id) {
        super(toMessage.get(id));
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
