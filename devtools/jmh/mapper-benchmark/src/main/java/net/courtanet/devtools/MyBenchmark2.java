/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package net.courtanet.devtools;


import static net.courtanet.devtools.MyBenchmark2.EMutable.*;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
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
@Measurement(iterations = 30)
public class MyBenchmark2 {

    enum E {
        _1,
        _2,
        _3,
        _4,
        _5,
        _6,
        _7,
        _8,
        _9,
        _10,
        _11,
        _12,
        _13,
        _14,
        _15,
        _16,
        _17,
        _18,
        _19,
        _20,;
    }

    enum EMutable {
        a("a"),
        b("b"),
        c("c"),
        d("d"),
        e("e"),
        f("f"),
        g("g"),
        h("h"),
        i("i"),
        j("j"),
        k("k"),
        l("l"),
        m("m"),
        n("n"),
        o("o"),
        p("p"),
        q("q"),
        r("r"),
        s("s"),
        t("t"),
        ;

        private final String code;

        EMutable(String code) {
            this.code = code;
        }

        public E map(EMutable e) throws IllegalArgumentException {
            if (e == null) {
                return E._1;
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
                case e:
                    return E._5;
                case f:
                    return E._6;
                case g:
                    return E._7;
                case h:
                    return E._8;
                case i:
                    return E._9;
                case j:
                    return E._10;
                case k:
                    return E._11;
                case l:
                    return E._12;
                case m:
                    return E._13;
                case n:
                    return E._14;
                case o:
                    return E._15;
                case p:
                    return E._16;
                case q:
                    return E._17;
                case r:
                    return E._18;
                case s:
                    return E._19;
                case t:
                    return E._20;
                default:
                    throw new IllegalArgumentException("not supported");
            }
        }

        public String getCode() {
            return code;
        }
    }

    @State(Scope.Benchmark)
    public static class MyState {
        public EMutable a = EMutable.a;
        public EMutable t = EMutable.t;
        public EMutable n = null;
    }

    static Mapper<EMutable, E> mapping = Mapper.builder(EMutable.class, E.class)
                .map(a).to(E._1)
                .map(b).to(E._2)
                .map(c).to(E._3)
                .map(d).to(E._4)
                .map(e).to(E._5)
                .map(f).to(E._6)
                .map(g).to(E._7)
                .map(h).to(E._8)
                .map(i).to(E._9)
                .map(j).to(E._10)
                .map(k).to(E._11)
                .map(l).to(E._12)
                .map(m).to(E._13)
                .map(n).to(E._14)
                .map(o).to(E._15)
                .map(p).to(E._16)
                .map(q).to(E._17)
                .map(r).to(E._18)
                .map(s).to(E._19)
                .map(t).to(E._20)
                .mapNull().to(E._1)
                .build();

    @Benchmark
    public void baseline(Blackhole blackhole, MyState myState) {
        blackhole.consume(myState.a);
        blackhole.consume(myState.t);
        blackhole.consume(myState.n);
    }

    @Benchmark
    public void switchTest(Blackhole blackhole, MyState myState) {
        blackhole.consume(a.map(myState.a));
        blackhole.consume(a.map(myState.t));
        blackhole.consume(a.map(myState.n));
    }

    @Benchmark
    public void mapTest(Blackhole blackhole, MyState myState) {
        blackhole.consume(mapping.map(myState.a));
        blackhole.consume(mapping.map(myState.t));
        blackhole.consume(mapping.map(myState.n));
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(MyBenchmark2.class.getSimpleName())
                .warmupIterations(5)
                .measurementIterations(5)
                .forks(3)
                .build();

        new Runner(opt).run();
    }
}