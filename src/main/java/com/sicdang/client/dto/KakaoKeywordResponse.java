package com.sicdang.client.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 카카오 로컬 API 키워드 검색 응답.
 * https://developers.kakao.com/docs/latest/ko/local/dev-guide#search-by-keyword
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoKeywordResponse(Meta meta, List<Document> documents) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Meta(
			@JsonProperty("total_count") int totalCount,
			@JsonProperty("pageable_count") int pageableCount,
			@JsonProperty("is_end") boolean isEnd) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Document(
			String id,
			@JsonProperty("place_name") String placeName,
			@JsonProperty("category_name") String categoryName,
			@JsonProperty("category_group_code") String categoryGroupCode,
			String phone,
			@JsonProperty("address_name") String addressName,
			@JsonProperty("road_address_name") String roadAddressName,
			String x,
			String y,
			@JsonProperty("place_url") String placeUrl) {
	}
}
