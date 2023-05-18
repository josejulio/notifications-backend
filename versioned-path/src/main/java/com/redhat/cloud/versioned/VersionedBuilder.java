package com.redhat.cloud.versioned;

import com.redhat.cloud.versioned.annotation.AnnotationParamWriter;
import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JAnnotatable;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

public class VersionedBuilder {

    private final JCodeModel codeModel = new JCodeModel();
    private final Messager messager;
    private final Elements elements;
    private final Types types;

    private static final List<Class<? extends Annotation>> bannedAnnotations = List.of(
            Path.class,
            GET.class,
            POST.class,
            PUT.class,
            PATCH.class,
            DELETE.class,
            OPTIONS.class,
            HEAD.class
    );

    private static final String VERSION_REPLACEMENT = "$version";

    public VersionedBuilder(Messager messager, Elements elements, Types types) {
        this.messager = messager;
        this.elements = elements;
        this.types = types;
    }

    public void addClass(Element element) {
        checkForBannedAnnotations(element);

        List<ExecutableElement> methodsWithVersionedPath = collectMethodsWithVersionedPath(element);
        validate(methodsWithVersionedPath);

        Set<Version> versions = collectVersions(element);

        VersionedPath versionedPath = element.getAnnotation(VersionedPath.class);

        Version classVersion = new Version(versionedPath.sinceVersion());

        for (Version targetVersion: versions) {
            try {
                PackageElement packageElement = elements.getPackageOf(element);
                String packageName = packageElement.getQualifiedName().toString();
                String nonPackage = element.asType().toString().replace(packageName + ".",  "").replace(".", "_");

                String name = packageName + "." + nonPackage + "V" + targetVersion.toMinorVersionString().replace(".", "_");
                JDefinedClass definedClass = codeModel._class(name);
                definedClass.annotate(Path.class).param("value", versionedPath.path().replace(VERSION_REPLACEMENT, targetVersion.toMinorVersionString()));
                definedClass._extends(codeModel.directClass(element.asType().toString()));

                List<ExecutableElement> targetMethods = collectMethodsForTargetVersion(methodsWithVersionedPath, targetVersion, classVersion);

                for (ExecutableElement method : targetMethods) {
                    addMethod(method, definedClass, targetVersion);
                }
            } catch (JClassAlreadyExistsException classAlreadyExistsException) {
                throw new RuntimeException("Class already exists, please rename it", classAlreadyExistsException);
            }
        }
    }

    public void build(CodeWriter codeWriter) throws IOException {
        codeModel.build(codeWriter);
    }

    private Class<? extends Annotation> toRestMethodAnnotation(VersionedMethod.HttpMethod versionedMethod) {
        return switch (versionedMethod) {
            case GET -> GET.class;
            case PUT -> PUT.class;
            case HEAD -> HEAD.class;
            case DELETE -> DELETE.class;
            case OPTIONS -> OPTIONS.class;
            case PATCH -> PATCH.class;
            case POST -> POST.class;
        };
    }

    protected void addMethod(ExecutableElement method, JDefinedClass jDefinedClass, Version version) {

        JType returnValue = resolveTypeMirror(method.getReturnType());

        JMethod jMethod = jDefinedClass
                .method(JMod.PUBLIC, returnValue, method.getSimpleName().toString());

        VersionedPath versionedPath = method.getAnnotation(VersionedPath.class);

        jMethod.annotate(Override.class);
        jMethod.annotate(Path.class).param("value", versionedPath.path().replace(VERSION_REPLACEMENT, version.toMinorVersionString()));
        jMethod.annotate(toRestMethodAnnotation(getHttpMethod(method)));

        writeAnnotations(method, jMethod);
        List<JVar> params = writeMethodParams(method, jMethod);

        JInvocation supercall = JExpr._super().invoke(method.getSimpleName().toString());

        for (JVar param: params) {
            supercall.arg(param);
        }

        if (returnValue.equals(codeModel.VOID)) {
            jMethod.body().add(supercall);
        } else {
            jMethod.body()._return(supercall);
        }
    }


    protected List<JVar> writeMethodParams(ExecutableElement method, JMethod jMethod) {

        List<JVar> params = new ArrayList<>();

        for (VariableElement param: method.getParameters()) {

            JVar jvar = jMethod.param(
                    resolveTypeMirror(param.asType()),
                    param.getSimpleName().toString()
            );

            writeAnnotations(param, jvar);
            params.add(jvar);
        }

        return params;
    }

    protected JType resolveTypeMirror(TypeMirror typeMirror) {
        if (typeMirror instanceof DeclaredType declaredType) {
            JClass jClass = codeModel.ref(types.erasure(declaredType).toString());

            if (declaredType.getTypeArguments().size() > 0) {
                return jClass.narrow(
                        declaredType.getTypeArguments().stream().map(typedArgument -> (JClass) resolveTypeMirror(typedArgument)).collect(Collectors.toList())
                );
            }

            return jClass;
        }

        return switch (typeMirror.getKind()) {
            case CHAR -> codeModel.CHAR;
            case BYTE -> codeModel.BYTE;
            case INT -> codeModel.INT;
            case LONG -> codeModel.LONG;
            case VOID -> codeModel.VOID;
            case FLOAT -> codeModel.FLOAT;
            case SHORT -> codeModel.SHORT;
            case DOUBLE -> codeModel.DOUBLE;
            case BOOLEAN -> codeModel.BOOLEAN;
            default -> throw new RuntimeException("Unexpected type mirror: " + typeMirror);
        };
    }

    protected void writeAnnotations(Element target, JAnnotatable annotatable) {
        List<? extends AnnotationMirror> annotations = target.getAnnotationMirrors();
        for (AnnotationMirror annotationMirror: annotations) {
            if (annotationMirror.getAnnotationType().asElement().asType().toString().equals(VersionedPath.class.getCanonicalName())) {
                continue;
            }

            JAnnotationUse jAnnotationUse = annotatable.annotate(codeModel.directClass(annotationMirror.getAnnotationType().asElement().asType().toString()));

            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry: annotationMirror.getElementValues().entrySet()) {
                String key = entry.getKey().getSimpleName().toString();
                writeAnnotationValue(new AnnotationParamWriter(key, jAnnotationUse), entry.getValue());
            }
        }
    }

    protected void writeAnnotationMirror(AnnotationParamWriter annotationParamWriter, AnnotationMirror annotationMirror) {
        try {
            Class<? extends Annotation> klass = (Class<? extends Annotation>) Class.forName(annotationMirror.getAnnotationType().asElement().asType().toString());
            JAnnotationUse jAnnotationUse = annotationParamWriter.writeClass(klass);
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry: annotationMirror.getElementValues().entrySet()) {
                String key = entry.getKey().getSimpleName().toString();
                writeAnnotationValue(new AnnotationParamWriter(key, jAnnotationUse), entry.getValue());
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected void writeAnnotationValue(AnnotationParamWriter annotationParamWriter, AnnotationValue annotationValue) {
        Object value = annotationValue.getValue();

        if (value instanceof Number number) {
            writeAnnotationNumber(annotationParamWriter, number);
        } else if (value instanceof String) {
            annotationParamWriter.writeString(value.toString());
        } else if (value instanceof TypeMirror typeMirror) {
            writeAnnotationTypeMirror(annotationParamWriter, typeMirror);
        } else if (value instanceof VariableElement variableElement) {
            // annotationParamWriter.writeString(variableElement.getConstantValue().toString());
            //annotationParamWriter.writeEnum(codeModel.directClass(variableElement.asType().toString()) );
            // annotationParamWriter.writeJType(resolveTypeMirror(variableElement.asType()));

            annotationParamWriter.writeEnum(Enum.valueOf((Class<? extends Enum>) resolveClass(variableElement), variableElement.getSimpleName().toString()));


            // throw new RuntimeException("VariableElement is:"+ value);
        } else if (value instanceof AnnotationMirror annotationMirror) {
            writeAnnotationMirror(annotationParamWriter, annotationMirror);
        } else if (value instanceof List<?>) {
            AnnotationParamWriter arrayWriter = annotationParamWriter.writeArray();

            for (AnnotationValue val: (List<? extends AnnotationValue>) value) {
                writeAnnotationValue(arrayWriter, val);
            }
        }
    }

    private Class<?> resolveClass(Element element) {
        Stack<String> stack = new Stack<>();

        if (!element.getKind().isClass() && !element.getKind().equals(ElementKind.ANNOTATION_TYPE)) {
            element = element.getEnclosingElement();
        }

        if (element.getKind().isClass()) {
            stack.push(element.getSimpleName().toString());
        } else {
            throw new RuntimeException("Unable to resolve class");
        }

        while (!element.getKind().equals(ElementKind.PACKAGE)) {
            element = element.getEnclosingElement();

            if (element.getKind().isClass() || element.getKind().equals(ElementKind.ANNOTATION_TYPE)) {
                stack.push("$");
                stack.push(element.getSimpleName().toString());
            } else {
                stack.push(".");
                stack.push(element.asType().toString());
            }
        }

        StringBuilder builder = new StringBuilder();
        while (!stack.empty()) {
            builder.append(stack.pop());
        }

        try {
            return Class.forName(builder.toString());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected void writeAnnotationTypeMirror(AnnotationParamWriter annotationParamWriter, TypeMirror typeMirror) {
        annotationParamWriter.writeJType(resolveTypeMirror(typeMirror));
    }

    protected void writeAnnotationNumber(AnnotationParamWriter annotationParamWriter, Number number) {
        if (number instanceof Integer) {
            annotationParamWriter.writeInteger(number.intValue());
        } else if (number instanceof Float) {
            annotationParamWriter.writeFloat(number.floatValue());
        } else if (number instanceof Byte) {
            annotationParamWriter.writeByte(number.byteValue());
        } else if (number instanceof Double) {
            annotationParamWriter.writeDouble(number.doubleValue());
        } else if (number instanceof Long) {
            annotationParamWriter.writeLong(number.longValue());
        } else if (number instanceof Short) {
            annotationParamWriter.writeShort(number.shortValue());
        } else {
            throw new RuntimeException("Unexpected number type: " + number);
        }
    }

    protected Set<Version> collectVersions(Element parentElement) {
        Set<Version> versionSet = new HashSet<>();

        String sinceClassVersion = parentElement.getAnnotation(VersionedPath.class).sinceVersion();
        if (sinceClassVersion.isBlank()) {
            throw new RuntimeException("`sinceVersion` is required for class: " + parentElement.asType().toString());
        }

        Version classVersion = new Version(
                parentElement.getAnnotation(VersionedPath.class).sinceVersion()
        );

        versionSet.add(classVersion);

        for (Element element: collectMethodsWithVersionedPath(parentElement)) {
            String methodVersion = element
                    .getAnnotation(VersionedPath.class)
                    .sinceVersion();

            versionSet.add(methodVersion.isBlank() ? classVersion : new Version(methodVersion));
        }

        return versionSet;
    }

    protected List<ExecutableElement> collectMethodsForTargetVersion(List<ExecutableElement> methods, Version targetVersion, Version defaultVersion) {
        Map<String, ExecutableElement> targetMethods = new HashMap<>();

        for (ExecutableElement method : methods) {
            Version methodVersion = getVersion(method, defaultVersion);

            if (methodVersion.compareTo(targetVersion) <= 0) {
                String key = computeKeyForMethod(method);
                ExecutableElement presentExecutableElement = targetMethods.get(key);

                if (presentExecutableElement == null) {
                    targetMethods.put(key, method);
                } else {
                    Version presentVersion = getVersion(presentExecutableElement, defaultVersion);

                    if (presentVersion.equals(methodVersion)) {
                        throw new RuntimeException("Multiple methods for the same endpoint/version: " + key);
                    }

                    if (presentVersion.compareTo(methodVersion) < 0) {
                        targetMethods.put(key, method);
                    }
                }
            }
        }

        return List.copyOf(targetMethods.values());
    }

    protected List<ExecutableElement> collectMethodsWithVersionedPath(Element classElement) {
        return classElement
                .getEnclosedElements()
                .parallelStream()
                .filter(element -> element.getKind().equals(ElementKind.METHOD))
                .filter(element -> element.getAnnotation(VersionedPath.class) != null)
                .map(element -> (ExecutableElement) element)
                .collect(Collectors.toList());
    }

    protected void validate(List<ExecutableElement> elements) {
        for (Element enclosedElement: elements) {
            // Error: Method is not public
            if (!enclosedElement.getModifiers().contains(Modifier.PUBLIC)) {
                throw new RuntimeException("Method is not public");
            }

            // Error: Method is final
            if (enclosedElement.getModifiers().contains(Modifier.FINAL)) {
                throw new RuntimeException("Method is final");
            }

            checkForBannedAnnotations(enclosedElement);
        }
    }

    private void checkForBannedAnnotations(Element element) {
        for (Class<? extends Annotation> annotation: bannedAnnotations) {
            if (element.getAnnotation(annotation) != null) {
                throw new RuntimeException("Annotation %s not allowed in %s".formatted(annotation, element.getSimpleName().toString()));
            }
        }
    }

    protected String computeKeyForMethod(ExecutableElement method) {
        VersionedPath versionedPath = method.getAnnotation(VersionedPath.class);


        return "%s_%s".formatted(getHttpMethod(method).name(), versionedPath.path());
    }

    private VersionedMethod.HttpMethod getHttpMethod(Element element) {
        VersionedMethod versionedMethod = element.getAnnotation(VersionedMethod.class);
        if (versionedMethod == null) {
            return VersionedMethod.HttpMethod.GET;
        }

        return versionedMethod.value();
    }

    protected Version getVersion(Element element, Version defaultVersion) {
        VersionedPath versionedPath = element.getAnnotation(VersionedPath.class);
        if (versionedPath == null || versionedPath.sinceVersion().isBlank()) {
            return defaultVersion;
        }

        return new Version(versionedPath.sinceVersion());
    }
}
