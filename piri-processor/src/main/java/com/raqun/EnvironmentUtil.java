package com.raqun;

import com.squareup.javapoet.JavaFile;

import java.io.IOException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

/**
 * Created by tyln on 19/05/2017.
 */

public final class EnvironmentUtil {
    private static ProcessingEnvironment processingEnvironment;

    private EnvironmentUtil() {}

    public static void init(ProcessingEnvironment environment) {
        processingEnvironment = environment;
    }

    public static ProcessingEnvironment getProcessingEnvironment() {
        return processingEnvironment;
    }

    public static void logError(String message, Element element) {
        processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    public static void logWarning(String message, Element element) {
        processingEnvironment.getMessager().printMessage(Diagnostic.Kind.WARNING, message, element);
    }

    public static void generateFile(final Generatable generatable) throws IOException {
        JavaFile.builder(generatable.getPackage(), generatable.getTypeSpec())
                .build()
                .writeTo(processingEnvironment.getFiler());
    }

    public static boolean isActivity(TypeMirror typeMirror) {
        final TypeMirror activity = processingEnvironment.getElementUtils()
                .getTypeElement("android.app.Activity").asType();
        return processingEnvironment.getTypeUtils().isAssignable(typeMirror, activity);
    }

    public static boolean isLong(TypeMirror typeMirror) {
        final TypeMirror longType = processingEnvironment.getElementUtils()
                .getTypeElement("java.lang.Long").asType();
        final TypeMirror primitiveLong = processingEnvironment.getTypeUtils().getPrimitiveType(TypeKind.LONG);
        return (processingEnvironment.getTypeUtils().isSameType(typeMirror, longType) || processingEnvironment.getTypeUtils().isSameType(typeMirror, primitiveLong));
    }

    public static boolean isInt(TypeMirror typeMirror) {
        final TypeMirror integerType = processingEnvironment.getElementUtils()
                .getTypeElement("java.lang.Integer").asType();
        final TypeMirror primitiveInt = processingEnvironment.getTypeUtils().getPrimitiveType(TypeKind.INT);
        return (processingEnvironment.getTypeUtils().isSameType(typeMirror, integerType) || processingEnvironment.getTypeUtils().isSameType(typeMirror, primitiveInt));
    }

    public static boolean isString(TypeMirror typeMirror) {
        final TypeMirror stringType = processingEnvironment.getElementUtils()
                .getTypeElement("java.lang.String").asType();
        return processingEnvironment.getTypeUtils().isSameType(typeMirror, stringType);
    }

    public static boolean isSerializable(TypeMirror typeMirror) {
        TypeMirror serializable = processingEnvironment.getElementUtils()
                .getTypeElement("java.io.Serializable").asType();
        return processingEnvironment.getTypeUtils().isAssignable(typeMirror, serializable);
    }
}
