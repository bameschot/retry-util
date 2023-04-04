package com.ameschot.retryutil;

public class Retry {

    public static <T> T retry(IRetryableAction<T> retryableAction, IRetryAssertion retryAssertion, IDoOnRetry iDoOnRetry, DelayStrategies delayStrategy, RetryConfig retryConfig) throws Exception {
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
                        if (iDoOnRetry != null) {
                            iDoOnRetry.perform(retryConfig.maxRetries - retriesLeft, e);
                        }

                        //busy wait for the indicated delay
                        Thread.sleep(currentDelay);
                    } catch (InterruptedException ie) {
                        //discard
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    public interface IRetryableAction<T> {
        T perform() throws Exception;
    }

    public interface IDoOnRetry {
        void perform(int retryCount, Exception e) throws Exception;
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
