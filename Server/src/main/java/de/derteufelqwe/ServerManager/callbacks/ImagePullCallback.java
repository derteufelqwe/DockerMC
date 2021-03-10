package de.derteufelqwe.ServerManager.callbacks;

import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.api.model.ResponseItem;

import java.util.concurrent.TimeUnit;

public class ImagePullCallback extends ResultCallbackTemplate<ImagePullCallback, PullResponseItem> {

    private ResponseItem.ErrorDetail error;
    private Throwable throwable;
    private boolean completed = false;

    @Override
    public void onNext(PullResponseItem object) {
        if (object.isErrorIndicated())
            this.error = object.getErrorDetail();

        ResponseItem.ProgressDetail progress = object.getProgressDetail();
        if (progress == null || progress.equals(new ResponseItem.ProgressDetail()))
            return;

        // Use normal print so the logs don't get flooded
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
    }

    public void awaitSuccess() throws InterruptedException {
        this.awaitCompletion();

        if (error != null)
            throw new DockerClientException("Image push failed with: " + error.getMessage() + "(" + error.getCode() + ").");

        if (throwable != null)
            throw new DockerClientException("Image push failed.", throwable);
    }

    @Override
    public boolean awaitCompletion(long timeout, TimeUnit timeUnit) throws InterruptedException {
        boolean result = super.awaitCompletion(timeout, timeUnit);

        if (error != null)
            throw new DockerClientException("Image push failed with: " + error.getMessage() + "(" + error.getCode() + ").");

        if (throwable != null)
            throw new DockerClientException("Image push failed.", throwable);

        return result;
    }
}
