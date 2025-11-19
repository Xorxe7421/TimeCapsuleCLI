package org.pavl.cli.domain;

import java.util.List;

public record CliInput(
        Command command,
        List<String> positionalArguments,
        List<String> flags
) {}