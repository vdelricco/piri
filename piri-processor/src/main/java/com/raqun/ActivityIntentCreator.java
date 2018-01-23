package com.raqun;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import static com.raqun.PiriProcessor.intentClass;
import static com.raqun.PiriProcessor.nonNullAnnotation;
import static com.raqun.PiriProcessor.nonNullContextParam;

/**
 * Data class for an Activity IntentCreator.
 */

public class ActivityIntentCreator implements Generatable {
    private static final String CLASS_NAME_INTENT_CREATOR_SUFFIX = "IntentCreator";
    private static final String CREATE_METHOD_NAME = "create";

    private final String packageName;
    private final String simpleName;
    private final ClassName className;

    // Builder for the class itself
    private final TypeSpec.Builder typeSpecBuilder;
    // Builder for constructor
    private final MethodSpec.Builder constructorBuilder;
    // Builder for the create() method
    private final MethodSpec.Builder createMethodBuilder;

    public ActivityIntentCreator(TypeElement element, List<KeyElementPair> piriParamList) {
        this.packageName = EnvironmentUtil.getProcessingEnvironment().getElementUtils().getPackageOf(element).toString();
        this.simpleName = element.getSimpleName().toString();
        this.className = ClassName.get(packageName,simpleName + CLASS_NAME_INTENT_CREATOR_SUFFIX);

        /* Begin creating typespec for the class */
        this.typeSpecBuilder = TypeSpec.classBuilder(className)
                  .addModifiers(Modifier.PUBLIC)
                  .addMethod(getAddFlagsMethod());

        /* Create the intent field which exists in all intent creators */
        final FieldSpec intentField = FieldSpec.builder(intentClass, "intent")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build();

        this.constructorBuilder = MethodSpec.constructorBuilder()
                .addParameter(nonNullContextParam)
                .addStatement("$L.$L = new $T($L, $T.class)",
                        "this",
                        "intent",
                        intentClass,
                        "context",
                        element.asType());

        /* Start building the `create` method of the intent creator. We will
        add statements as we loop through the KeyElementPairs */
        this.createMethodBuilder = MethodSpec.methodBuilder(CREATE_METHOD_NAME)
                .addModifiers(Modifier.PUBLIC)
                .returns(intentClass);

        /* Lists of items we will build our intent creator with */
        final List<MethodSpec> builderMethods = new ArrayList<>();
        final List<FieldSpec> fields = new ArrayList<>();
        fields.add(intentField);

        /* Process all piri params */
        if (!Utils.isNullOrEmpty(piriParamList)) {
            processPiriParams(piriParamList, builderMethods, fields);
        }

        /* Add the final statement to the create method */
        this.createMethodBuilder.addStatement("return $L", "intent");

            /* Finally, build the intent creator class! */
        this.typeSpecBuilder
                .addFields(fields)
                .addMethod(this.constructorBuilder.build())
                .addMethod(this.createMethodBuilder.build())
                .addMethods(builderMethods);
    }

    @Override
    public String getPackage() {
        return packageName;
    }

    @Override
    public TypeSpec getTypeSpec() {
        return typeSpecBuilder.build();
    }

    private void processPiriParams(List<KeyElementPair> piriParamList, List<MethodSpec> builderMethods, List<FieldSpec> fields) {
        for (KeyElementPair pair : piriParamList) {

            /* The name of the variable itself (int i; would be "i") */
            final String elementName = pair.element.getSimpleName().toString();

            /* Create the field that will hold a param for the intent creator */
            final FieldSpec.Builder fieldBuilder = FieldSpec
                    .builder(ClassName.get(pair.element.asType()).box(), elementName, Modifier.PRIVATE);

            /* Process the param based on whether it's required */
            if (pair.required) {
                processRequiredPiriParm(fieldBuilder, pair);
            } else {
                /* Pass builder methods list along because a builder method is generated for optional params */
                processOptionalPiriParam(fieldBuilder, pair, builderMethods);
            }

            /* Add the processed field */
            fields.add(fieldBuilder.build());
        }
    }

    /* If the param is required, accept it in the constructor and don't create builder method */
    private void processRequiredPiriParm(FieldSpec.Builder fieldBuilder, KeyElementPair pair) {
        String name = pair.element.getSimpleName().toString();
        /* Accept and set the param in the constructor */
        constructorBuilder.addParameter(generateRequiredIntentParam(pair.element.asType(), name));
        constructorBuilder.addStatement("$L.$L = $L", "this", name, name);

        /* Set corresponding field to final since it gets set in constructor */
        fieldBuilder.addModifiers(Modifier.FINAL);

        /* Always add required param to intent in create method */
        createMethodBuilder.addStatement("intent.putExtra($S, $L)", pair.key, name);
    }

    private void processOptionalPiriParam(FieldSpec.Builder fieldBuilder, KeyElementPair pair, List<MethodSpec> builderMethods) {
        String name = pair.element.getSimpleName().toString();

        /* Add a builder method for the optional param */
        builderMethods.add(generateOptionalIntentParamBuilderMethod(pair.element.asType(), name));

        /* Set non-required field to null */
        fieldBuilder.initializer("$L", "null");

        /* Add a statement in the create method to add the param to the intent if not null */
        createMethodBuilder.beginControlFlow("if ($L != null)", name)
                .addStatement("intent.putExtra($S, $L)", pair.key, name)
                .endControlFlow();
    }

    private MethodSpec generateOptionalIntentParamBuilderMethod(TypeMirror type, String name) {
        /* Create a builder method that will be added to the intent creator */
        return MethodSpec.methodBuilder(name)
                .addParameter(ClassName.get(type), name)
                .addStatement("this.$L = $L", name, name)
                .addStatement("return this")
                .returns(this.className)
                .build();
    }

    private ParameterSpec generateRequiredIntentParam(TypeMirror type, String name) {
        /* The required param is annotated NonNull */
        return ParameterSpec.builder(ClassName.get(type).box(), name)
                .addAnnotation(nonNullAnnotation)
                .build();
    }

    private MethodSpec getAddFlagsMethod() {
        /* Create addFlags helper method */
        return MethodSpec.methodBuilder("addFlags")
                .varargs(true)
                .addParameter(ArrayTypeName.of(TypeName.INT), "flags")
                .returns(this.className)
                .beginControlFlow("for($L $L : $L)", "int", "flag", "flags")
                .addStatement("$L.$L($L)", "intent", "addFlags", "flag")
                .endControlFlow()
                .addStatement("$L $L", "return", "this")
                .build();
    }
}
