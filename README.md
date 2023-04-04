# retry-util
Small Java utility for retries

## Usage

The utility can be used by supplying:
- The action to retry in the form of a supplier.
- An assertion that checks if the exception thrown requires a retry or is propogated immediately.
- A retry strategy (Either exponential or normal).
- A retry config that specifies.
  - The number of retries.
  - The base delay between retries which is modified by the retry strategy.
  
When an exception is thrown by the action to be performed the exception is checked against the assertion provided, if it matches the action is retried after the thread is blocked for a specified period of time modified by the retry strategy. If the assertion does not match the exception is rethrown immediately. 

All exceptions thrown by the retry utility are wrapped into a RetryException which extends Runtime Exception. 
 
 ## Example
  ```
          try {
            double result = Retry.retry(
                    //action to perform
                    () -> 10 / Math.random() - .5d,
                    //assertion to determine if a retry is required
                    e -> e instanceof ArithmeticException,
                    //action to perform before retrying
                    (retryCount, e) -> System.out.println("Warning! Retry" + retryCount + ". " + e),
                    //delay strategy
                    Retry.DelayStrategies.EXPONENTIAL,
                    //configuration
                    new Retry.RetryConfig(3, 10));
        } catch (Retry.RetryException e) {
            //unwrap to get the original exception
            System.err.println(e.getCause());
        }


```
