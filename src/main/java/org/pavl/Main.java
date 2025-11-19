package org.pavl;

import org.pavl.cli.CliHandler;

public class Main {
    public static void main(String[] args) {
        CliHandler cliHandler = new CliHandler();
        cliHandler.handle(args);
    }
}
