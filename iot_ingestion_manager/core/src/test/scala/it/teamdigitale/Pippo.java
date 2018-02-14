package it.teamdigitale;

import com.cloudera.livy.*;
import it.teamdigitale.livy.PiJob;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.sun.tools.doclint.Entity.pi;
import static com.sun.tools.internal.ws.wsdl.parser.Util.fail;

public class Pippo {
    /* Since it's hard to test a streaming context, test that a
   * streaming context has been created. Also checks that improper
   * sequence of streaming context calls (i.e create, stop, retrieve)
   * result in a failure.
   */
    private static class SparkStreamingJob implements Job<Boolean> {
        @Override
        public Boolean call(JobContext jc) throws Exception {
            try {
                jc.streamingctx();
                fail("Access before creation: Should throw IllegalStateException");
            } catch (IllegalStateException ex) {
                // Expected.
            }
            try {
                jc.stopStreamingCtx();
                fail("Stop before creation: Should throw IllegalStateException");
            } catch (IllegalStateException ex) {
                // Expected.
            }
            try {
                jc.createStreamingContext(1000L);
                JavaStreamingContext streamingContext = jc.streamingctx();
                jc.stopStreamingCtx();
                jc.streamingctx();
                fail("");
            } catch (IllegalStateException ex) {
                // Expected.
            }

            jc.createStreamingContext(1000L);
            JavaStreamingContext streamingContext = jc.streamingctx();
            jc.stopStreamingCtx();
            return streamingContext != null;
        }
    }

    public static void main(String[] args) throws IOException, URISyntaxException, TimeoutException {

        LivyClient client = new LivyClientBuilder()
                .setURI(new URI("http://livy.default.svc.cluster.local:8998"))
                .setConf("", "" )
                .build();

        try {
            //System.err.printf("Uploading %s to the Spark context...\n", piJar)
            //client.uploadJar(new File(piJar)).get();

            //int samples = 2;

            //System.err.printf("Running PiJob with %d samples...\n", samples);
            //double pi = 0;
            try {
                //pi = client.submit(new PiJob(samples)).get();
                JobHandle<Boolean> handle = client.submit(new SparkStreamingJob());
                Boolean streamingContextCreated = handle.get(30000, TimeUnit.SECONDS);
                System.out.println( streamingContextCreated);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            System.out.println("Pi is roughly: " + pi);
        } finally {
            client.stop(true);
        }
    }
}
