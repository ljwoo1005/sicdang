package com.sicdang.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CafeteriaRepository extends JpaRepository<Cafeteria, String> {

	List<Cafeteria> findByLatitudeBetweenAndLongitudeBetween(
			double minLatitude, double maxLatitude, double minLongitude, double maxLongitude);
}
