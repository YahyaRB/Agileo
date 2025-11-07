package com.agileo.AGILEO.service;

import com.agileo.AGILEO.entity.divalto.SocPrefNo;
import com.agileo.AGILEO.repository.divalto.SocPrefNoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class SocPrefNoService {

    @Autowired
    private SocPrefNoRepository socPrefNoRepository;

    @Transactional
    public BigDecimal getNextPinoForBL() {
        // 1. Récupérer l'entité SocPrefNo avec verrou pessimiste
        SocPrefNo socPrefNo = socPrefNoRepository
                .findByPicodAndTicodAndDosForReception()
                .orElseThrow(() -> new RuntimeException(
                        "Aucun numéro de préfixe trouvé pour PICOD=3, TICOD=F, DOS=1"
                ));

        // 2. Extraire la valeur actuelle de PINO
        BigDecimal currentPino = socPrefNo.getPino();

        // 3. Calculer le prochain PINO (PINO+1)
        BigDecimal nextPino = currentPino.add(BigDecimal.ONE);

        // 4. Incrémenter PINO dans la base de données
        int rowsUpdated = socPrefNoRepository.incrementPinoReception();

        if (rowsUpdated == 0) {
            throw new RuntimeException("Échec de l'incrémentation du PINO");
        }

        // 5. Retourner PINO+1 pour l'utiliser dans ENT
        return nextPino;
    }
    @Transactional
    public BigDecimal getNextPinoForCONS() {
        // 1. Récupérer l'entité SocPrefNo avec verrou pessimiste
        SocPrefNo socPrefNo = socPrefNoRepository
                .findByPicodAndTicodAndDosForConsommation()
                .orElseThrow(() -> new RuntimeException(
                        "Aucun numéro de préfixe trouvé pour PICOD=3, TICOD=F, DOS=1"
                ));

        // 2. Extraire la valeur actuelle de PINO
        BigDecimal currentPino = socPrefNo.getPino();
System.out.println("currentPinoxxxxxxxxxxxxxxxx :" +currentPino);
        // 3. Calculer le prochain PINO (PINO+1)
        BigDecimal nextPino = currentPino.add(BigDecimal.ONE);

        // 4. Incrémenter PINO dans la base de données
        int rowsUpdated = socPrefNoRepository.incrementPinoConsommation();

        if (rowsUpdated == 0) {
            throw new RuntimeException("Échec de l'incrémentation du PINO");
        }

        return nextPino;
    }
}