package com.sicdang.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sicdang.service.CafeteriaService;
import com.sicdang.web.dto.NearbyCafeteriasResponse;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/cafeterias")
public class CafeteriaApiController {

	private final CafeteriaService cafeteriaService;

	public CafeteriaApiController(CafeteriaService cafeteriaService) {
		this.cafeteriaService = cafeteriaService;
	}

	@GetMapping
	public NearbyCafeteriasResponse nearby(
			@RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
			@RequestParam @DecimalMin("-180") @DecimalMax("180") double lng,
			@RequestParam(defaultValue = "1000") @Min(100) @Max(5000) int radius) {
		return cafeteriaService.findNearby(lat, lng, radius);
	}
}
