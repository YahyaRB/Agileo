package com.agileo.AGILEO.repository.divalto;

import com.agileo.AGILEO.entity.divalto.MJoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MJointRepository extends JpaRepository<MJoint, Long> {

    @Query("SELECT COALESCE(MAX(m.joint), 0) FROM MJoint m WHERE m.applic = 'DAV'")
    Long findMaxJointByApplic();
}