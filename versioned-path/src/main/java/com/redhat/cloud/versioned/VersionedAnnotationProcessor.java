package com.redhat.cloud.versioned;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class VersionedAnnotationProcessor extends AbstractProcessor {

    private Messager messager;
    private Filer filer;
    private final Set<String> processedClasses = new HashSet<>();
    private Types types;
    private Elements elements;

    @Override
    public synchronized void init(ProcessingEnvironment environment) {
        messager = environment.getMessager();
        filer = environment.getFiler();
        elements = environment.getElementUtils();
        types = environment.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        VersionedBuilder versionedBuilder = new VersionedBuilder(messager, elements, types);

        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedElements = roundEnvironment.getElementsAnnotatedWith(annotation);
            for (Element element : annotatedElements) {
                // Only triggered by class elements
                if (element.getKind().equals(ElementKind.CLASS)) {
                    processedClasses.add(element.asType().toString());
                    versionedBuilder.addClass(element);
                } else {
                    if (!processedClasses.contains(element.getEnclosingElement().asType().toString())) {
                        messager.printMessage(
                                Diagnostic.Kind.ERROR,
                                "Container class (" +
                                        element.getEnclosingElement().asType().toString() +
                                        ") of element ("
                                        + element.getSimpleName().toString() +
                                        ") does not have the @VersionedPath annotation.",
                                element
                        );
                    }
                }
            }
        }

        try {
            versionedBuilder.build(new AnnotationProcessingCodeWriter(filer));
        } catch (IOException ex) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Error writing source files: " + ex);
        }

        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of("com.redhat.cloud.versioned.VersionedPath");
    }
}
