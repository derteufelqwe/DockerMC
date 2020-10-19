package de.derteufelqwe.commons.config.exceptions;

import de.derteufelqwe.commons.exceptions.BaseException;

/**
 * Raised if it was impossible to walk a path down a YAML file.
 * This is most likely caused by a YAML key not beeing present in the YAML file.
 */
public class YAMLWalkException extends BaseException {

    public YAMLWalkException(String message, Object... args) {
        super(message, args);
    }
}
