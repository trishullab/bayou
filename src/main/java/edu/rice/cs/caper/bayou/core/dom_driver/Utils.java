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
package edu.rice.cs.caper.bayou.core.dom_driver;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Utils {
    private Utils() {
        throw new AssertionError("Do not instantiate this class!");
    }

    public static boolean isRelevantCall(IMethodBinding binding) {
        ITypeBinding cls;
        if (binding == null || (cls = binding.getDeclaringClass()) == null)
            return false;
        IPackageBinding pack = cls.getPackage();
        String[] packs = pack.getNameComponents();
        if (Visitor.V().options.API_MODULES.contains(packs[0]))
            return true;
        if (Visitor.V().options.API_PACKAGES.contains(pack.getName()))
            return true;
        String className = cls.getQualifiedName();
        if (className.contains("<")) /* be agnostic to generic versions */
            className = className.substring(0, className.indexOf("<"));
        if (Visitor.V().options.API_CLASSES.contains(className))
            return true;

        return false;
    }

    public static MethodDeclaration checkAndGetLocalMethod(IMethodBinding binding) {
        if (binding != null)
            for (MethodDeclaration method : Visitor.V().allMethods)
                if (binding.isEqualTo(method.resolveBinding()))
                    return method;
        return null;
    }

    public static String getJavadoc(MethodDeclaration method, String javadocType) {
        try {
            Javadoc doc = method.getJavadoc();
            List<IDocElement> fragments = ((TagElement) doc.tags().get(0)).fragments();
            String str = String.join(" ", fragments.stream().map(f -> getJavadocText(f)).collect(Collectors.toList()));
            if (javadocType.equals("summary")) {
                Pattern p = Pattern.compile("(.*?)\\.\\W.*");
                Matcher m = p.matcher(str);
                return sanitizeJavadoc(m.matches() ? m.group(1) : str);
            } else if (javadocType.equals("full")) {
                return str;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String getJavadocText(IDocElement fragment) {
        if (fragment instanceof TextElement)
            return ((TextElement) fragment).getText();
        if (fragment instanceof Name)
            return ((Name) fragment).getFullyQualifiedName();
        if (fragment instanceof MemberRef)
            return ((MemberRef) fragment).getName().getIdentifier();
        if (fragment instanceof MethodRef)
            return ((MethodRef) fragment).getName().getIdentifier();
        if (fragment instanceof TagElement) {
            List<IDocElement> fragments = ((TagElement) fragment).fragments();
            return String.join(" ", fragments.stream().map(f -> getJavadocText(f)).collect(Collectors.toList()));
        }
        throw new RuntimeException();
    }

    private static String sanitizeJavadoc(String str) {
        String[] stop_words = { "TODO", "FIXME", "NOTE", "HACK", "XXX" };
        for (String w : stop_words) // ignore the doc if it contains any stop word
            if (str.contains(w))
                return null;
        str = str.replaceAll("<[^>]*>", ""); // remove HTML tags
        String[] splits = str.split("[\\p{Punct}\\p{Space}]+");
        List<String> ret = new ArrayList<>();
        for (String s : splits) { // split by camel case and make everything lower
            List<String> cc = Arrays.asList(StringUtils.splitByCharacterTypeCamelCase(s));
            cc = cc.stream().map(c -> c.toLowerCase()).collect(Collectors.toList());
            ret.add(String.join(" ", cc));
        }
        return String.join(" ", ret);
    }
}
