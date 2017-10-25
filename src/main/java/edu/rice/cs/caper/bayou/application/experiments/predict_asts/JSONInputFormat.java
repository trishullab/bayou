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
package edu.rice.cs.caper.bayou.application.experiments.predict_asts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import edu.rice.cs.caper.bayou.application.dom_driver.*;
import edu.rice.cs.caper.bayou.core.synthesizer.RuntimeTypeAdapterFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JSONInputFormat {

    public static List<DataPoint> readData(String file) throws IOException {
        RuntimeTypeAdapterFactory<DOMExpression> exprAdapter =
                RuntimeTypeAdapterFactory.of(DOMExpression.class, "node")
                    .registerSubtype(DOMMethodInvocation.class)
                    .registerSubtype(DOMClassInstanceCreation.class)
                    .registerSubtype(DOMInfixExpression.class)
                    .registerSubtype(DOMPrefixExpression.class)
                    .registerSubtype(DOMConditionalExpression.class)
                    .registerSubtype(DOMVariableDeclarationExpression.class)
                    .registerSubtype(DOMAssignment.class)
                    .registerSubtype(DOMParenthesizedExpression.class)
                    .registerSubtype(DOMNullLiteral.class)
                    .registerSubtype(DOMName.class)
                    .registerSubtype(DOMNumberLiteral.class);
        RuntimeTypeAdapterFactory<DOMStatement> stmtAdapter =
                RuntimeTypeAdapterFactory.of(DOMStatement.class, "node")
                    .registerSubtype(DOMBlock.class)
                    .registerSubtype(DOMExpressionStatement.class)
                    .registerSubtype(DOMIfStatement.class)
                    .registerSubtype(DOMSwitchStatement.class)
                    .registerSubtype(DOMSwitchCase.class)
                    .registerSubtype(DOMDoStatement.class)
                    .registerSubtype(DOMForStatement.class)
                    .registerSubtype(DOMEnhancedForStatement.class)
                    .registerSubtype(DOMWhileStatement.class)
                    .registerSubtype(DOMTryStatement.class)
                    .registerSubtype(DOMVariableDeclarationStatement.class)
                    .registerSubtype(DOMSynchronizedStatement.class)
                    .registerSubtype(DOMReturnStatement.class)
                    .registerSubtype(DOMLabeledStatement.class);
        Gson gson = new GsonBuilder().serializeNulls()
                .registerTypeAdapterFactory(exprAdapter)
                .registerTypeAdapterFactory(stmtAdapter)
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        String s = new String(Files.readAllBytes(Paths.get(file)));
        Data js = gson.fromJson(s, Data.class);

        return js.programs;
    }

    static class Data {
        @Expose
        List<DataPoint> programs;
        Data() {
            programs = new ArrayList<>();
        }
    }

    static class DataPoint {
        @Expose
        DOMMethodDeclaration aml_ast;
        @Expose
        List<DOMMethodDeclaration> out_aml_asts;
        @Expose
        String file;
        @Expose
        Set<String> apicalls;
        @Expose
        Set<String> types;
        @Expose
        Set<String> context;
        @Expose
        Set<String> keywords;
        @Expose
        boolean in_corpus;

        DataPoint() {
            out_aml_asts = new ArrayList<>();
            apicalls = new HashSet<>();
            types = new HashSet<>();
            context = new HashSet<>();
            keywords = new HashSet<>();
        }
    }
}
