package com.raqun;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
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
    private static final String CLASS_NAME_INTENT_CREATOR_SUFFIX = "IntentCreator";

    private static final ClassName intentClass = ClassName.get("android.content", "Intent");
    private static final ClassName contextClass = ClassName.get("android.content", "Context");
    private static final ClassName nonNullAnnotation = ClassName.get("android.support.annotation", "NonNull");

    private static final ParameterSpec nonNullContextParam = ParameterSpec.builder(contextClass, "context").addAnnotation(nonNullAnnotation).build();

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
        /* Get every element that is annotated with PiriActivity */
        final Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(PiriActivity.class);

        if (Utils.isNullOrEmpty(elements)) {
            return true;
        }

        for (Element element : elements) {
            /* Check if this element is a class AND inherits from Activity */
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
        /* The package of the activity we are making an intent creator for */
        final String activityPackageName = EnvironmentUtil.getProcessingEnvironment()
                .getElementUtils()
                .getPackageOf(element)
                .toString();

        /* Simple name of the activity */
        final String activitySimpleName = element.getSimpleName().toString();

        /* Activity intent creator class name (The one to be built) */
        final ClassName activityIntentCreatorClassName =
                ClassName.get(activityPackageName,activitySimpleName + "IntentCreator");

        /* TODO: Add required params in constructor */
        final MethodSpec constructor = MethodSpec.constructorBuilder()
                .addParameter(nonNullContextParam)
                .addStatement("$L.$L = new $T($L, $T.class)",
                        "this",
                        "intent",
                        intentClass,
                        "context",
                        element.asType())
                .build();

        final FieldSpec intentField = FieldSpec.builder(intentClass, "intent")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build();

        /* Grab Piri params */
        final List<KeyElementPair> pairs = findPiriParamFields(element);

        /* Lists of items we will build our intent creator with */
        final List<MethodSpec> builderMethods = new ArrayList<>();
        final List<FieldSpec> fields = new ArrayList<>();
        fields.add(intentField);

        /* Start building the `create` method of the intent creator. We will
           add statements as we loop through the KeyElementPairs */
        MethodSpec.Builder create = MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC)
                .returns(intentClass);

        if (!Utils.isNullOrEmpty(pairs)) {
            for (KeyElementPair pair : pairs) {

                /* The name of the variable itself (int i; would be "i" */
                final String elementName = pair.element.getSimpleName().toString();

                /* Create a builder method that will be added to the intent creator */
                final MethodSpec builderMethod = MethodSpec.methodBuilder(pair.element.getSimpleName().toString())
                        .addParameter(ClassName.get(pair.element.asType()), elementName)
                        .addStatement("this." + elementName + " = " + elementName)
                        .addStatement("return this")
                        .returns(activityIntentCreatorClassName)
                        .build();

                /* Create the field that will hold a param for the intent creator
                    TODO: Make final if it's a required param */
                final FieldSpec field = FieldSpec
                        .builder(ClassName.get(pair.element.asType()).box(), elementName, Modifier.PRIVATE)
                        .initializer("$L", "null")
                        .build();

                /* Add a statement in the create method to add the param to the intent if not null */
                create.beginControlFlow("if ($L != null)", elementName)
                        .addStatement("intent.putExtra($S, $L)", pair.key, elementName)
                        .endControlFlow();
                /* Add corresponding builder methods and fields to be added to the intent creator */
                builderMethods.add(builderMethod);
                fields.add(field);
            }
        }

        create.addStatement("return $L", "intent");

        MethodSpec addFlags = MethodSpec.methodBuilder("addFlags")
                .varargs(true)
                .addParameter(ArrayTypeName.of(TypeName.INT), "flags")
                .returns(activityIntentCreatorClassName)
                .beginControlFlow("for($L $L : $L)", "int", "flag", "flags")
                .addStatement("$L.$L($L)", "intent", "addFlags", "flag")
                .endControlFlow()
                .addStatement("$L $L", "return", "this")
                .build();

        /* Finally, build the intent creator class! */
        final TypeSpec activityIntentCreator =
                TypeSpec.classBuilder(element.getSimpleName() + CLASS_NAME_INTENT_CREATOR_SUFFIX)
                .addModifiers(Modifier.PUBLIC)
                .addFields(fields)
                .addMethod(constructor)
                .addMethod(create.build())
                .addMethods(builderMethods)
                .addMethod(addFlags)
                .build();

        /* And generate it */
        EnvironmentUtil.generateFile(activityIntentCreator, activityPackageName);
    }

    private List<KeyElementPair> findPiriParamFields(Element piriClass) {
        /* Grab a list of all the elements of this activity */
        final List<? extends Element> piriClassElements = piriClass.getEnclosedElements();

        /* Return if there are none */
        if (Utils.isNullOrEmpty(piriClassElements)) {
            return null;
        }

        final List<KeyElementPair> pairs = new ArrayList<>();

        /* Loop through all the elements and find any that are PiriParam annotated */
        for (Element element : piriClassElements) {
            final PiriParam piriAnnotation = element.getAnnotation(PiriParam.class);

            /* Check if it's PiriParam annotated. If so, also ensure we are dealing with a field */
            if ((piriAnnotation != null) && (element.getKind() == ElementKind.FIELD)) {
                /* Log a warning and ignore the param if the dev didn't provide a key */
                if (Utils.isNullOrEmpty(piriAnnotation.key())) {
                    EnvironmentUtil.logWarning("Using PiriParam Annotation without a Key! Field'll be ignored! " +
                            element.getSimpleName() + " in " + element.getSimpleName(), element);
                    continue;
                }

                /* Add to our list of PiriParam KeyElementPairs */
                pairs.add(new KeyElementPair(piriAnnotation.key(), element));
            }
        }

        return pairs;
    }
}
