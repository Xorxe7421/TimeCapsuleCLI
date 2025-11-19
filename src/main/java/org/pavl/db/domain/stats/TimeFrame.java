package org.pavl.db.domain.stats;

import java.time.LocalDate;

public record TimeFrame(LocalDate start, LocalDate end) {}