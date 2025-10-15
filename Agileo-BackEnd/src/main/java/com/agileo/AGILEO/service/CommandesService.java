package com.agileo.AGILEO.service;

import com.agileo.AGILEO.Dtos.response.ArticleDisponibleDTO;
import com.agileo.AGILEO.entity.primary.ArticleReception;
import com.agileo.AGILEO.entity.primary.LigneReception;
import com.agileo.AGILEO.entity.primary.Reception;
import com.agileo.AGILEO.exception.BadRequestException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public  interface CommandesService {
    public List<ArticleDisponibleDTO> getArticlesDisponibles(Long commandeId);

}
