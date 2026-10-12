package com.sicdang.web.dto;

/**
 * 주소·장소 검색 결과 한 건.
 *
 * @param type ADDRESS: 주소 검색 결과, PLACE: 시설물(장소명) 검색 결과
 */
public record PlaceResponse(
		Type type,
		String name,
		String address,
		String category,
		double lat,
		double lng) {

	public enum Type {
		ADDRESS, PLACE
	}
}
