package com.sicdang.web.dto;

import java.util.List;

/**
 * @param source KAKAO: 카카오 실시간 검색 결과, DATABASE: 카카오 호출 실패로 저장된 데이터에서 조회
 */
public record NearbyCafeteriasResponse(
		Source source,
		double lat,
		double lng,
		int radius,
		List<CafeteriaResponse> items) {

	public enum Source {
		KAKAO, DATABASE
	}
}
