package com.agileo.AGILEO.Dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface EntProjection {
    String getBqcpce();
    BigDecimal getOrigine();
    LocalDate getDeldemdt();
    LocalDate getDelaccdt();
    String getRegl();
    String getDepo();
    String getTiers();
    String getProjet();
}