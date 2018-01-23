package com.raqun;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes({
        "com.raqun.PiriActivity",
        "com.raqun.PiriParam",
})
public final class PiriProcessor extends AbstractProcessor {
    private final Map<TypeElement, List<KeyElementPair>> activityParamMap = new HashMap<>();

    public static final ClassName intentClass = ClassName.get("android.content", "Intent");
    public static final ClassName contextClass = ClassName.get("android.content", "Context");
    public static final ClassName nonNullAnnotation = ClassName.get("android.support.annotation", "NonNull");

    public static final ParameterSpec nonNullContextParam = ParameterSpec.builder(contextClass, "context").addAnnotation(nonNullAnnotation).build();

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

            activityParamMap.put((TypeElement) element, findPiriParamFields(element));
        }

        for (TypeElement element : activityParamMap.keySet()) {
            try {
                generateIntentCreatorForActivity(element, activityParamMap.get(element));
            } catch (IOException e) {
                return false;
            }
        }

        return true;
    }

    private void generateIntentCreatorForActivity(TypeElement element, List<KeyElementPair> piriParamList) throws IOException {
        final ActivityIntentCreator activityIntentCreator = new ActivityIntentCreator(element, piriParamList);

        /* And generate it */
        EnvironmentUtil.generateFile(activityIntentCreator);
    }

    private List<KeyElementPair> findPiriParamFields(Element piriClass) {
        /* Grab a list of all the elements of this activity */
        final List<? extends Element> piriClassElements = piriClass.getEnclosedElements();
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
                pairs.add(new KeyElementPair(piriAnnotation.key(), piriAnnotation.required(), element));
            }
        }

        return pairs;
    }
}
