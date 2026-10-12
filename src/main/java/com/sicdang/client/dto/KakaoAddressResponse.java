package com.sicdang.client.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 카카오 로컬 API 주소 검색 응답.
 * https://developers.kakao.com/docs/latest/ko/local/dev-guide#address-coord
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoAddressResponse(List<Document> documents) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Document(
			@JsonProperty("address_name") String addressName,
			String x,
			String y,
			@JsonProperty("road_address") RoadAddress roadAddress) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record RoadAddress(
			@JsonProperty("address_name") String addressName,
			@JsonProperty("building_name") String buildingName) {
	}
}
