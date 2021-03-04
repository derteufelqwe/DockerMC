package de.derteufelqwe.ServerManager.callbacks;

import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.ErrorDetail;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.api.model.ResponseItem;

import javax.print.Doc;

/**
 * A custom callback for image pushing, which prints the current push progress in the console (NOT the logger)
 * and supports sync waiting for completion.
 */
public class ImagePushCallback extends ResultCallbackTemplate<ImagePushCallback, PushResponseItem> {

    private ResponseItem.ErrorDetail error;
    private Throwable throwable;
    private boolean completed = false;

    @Override
    public void onNext(PushResponseItem object) {
        if (object.isErrorIndicated())
            this.error = object.getErrorDetail();

        ResponseItem.ProgressDetail progress = object.getProgressDetail();
        if (progress == null || progress.equals(new ResponseItem.ProgressDetail()))
            return;
        System.out.print("\r" + object.getProgress());
        System.out.flush();
    }

    @Override
    public void onError(Throwable throwable) {
        super.onError(throwable);
        this.throwable = throwable;
    }

    @Override
    public void onComplete() {
        this.completed = true;
        super.onComplete();
        System.out.println("");
    }

    public void awaitSuccess() throws InterruptedException {
        this.awaitCompletion();

        if (error != null)
            throw new DockerClientException("Image push failed with: " + error.getMessage() + "(" + error.getCode() + ").");

        if (throwable != null)
            throw new DockerClientException("Image push failed.", throwable);
    }

}
