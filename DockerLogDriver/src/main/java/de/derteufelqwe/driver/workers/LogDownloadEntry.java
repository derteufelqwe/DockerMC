package de.derteufelqwe.driver.workers;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.concurrent.Future;

@AllArgsConstructor
@Getter
public class LogDownloadEntry {

    private LogConsumer consumer;
    private Future<?> future;

}
