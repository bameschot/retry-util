package com.ameschot.retryutil;

public class Retry {

    /**
     * @see Retry#retry(IRetryableAction, IRetryAssertion, DelayStrategies, RetryConfig)
     */
    public static <T> T retry (IRetryableAction<T> retryableAction, RetryConfig retryConfig){
        return retry(retryableAction,e -> true,DelayStrategies.NORMAL,retryConfig);
    }

    /**
     * @see Retry#retry(IRetryableAction, IRetryAssertion, DelayStrategies, RetryConfig)
     */
    public static <T> T retry (IRetryableAction<T> retryableAction, DelayStrategies delayStrategy, RetryConfig retryConfig){
        return retry(retryableAction,e -> true,delayStrategy,retryConfig);
    }

    /**
     * @see Retry#retry(IRetryableAction, IRetryAssertion, DelayStrategies, RetryConfig)
     */
    public static <T> T retry (IRetryableAction<T> retryableAction, IRetryAssertion retryAssertion,DelayStrategies delayStrategy, RetryConfig retryConfig){
        return retry(retryableAction,retryAssertion,delayStrategy,retryConfig);
    }

    /**
     * Retry the provided action for a specified amount of times using a delay determined by the provided {@link DelayStrategies} and {@link RetryConfig} when the exception matches the provided assertion.
     * @param retryableAction The action to retry
     * @param retryAssertion The assertion that determines if an exception is retried
     * @param doOnRetry The action to perform if the action is retried, executed before the delay. If left null no action is performed
     * @param delayStrategy The delay strategy used to calculate the delay ach retry
     * @param retryConfig The configuration used to determine the amount of retries and the delay
     * @return The result of the action
     * @param <T> The type of the result of the action
     * @throws RetryException every exception thrown by the action to retry are wrapped in a {@link RetryException}
     */

    public static <T> T retry(IRetryableAction<T> retryableAction, IRetryAssertion retryAssertion, IDoOnRetry doOnRetry, DelayStrategies delayStrategy, RetryConfig retryConfig) {
        int retriesLeft = retryConfig.getMaxRetries();
        int currentDelay;

        while (true) {
            try {
                return retryableAction.perform();
            } catch (Exception e) {
                if (retryAssertion.isRetryable(e) && retriesLeft > 0) {
                    retriesLeft--;
                    try {
                        //calculate current delay and
                        int retry = retryConfig.maxRetries - retriesLeft;
                        currentDelay = delayStrategy.calculateDelay(retry, retryConfig.getDelayMs());

                        //perform retry action if provided
                        if (doOnRetry != null) {
                            doOnRetry.perform(retryConfig.maxRetries - retriesLeft, e);
                        }

                        //busy wait for the indicated delay
                        Thread.sleep(currentDelay);
                    } catch (InterruptedException ie) {
                        //discard
                    }
                } else {
                    throw new RetryException(e);
                }
            }
        }
    }

    public interface IRetryableAction<T> {
        T perform() throws Exception;
    }

    public interface IDoOnRetry {
        void perform(int retryCount, Exception e);
    }

    public interface IRetryAssertion {
        boolean isRetryable(Exception e);
    }

    public static class RetryConfig {
        private final int maxRetries;
        private final int delayMs;

        public RetryConfig(int maxRetries, int delayMs) {
            this.maxRetries = maxRetries;
            this.delayMs = delayMs;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public int getDelayMs() {
            return delayMs;
        }
    }

    public static class RetryException extends RuntimeException{
        public RetryException(Throwable cause) {
            super(cause);
        }
    }

    public enum DelayStrategies {
        EXPONENTIAL((retry, delay) -> (int) Math.pow(delay, retry)),
        NORMAL((retry, delay) -> retry * delay);

        public final IDDelayCalc delayCalc;

        DelayStrategies(IDDelayCalc delayCalc) {
            this.delayCalc = delayCalc;
        }

        public int calculateDelay(int retry, int delay) {
            return delayCalc.calculate(retry, delay);
        }
    }

    public interface IDDelayCalc {
        int calculate(int retry, int delay);
    }

}
