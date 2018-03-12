package it.teamdigitale.livy;

import java.util.*;

import org.apache.spark.api.java.*;
import org.apache.spark.api.java.function.*;

import com.cloudera.livy.*;

public class PiJob implements Job<Double>, Function<Integer, Integer>,
        Function2<Integer, Integer, Integer> {

    private final int samples;

    public PiJob(int samples) {
        this.samples = samples;
    }

    @Override
    public Double call(JobContext ctx) throws Exception {
        List<Integer> sampleList = new ArrayList<Integer>();
        for (int i = 0; i < samples; i++) {
            sampleList.add(i + 1);
        }

        return 4.0d * ctx.sc().parallelize(sampleList).map(this).reduce(this) / samples;
    }

    @Override
    public Integer call(Integer v1) {
        double x = Math.random();
        double y = Math.random();
        return (x*x + y*y < 1) ? 1 : 0;
    }

    @Override
    public Integer call(Integer v1, Integer v2) {
        return v1 + v2;
    }

}
