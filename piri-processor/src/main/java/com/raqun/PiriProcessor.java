package com.raqun;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes({
        "com.raqun.PiriActivity",
        "com.raqun.PiriParam",
})
public final class PiriProcessor extends AbstractProcessor {

    //TODO remove static package name implementation
    private static final String PACKAGE_NAME = "com.raqun.piri.sample";

    private static final String CLASS_NAME_INTENT_FACTORY = "PiriIntentFactory";

    private static final ClassName intentClass = ClassName.get("android.content", "Intent");
    private static final ClassName contextClass = ClassName.get("android.content", "Context");
    private static final ClassName nonNullAnnotation = ClassName.get("android.support.annotation", "NonNull");

    private static final ClassName intentCreatorClass = ClassName.get(PACKAGE_NAME, "IntentCreator");

    private static final String METHOD_PREFIX_NEW_INTENT = "newIntentFor";

    private static final String PARAM_NAME_CONTEXT = "context";
    private static final String CLASS_SUFFIX = ".class";

    private final List<MethodSpec> newIntentMethodSpecs = new ArrayList<>();

    private boolean HALT = false;
    private int round = -1;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {

        round++;

        if (round == 0) {
            EnvironmentUtil.init(processingEnv);
        }

        if (!processAnnotations(roundEnvironment)) {
            return HALT;
        }

        if (roundEnvironment.processingOver()) {
            try {
                createIntentFactory();
                createIntentCreator();
                createAbstractIntentCreator();
                HALT = true;
            } catch (IOException ex) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, ex.toString());
            }
        }

        return HALT;
    }

    private boolean processAnnotations(RoundEnvironment roundEnv) {
        return processActivities(roundEnv);
    }

    private boolean processActivities(RoundEnvironment roundEnv) {
        final Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(PiriActivity.class);

        if (Utils.isNullOrEmpty(elements)) {
            return true;
        }

        for (Element element : elements) {
            if (element.getKind() != ElementKind.CLASS) {
                EnvironmentUtil.logError("PiriActivity can only be used for classes!");
                return false;
            }

            if (!generateNewIntentMethod((TypeElement) element)) {
                return false;
            }
        }

        return true;
    }

    private boolean generateNewIntentMethod(TypeElement element) {
        final MethodSpec.Builder navigationMethodSpecBuilder = MethodSpec
                .methodBuilder(METHOD_PREFIX_NEW_INTENT + element.getSimpleName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(intentClass)
                .addParameter(contextClass, PARAM_NAME_CONTEXT);

        final List<KeyElementPair> pairs = findPiriParamFields(element);
        navigationMethodSpecBuilder.addStatement("final $T intent = new $T($L, $L)",
                intentClass,
                intentClass,
                PARAM_NAME_CONTEXT,
                element.getSimpleName() + CLASS_SUFFIX);
        if (!Utils.isNullOrEmpty(pairs)) {
            for (KeyElementPair pair : pairs) {
                navigationMethodSpecBuilder.addParameter(ClassName.get(pair.element.asType()),
                        pair.element.getSimpleName().toString());
                navigationMethodSpecBuilder.addStatement("intent.putExtra($S, $L)",
                        pair.key,
                        pair.element);
            }
        }
        navigationMethodSpecBuilder.addStatement("return intent");

        newIntentMethodSpecs.add(navigationMethodSpecBuilder.build());
        return true;
    }

    private List<KeyElementPair> findPiriParamFields(Element parent) {
        final List<? extends Element> citizens = parent.getEnclosedElements();
        if (Utils.isNullOrEmpty(citizens)) return null;

        final List<KeyElementPair> pairs = new ArrayList<>();
        for (Element citizen : citizens) {
            final PiriParam piriAnnotation = citizen.getAnnotation(PiriParam.class);
            if (piriAnnotation != null) {
                if (Utils.isNullOrEmpty(piriAnnotation.key())) {
                    EnvironmentUtil.logWarning("Using PiriParam Annotation without a Key! Field'll be ignored! " +
                            citizen.getSimpleName() + " in " + parent.getSimpleName());
                    continue;
                }
                pairs.add(new KeyElementPair(piriAnnotation.key(), citizen));
            }
        }

        return pairs;
    }
    private void createIntentFactory() throws IOException {
        final TypeSpec.Builder builder = TypeSpec.classBuilder(CLASS_NAME_INTENT_FACTORY);
        builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        for (MethodSpec methodSpec : newIntentMethodSpecs) {
            builder.addMethod(methodSpec);
        }

        EnvironmentUtil.generateFile(builder.build(), PACKAGE_NAME);
    }

    private void createIntentCreator() throws IOException {
        TypeSpec intentCreator = TypeSpec.interfaceBuilder("IntentCreator")
                .addModifiers(Modifier.PUBLIC)
                .addMethod(MethodSpec.methodBuilder("create")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(intentClass)
                        .build())
                .build();

        EnvironmentUtil.generateFile(intentCreator, PACKAGE_NAME);
    }

    private void createAbstractIntentCreator() throws IOException {
        ParameterSpec nonNullContext = ParameterSpec.builder(contextClass, "context")
                .addAnnotation(nonNullAnnotation)
                .build();

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addParameter(nonNullContext)
                .addStatement("this.context = context")
                .build();

        TypeSpec abstractIntentCreator = TypeSpec.classBuilder("AbstractIntentCreator")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addSuperinterface(intentCreatorClass)
                .addField(contextClass, "context", Modifier.PROTECTED, Modifier.FINAL)
                .addMethod(constructor)
                .build();

        EnvironmentUtil.generateFile(abstractIntentCreator, PACKAGE_NAME);
    }
}
