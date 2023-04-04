package com.ameschot.retryutil;

import org.junit.Assert;
import org.junit.Test;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class RetryTest {

    @Test
    public void testNoRetryOnSuccess() throws Exception {
        Retry.RetryConfig retryConfig = new Retry.RetryConfig(3, 5);

        AtomicInteger i = new AtomicInteger(0);

        long start = System.currentTimeMillis();
        String result = Retry.retry(() -> functionSuccess("This many paws: ", i), Objects::nonNull, (i1, e) -> System.out.println("warning " + i1+"/"+ e), Retry.DelayStrategies.NORMAL, retryConfig);
        long end = System.currentTimeMillis();

        Assert.assertEquals("This many paws: 1", result);
        Assert.assertTrue(end - start < 300);
    }

    @Test
    public void testNoRetryUnMatchedException() {
        Retry.RetryConfig retryConfig = new Retry.RetryConfig(3, 5);
        AtomicInteger i = new AtomicInteger(0);

        Exception ex = Assert.assertThrows(ArithmeticException.class, () -> Retry.retry(() -> functionError(new ArithmeticException(),i), e -> e instanceof IndexOutOfBoundsException, null, Retry.DelayStrategies.NORMAL, retryConfig));

        Assert.assertEquals(1,i.get());
        Assert.assertTrue(ex instanceof ArithmeticException);
    }

    @Test
    public void testRetryUntilEndMatchedException() {
        Retry.RetryConfig retryConfig = new Retry.RetryConfig(3, 5);
        AtomicInteger i = new AtomicInteger(0);

        Exception ex = Assert.assertThrows(ArithmeticException.class, () -> Retry.retry(() -> functionError(new ArithmeticException(),i), e -> e instanceof ArithmeticException, (i1, e) -> System.out.println("warning " + i1+"/"+ e), Retry.DelayStrategies.NORMAL, retryConfig));

        Assert.assertEquals(4,i.get());
        Assert.assertTrue(ex instanceof ArithmeticException);
    }

    @Test
    public void testRetryUntilSuccessException() throws Exception {
        Retry.RetryConfig retryConfig = new Retry.RetryConfig(3, 5);

        long start = System.currentTimeMillis();
        AtomicInteger i = new AtomicInteger(0);
        String result = Retry.retry(() -> functionSuccessError("This many paws: ", i, 2, new ArithmeticException()), e -> e instanceof ArithmeticException, (i1, e) -> System.out.println("warning " + i1+"/"+ e), Retry.DelayStrategies.NORMAL, retryConfig);
        long end = System.currentTimeMillis();

        Assert.assertEquals("This many paws: 2", result);
    }

    @Test
    public void testNormalDelay(){
        Assert.assertEquals(300, Retry.DelayStrategies.NORMAL.calculateDelay(1,300));
        Assert.assertEquals(600, Retry.DelayStrategies.NORMAL.calculateDelay(2,300));
        Assert.assertEquals(900, Retry.DelayStrategies.NORMAL.calculateDelay(3,300));
        Assert.assertEquals(1200, Retry.DelayStrategies.NORMAL.calculateDelay(4,300));
    }

    @Test
    public void testExponentialDelay(){
        Assert.assertEquals(5, Retry.DelayStrategies.EXPONENTIAL.calculateDelay(1,5));
        Assert.assertEquals(25, Retry.DelayStrategies.EXPONENTIAL.calculateDelay(2,5));
        Assert.assertEquals(125, Retry.DelayStrategies.EXPONENTIAL.calculateDelay(3,5));
        Assert.assertEquals(625, Retry.DelayStrategies.EXPONENTIAL.calculateDelay(4,5));
    }

    public String functionSuccess(String s, AtomicInteger i) {
        return s + i.incrementAndGet();
    }

    public String functionError(Exception e,AtomicInteger i) throws Exception {
        i.incrementAndGet();
        throw e;
    }

    public String functionSuccessError(String s, AtomicInteger i, int ie, Exception e) throws Exception {
        if (i.get() < ie) {
            i.incrementAndGet();
            throw e;
        }
        return s + i.get();
    }
}
