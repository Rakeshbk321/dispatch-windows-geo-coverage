package com.retail.dispatch.repository;

import com.retail.dispatch.domain.Commune;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommuneRepository extends JpaRepository<Commune, String> {

	@EntityGraph(attributePaths = "zone")
	Optional<Commune> findWithZoneByCode(String code);

	@EntityGraph(attributePaths = "zone")
	List<Commune> findAllWithZoneByOrderByCode();

}
