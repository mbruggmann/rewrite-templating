/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.template.internal;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.TreeScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ImportDetector {

    /**
     * Locate types that are directly referred to by name in the
     * lambda body and therefore need an import in the template.
     *
     * @return The list of imports to add.
     */
    public static List<String> imports(JCTree.JCLambda lambda) {
        List<String> imports = new ArrayList<>();

        new TreeScanner() {
            @Override
            public void scan(JCTree tree) {
                JCTree maybeFieldAccess = tree;
                if (maybeFieldAccess instanceof JCFieldAccess &&
                    Character.isUpperCase(((JCFieldAccess) maybeFieldAccess).getIdentifier().toString().charAt(0))) {
                    while (maybeFieldAccess instanceof JCFieldAccess) {
                        maybeFieldAccess = ((JCFieldAccess) maybeFieldAccess).getExpression();
                        if (maybeFieldAccess instanceof JCIdent &&
                            Character.isUpperCase(((JCIdent) maybeFieldAccess).getName().toString().charAt(0))) {
                            // this might be a fully qualified type name, so we don't want to add an import for it
                            // and returning will skip the nested identifier which represents just the class simple name
                            return;
                        }
                    }
                }

                if (tree instanceof JCIdent) {
                    if (tree.type == null || !(tree.type.tsym instanceof Symbol.ClassSymbol)) {
                        return;
                    }
                    String fqn = ((Symbol.ClassSymbol) tree.type.tsym).flatname.toString();
                    if (fqn.substring(fqn.lastIndexOf('.') + 1).replace('$', '.')
                            .equals(((JCIdent) tree).getName().toString())) {
                        imports.add(fqn.replace('$', '.'));
                    }
                }

                super.scan(tree);
            }
        }.scan(lambda);

        return imports;
    }
}