package com.gianlu.pluggableserver.core;

/**
 * @author Gianlu
 */
public class Main {
    public static void main(String[] args) {
        new Core(args.length == 0 ? null : args[0]).start();
    }
}
