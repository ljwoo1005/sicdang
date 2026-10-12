package com.sicdang.client;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.sicdang.client.dto.KakaoAddressResponse;
import com.sicdang.client.dto.KakaoKeywordResponse;
import com.sicdang.client.dto.KakaoKeywordResponse.Document;

@Component
public class KakaoLocalClient {

	// 카카오 키워드 검색은 페이지당 최대 15건, 최대 45건(3페이지)까지만 조회된다.
	private static final int PAGE_SIZE = 15;
	private static final int MAX_PAGE = 3;

	private final RestClient kakaoRestClient;

	public KakaoLocalClient(RestClient kakaoRestClient) {
		this.kakaoRestClient = kakaoRestClient;
	}

	/**
	 * 중심 좌표 기준 반경 내에서 키워드로 장소를 검색한다. 거리순으로 최대 45건.
	 * 같은 키워드·좌표·반경 조합은 캐시되므로 호출 측에서 좌표를 반올림해 넘기는 것이 좋다.
	 */
	@Cacheable(cacheNames = "kakaoKeywordSearch", key = "#query + ':' + #lat + ':' + #lng + ':' + #radius")
	public List<Document> searchKeyword(String query, double lat, double lng, int radius) {
		List<Document> results = new ArrayList<>();
		for (int page = 1; page <= MAX_PAGE; page++) {
			int currentPage = page;
			KakaoKeywordResponse response = kakaoRestClient.get()
					.uri(uri -> uri.path("/v2/local/search/keyword.json")
							.queryParam("query", query)
							.queryParam("x", lng)
							.queryParam("y", lat)
							.queryParam("radius", radius)
							.queryParam("sort", "distance")
							.queryParam("size", PAGE_SIZE)
							.queryParam("page", currentPage)
							.build())
					.retrieve()
					.body(KakaoKeywordResponse.class);

			if (response == null || response.documents() == null) {
				break;
			}
			results.addAll(response.documents());
			if (response.meta() == null || response.meta().isEnd()) {
				break;
			}
		}
		return results;
	}

	/** 주소(지번/도로명)로 좌표를 검색한다. */
	public List<KakaoAddressResponse.Document> searchAddress(String query, int size) {
		KakaoAddressResponse response = kakaoRestClient.get()
				.uri(uri -> uri.path("/v2/local/search/address.json")
						.queryParam("query", query)
						.queryParam("size", size)
						.build())
				.retrieve()
				.body(KakaoAddressResponse.class);
		return response == null || response.documents() == null ? List.of() : response.documents();
	}

	/** 위치 제한 없이 키워드(시설물·장소명)로 검색한다. 정확도순. */
	public List<Document> searchPlaces(String query, int size) {
		KakaoKeywordResponse response = kakaoRestClient.get()
				.uri(uri -> uri.path("/v2/local/search/keyword.json")
						.queryParam("query", query)
						.queryParam("size", size)
						.build())
				.retrieve()
				.body(KakaoKeywordResponse.class);
		return response == null || response.documents() == null ? List.of() : response.documents();
	}
}
