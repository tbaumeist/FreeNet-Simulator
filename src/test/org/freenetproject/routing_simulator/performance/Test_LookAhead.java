package test.org.freenetproject.routing_simulator.performance;

import junit.framework.Assert;

import org.junit.Test;

import test.org.freenetproject.routing_simulator.TestingBase;

public class Test_LookAhead extends TestingBase {

    @Test
    public void benchMarkLookAhead() throws Exception {

        /*
         * The original run times for this test were: 174042ms 178212ms 176805ms
         * 
         * 184842ms 168042ms 188729ms
         */

        /*
         * Dump most of the program output into the void.
         */
//        System.setErr(new PrintStream(new OutputStream() {
//            @Override
//            public void write(int b) {
//                // DO NOTHING
//            }
//        }));
//
//        String[] args = new String[] { "--link-ideal", "--degree-fixed", "10",
//                "--graph-size", "1000", "--route", "100000", "--route-hops",
//                "10000", "--route-fold-policy", "NONE", "--route-look-ahead",
//                "3" };
//        long startTime = new Date().getTime();
//        new RoutingSim().run(Arguments.parse(args));
//        System.out.println("run time: " + (new Date().getTime() - startTime)
//                + "ms");
//
//        startTime = new Date().getTime();
//        new RoutingSim().run(Arguments.parse(args));
//        System.out.println("run time: " + (new Date().getTime() - startTime)
//                + "ms");
//
//        startTime = new Date().getTime();
//        new RoutingSim().run(Arguments.parse(args));
//        System.out.println("run time: " + (new Date().getTime() - startTime)
//                + "ms");

        Assert.assertTrue(true);
    }
}
