package com.jforex.programming.order.task.params.basic;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Consumer;

import com.jforex.programming.order.task.params.CommonParamsBuilder;

import io.reactivex.functions.Action;

@SuppressWarnings("unchecked")
public abstract class BasicParamsBuilder<T> extends CommonParamsBuilder<T> {

    public int noOfRetries;
    public long delayInMillis;
    protected Consumer<Throwable> errorConsumer = err -> {};
    protected Action startAction = () -> {};
    protected Action completeAction = () -> {};

    public T retryOnReject(final int noOfRetries,
                           final long delayInMillis) {
        this.noOfRetries = noOfRetries;
        this.delayInMillis = delayInMillis;
        return (T) this;
    }

    public T doOnStart(final Action startAction) {
        checkNotNull(startAction);

        this.startAction = startAction;
        return (T) this;
    }

    public T doOnComplete(final Action completeAction) {
        checkNotNull(completeAction);

        this.completeAction = completeAction;
        return (T) this;
    }

    public T doOnError(final Consumer<Throwable> errorConsumer) {
        checkNotNull(errorConsumer);

        this.errorConsumer = errorConsumer;
        return (T) this;
    }
}
