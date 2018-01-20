package com.raqun;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes({
        "com.raqun.PiriActivity",
        "com.raqun.PiriParam",
})
public final class PiriProcessor extends AbstractProcessor {

    //TODO remove static package name implementation
    private static final String PACKAGE_NAME = "com.raqun.piri.sample";

    private static final String CLASS_NAME_INTENT_CREATOR_SUFFIX = "IntentCreator";

    private static final ClassName intentClass = ClassName.get("android.content", "Intent");
    private static final ClassName contextClass = ClassName.get("android.content", "Context");
    private static final ClassName nonNullAnnotation = ClassName.get("android.support.annotation", "NonNull");

    private static final ClassName intentCreatorClass = ClassName.get(PACKAGE_NAME, "IntentCreator");
    private static final ClassName abstractIntentCreatorClass = ClassName.get(PACKAGE_NAME, "AbstractIntentCreator");

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        EnvironmentUtil.init(processingEnvironment);
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        try {
            createIntentCreator();
            createAbstractIntentCreator();
            return processActivities(roundEnvironment);
        } catch (IOException e) {
            return false;
        }
    }

    private boolean processActivities(RoundEnvironment roundEnv) {
        final Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(PiriActivity.class);

        if (Utils.isNullOrEmpty(elements)) {
            return true;
        }

        for (Element element : elements) {
            if ((element.getKind() != ElementKind.CLASS) || !EnvironmentUtil.isActivity(element.asType())) {
                EnvironmentUtil.logError("PiriActivity can only be used for Activity classes!", element);
                return false;
            }

            try {
                generateActivityIntentCreator((TypeElement) element);
            } catch (IOException e) {
                return false;
            }
        }

        return true;
    }

    private void generateActivityIntentCreator(TypeElement element) throws IOException {
        ParameterSpec nonNullContext = ParameterSpec.builder(contextClass, "context")
                .addAnnotation(nonNullAnnotation)
                .build();

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addParameter(nonNullContext)
                .addStatement("super(context)")
                .build();

        final List<KeyElementPair> pairs = findPiriParamFields(element);
        final List<MethodSpec> builderMethods = new ArrayList<>();
        final List<FieldSpec> fields = new ArrayList<>();
        final List<String> createStatements = new ArrayList<>();
        if (!Utils.isNullOrEmpty(pairs)) {
            for (KeyElementPair pair : pairs) {

                final MethodSpec builderMethod = MethodSpec.methodBuilder(pair.key)
                        .addParameter(ClassName.get(pair.element.asType()), pair.key)
                        .addStatement("this." + pair.key + " = " + pair.key)
                        .addStatement("return this")
                        .returns(ClassName.get(PACKAGE_NAME, element.getSimpleName() + "IntentCreator"))
                        .build();

                final FieldSpec field = FieldSpec
                        .builder(ClassName.get(pair.element.asType()), pair.key, Modifier.PRIVATE)
                        .build();

                CodeBlock block = CodeBlock.of("intent.putExtra($S, $L)", pair.key, pair.key);

                createStatements.add(block.toString());
                builderMethods.add(builderMethod);
                fields.add(field);
            }
        }

        MethodSpec.Builder create = MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC)
                .returns(intentClass)
                .addStatement("$T $L = new $T($L," + element.getQualifiedName() + ".class)",
                        intentClass,
                        "intent",
                        intentClass,
                        "context");
                for (String statement : createStatements) {
                    create.addStatement(statement);
                }
                create.addStatement("return $L", "intent");

        final TypeSpec activityIntentCreator =
                TypeSpec.classBuilder(element.getSimpleName() + CLASS_NAME_INTENT_CREATOR_SUFFIX)
                .addModifiers(Modifier.PUBLIC)
                .superclass(abstractIntentCreatorClass)
                .addFields(fields)
                .addMethod(constructor)
                .addMethod(create.build())
                .addMethods(builderMethods)
                .build();

        EnvironmentUtil.generateFile(activityIntentCreator, PACKAGE_NAME);
    }

    private List<KeyElementPair> findPiriParamFields(Element piriClass) {
        final List<? extends Element> piriClassElements = piriClass.getEnclosedElements();
        if (Utils.isNullOrEmpty(piriClassElements)) return null;

        final List<KeyElementPair> pairs = new ArrayList<>();
        for (Element element : piriClassElements) {
            final PiriParam piriAnnotation = element.getAnnotation(PiriParam.class);

            if ((piriAnnotation != null) && (element.getKind() == ElementKind.FIELD)) {
                if (Utils.isNullOrEmpty(piriAnnotation.key())) {
                    EnvironmentUtil.logWarning("Using PiriParam Annotation without a Key! Field'll be ignored! " +
                            element.getSimpleName() + " in " + element.getSimpleName(), element);
                    continue;
                }
                pairs.add(new KeyElementPair(piriAnnotation.key(), element));
            }
        }

        return pairs;
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
