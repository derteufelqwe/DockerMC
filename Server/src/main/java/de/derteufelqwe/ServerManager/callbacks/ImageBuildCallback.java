package de.derteufelqwe.ServerManager.callbacks;

import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.model.BuildResponseItem;
import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.List;

/**
 * A custom callback for image building, which prints the current push progress in the console (NOT the logger)
 */
public class ImageBuildCallback extends BuildImageResultCallback {

    private String buffer = "";

    @Override
    public void onNext(BuildResponseItem item) {
        super.onNext(item);
        if (item.getStream() != null)
            buffer += item.getStream();

        this.checkLogbuffer();
    }

    @Override
    public void onComplete() {
        super.onComplete();
        System.out.println(buffer.trim());
    }

    /**
     * Analyzes the current log buffer, prints all valid lines into the console and removes them from the log buffer.
     */
    @SneakyThrows
    private void checkLogbuffer() {
        Thread.sleep(50);
        this.buffer = buffer
                .replaceAll("\\n --->.+", "")
                .replaceAll("Removing intermediate container.+\\n", "");

        List<String> oldLines = Arrays.asList(this.buffer.split("\\n"));
        for (int i = 0; i < oldLines.size() - 1; i++) {
            String line = oldLines.get(i);

            if (line.startsWith("Successfully")) {
                System.out.print("\r" + line);
                System.out.flush();
                this.buffer = buffer.replace(line + "\n", "");
            }

            String nextLine = oldLines.get(i + 1);
            if (nextLine.startsWith("Step") || nextLine.startsWith("Successfully")) {
                System.out.print("\r" + line);
                System.out.flush();
                this.buffer = buffer.replace(line + "\n", "");
            }
        }
    }

}
