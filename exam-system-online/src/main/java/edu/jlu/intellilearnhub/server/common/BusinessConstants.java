package edu.jlu.intellilearnhub.server.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

public final class BusinessConstants {
    private BusinessConstants() {

    }


    @AllArgsConstructor
    @Getter
    public enum PaperStatus {
        DRAFT("DRAFT"),
        PUBLISHED("PUBLISHED"),
        STOPPED("STOPPED");

        private final String status;
    }
}
