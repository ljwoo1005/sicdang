package com.sicdang.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kakao")
public record KakaoProperties(
		String restKey,
		String jsKey,
		String baseUrl,
		List<String> keywords,
		List<String> categoryIncludes) {

	public KakaoProperties {
		if (baseUrl == null || baseUrl.isBlank()) {
			baseUrl = "https://dapi.kakao.com";
		}
		if (keywords == null || keywords.isEmpty()) {
			keywords = List.of("구내식당");
		}
		if (categoryIncludes == null || categoryIncludes.isEmpty()) {
			categoryIncludes = List.of("구내식당");
		}
	}

	/** 카카오 카테고리명(예: "음식점 > 구내식당")에 허용 목록 중 하나라도 포함되면 구내식당으로 본다. */
	public boolean isCafeteriaCategory(String categoryName) {
		return categoryName != null && categoryIncludes.stream().anyMatch(categoryName::contains);
	}

	public boolean hasRestKey() {
		return restKey != null && !restKey.isBlank();
	}
}
