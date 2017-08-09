package net.courtanet.devtools;

import static net.courtanet.devtools.MyBenchmark.EMutable.a;
import static net.courtanet.devtools.MyBenchmark.EMutable.b;
import static net.courtanet.devtools.MyBenchmark.EMutable.c;
import static net.courtanet.devtools.MyBenchmark.EMutable.d;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import net.courtanet.config.type.Mapper;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
public class MyBenchmark {

    enum E {
        _1,
        _2,
        _3,
        _4,;
    }

    enum EMutable {
        a("a"),
        b("b"),
        c("c"),
        d("d");

        private final String code;

        EMutable(String code) {
            this.code = code;
        }

        public E map(EMutable e) throws IllegalArgumentException {
            if (e == null) {
                return E._1;
                //                throw new IllegalArgumentException("null value");
            }
            switch (e) {
                case a:
                    return E._1;
                case b:
                    return E._2;
                case c:
                    return E._3;
                case d:
                    return E._4;
                default:
                    throw new IllegalArgumentException("not supported");
            }
        }

        public String getCode() {
            return code;
        }
    }

    Mapper<EMutable, E> mapping = Mapper.builder(EMutable.class, E.class)
            .map(a).to(E._1)
            .map(b).to(E._2)
            .map(c).to(E._3)
            .map(d).to(E._4)
            .withDefault(E._3)
            //            .mapNull().to(E._3)
            .build();

    @Param({ "null", "a" })
    public String arg;

    private EMutable getArg(String stringValue) {
        if ("null".equals(stringValue)) {
            return null;
        } else {
            return EMutable.valueOf(stringValue);
        }
    }

    @Benchmark
    public EMutable baselineZero() {
        return EMutable.a;
    }

    @Benchmark
    public EMutable baseline() {
        return getArg(arg);
    }

    @Benchmark
    public E switchTest() {
        return a.map(getArg(arg));
    }

    @Benchmark
    public E mapTest() {
        return mapping.map(getArg(arg));
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(MyBenchmark.class.getSimpleName())
                .warmupIterations(5)
                .measurementIterations(5)
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}