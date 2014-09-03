/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.webdemo;

import com.google.common.base.Predicates;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.jet.codegen.ClassBuilderFactories;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;

import java.util.List;

public class ResolveUtils {

    public static BindingContext getBindingContext(@NotNull List<JetFile> files, Project project) {
        AnalyzeExhaust analyzeExhaust = analyzeFile(files, project);
        return analyzeExhaust.getBindingContext();
    }

    public static GenerationState getGenerationState(@NotNull List<JetFile> files, Project project) {
        AnalyzeExhaust analyzeExhaust = analyzeFile(files, project);
        return new GenerationState(
                project,
                ClassBuilderFactories.BINARIES,
                analyzeExhaust.getModuleDescriptor(),
                analyzeExhaust.getBindingContext(),
                files
        );
    }

    private static AnalyzeExhaust analyzeFile(@NotNull List<JetFile> files, Project project) {

        WebDemoLightClassGenerationSupport.getInstanceForCli(project).newBindingTrace();

        CliLightClassGenerationSupport support = WebDemoLightClassGenerationSupport.getInstanceForCli(project);
        BindingTrace sharedTrace = support.getTrace();

        return AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                project,
                files,
                sharedTrace,
                Predicates.<PsiFile>alwaysTrue(),
                support.newModule(),
                null,
                null
        );
    }

    private ResolveUtils() {
    }
}
