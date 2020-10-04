package org.lastnpe.examples.hello;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

public abstract class CompletionStageWrapper<T> implements CompletionStage<T> {

    @Override
    // "Illegal redefinition of parameter executor, inherited method from CompletionStage<T> does not constrain this parameter"
    // This is wrong, because the CompletionStage EEA annotates Executor as @NonNull
    // Workaround @Nullable Executor makes it disappear, but why is this needed?
    // TODO Is this a JDT bug?!
    public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
        return CompletableFuture.completedFuture(null);
    }


}
