package com.retail.dispatch.repository;

import com.retail.dispatch.domain.Zone;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZoneRepository extends JpaRepository<Zone, Long> {

	List<Zone> findAllByOrderByCode();

}
