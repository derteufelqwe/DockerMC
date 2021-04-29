package de.derteufelqwe.plugin.misc;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.concurrent.Future;

@AllArgsConstructor
@Getter
public class LogDownloadEntry {

    private LogConsumer consumer;
    private Future<?> future;

}
