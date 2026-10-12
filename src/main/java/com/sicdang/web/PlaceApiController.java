package com.sicdang.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import com.sicdang.service.PlaceSearchService;
import com.sicdang.web.dto.PlaceResponse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/places")
public class PlaceApiController {

	private final PlaceSearchService placeSearchService;

	public PlaceApiController(PlaceSearchService placeSearchService) {
		this.placeSearchService = placeSearchService;
	}

	@GetMapping
	public List<PlaceResponse> search(@RequestParam @NotBlank @Size(max = 100) String query) {
		return placeSearchService.search(query.strip());
	}

	@ExceptionHandler(RestClientException.class)
	public ProblemDetail handleKakaoFailure(RestClientException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
				"주소·장소 검색 서비스(카카오)를 일시적으로 사용할 수 없습니다.");
	}
}
