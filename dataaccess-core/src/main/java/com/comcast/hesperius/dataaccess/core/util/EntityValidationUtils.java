/*
 * If not stated otherwise in this file or this component's Licenses.txt file the
 * following copyright and licenses apply:
 *
 * Copyright 2018 RDK Management
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
package com.comcast.hesperius.dataaccess.core.util;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.*;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.*;

public final class EntityValidationUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityValidationUtils.class);


    private static final ImmutableMap<Class<?>, Validator> VALIDATORS_CACHE;
    private static final Properties validationMessages = new Properties();

    static {
        final Set<Class<?>> beans = AnnotationScanner.getAnnotatedClasses(new Class[]{Validated.class}, CoreUtil.dsconfig.getValidatorsBasePackage());
        ImmutableMap.Builder<Class<?>, Validator> validatorCacheBuilder = ImmutableMap.builder();
        for (final Class<?> obj : beans) {
            final Validator validator;
            try {
                validator = (Validator) obj.newInstance();
                final Validated annotation = validator.getClass().getAnnotation(Validated.class);
                for (final Class<?> klass : annotation.value()) {
                    validatorCacheBuilder.put(klass, validator);
                    LOGGER.info("registered {} as validator for {}", klass.getSimpleName(), obj.getSimpleName());
                }
            } catch (InstantiationException e) {
                LOGGER.error("could not instantiate {}", obj.getSimpleName());
            } catch (IllegalAccessException e) {
                LOGGER.error("inappropriate access permissions or no default constructor in {}", obj.getSimpleName());
            }
        }
        VALIDATORS_CACHE = validatorCacheBuilder.build();

        try {
            InputStream resourceInputStream = EntityValidationUtils.class.getClassLoader().getResourceAsStream("validation.properties");
            if (resourceInputStream == null) throw new IOException();
            validationMessages.load(resourceInputStream);
        } catch (IOException e) {
            LOGGER.error("Could not load validation.properties from classpath");
        }
    }

    public static Errors validate(final Object entity) {
        final String className = entity.getClass().getSimpleName();
        final Errors errors = new BeanPropertyBindingResult(entity, StringUtils.uncapitalize(className));
        return validate(entity, errors);
    }

    public static <T> List<Errors> validate(final Collection<T> entries) {
        final List<Errors> result = new ArrayList<Errors>();
        for (T entity : entries) {
            final Errors errors = validate(entity);
            result.add(errors);
        }
        return result;
    }

    public static Errors validate(final Object entity, final Errors errors) {
        final Validator validator = VALIDATORS_CACHE.get(entity.getClass());
        if (validator != null) {
            validator.validate(entity, errors);
        } else {
            LOGGER.debug("Could not get validator for " + entity.getClass());
        }
        return errors;
    }

    public static Map<String, String> resolveMessages(final Errors errors) {
        final Map<String, String> result = new HashMap<String, String>();
        for (final FieldError error : errors.getFieldErrors()) {
            final String message = validationMessages.getProperty(error.getCode(), "not valid");
            result.put(error.getField(), new MessageFormat(message).format(error.getArguments()));
        }
        return result;
    }

    public static void validateForSave(final Object entity) throws ValidationException {
        if (entity == null) {
            throw new ValidationException("entity is null");
        }
        Errors errors = validate(entity);
        if (errors.hasErrors()) {
            throw new ValidationException(resolveMessages(errors).toString());
        }
    }

    /**
     * trugger class to be loaded
     */
    public static void init(){};
}
