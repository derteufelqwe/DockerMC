package de.derteufelqwe.ServerManager.setup;

import lombok.AllArgsConstructor;
import lombok.Data;

public interface Validatable {

    ValidationResponse valid();

    @Data
    @AllArgsConstructor
    public class ValidationResponse {

        private boolean valid;
        private String name;
        private String reason;

    }
}
