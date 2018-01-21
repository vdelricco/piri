package com.raqun;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
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

    public static void generateFile(final TypeSpec typeSpec, String packageName) throws IOException {
        JavaFile.builder(packageName, typeSpec)
                .build()
                .writeTo(processingEnvironment.getFiler());
    }

    public static boolean isActivity(TypeMirror typeMirror) {
        final TypeMirror activity = processingEnvironment.getElementUtils()
                .getTypeElement("android.app.Activity").asType();
        return processingEnvironment.getTypeUtils().isAssignable(typeMirror, activity);
    }
}
