package com.sicdang.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sicdang.client.KakaoLocalClient;
import com.sicdang.client.dto.KakaoAddressResponse;
import com.sicdang.client.dto.KakaoKeywordResponse;
import com.sicdang.web.dto.PlaceResponse;
import com.sicdang.web.dto.PlaceResponse.Type;

@Service
public class PlaceSearchService {

	private static final int ADDRESS_SIZE = 5;
	private static final int PLACE_SIZE = 10;

	private final KakaoLocalClient kakaoLocalClient;

	public PlaceSearchService(KakaoLocalClient kakaoLocalClient) {
		this.kakaoLocalClient = kakaoLocalClient;
	}

	/** 주소 검색 결과를 먼저, 이어서 시설물 검색 결과를 돌려준다. */
	public List<PlaceResponse> search(String query) {
		List<PlaceResponse> results = new ArrayList<>();
		for (KakaoAddressResponse.Document doc : kakaoLocalClient.searchAddress(query, ADDRESS_SIZE)) {
			results.add(fromAddress(doc));
		}
		for (KakaoKeywordResponse.Document doc : kakaoLocalClient.searchPlaces(query, PLACE_SIZE)) {
			results.add(fromPlace(doc));
		}
		return results;
	}

	private PlaceResponse fromAddress(KakaoAddressResponse.Document doc) {
		KakaoAddressResponse.RoadAddress road = doc.roadAddress();
		String roadAddress = road != null ? road.addressName() : null;
		String buildingName = road != null && road.buildingName() != null && !road.buildingName().isBlank()
				? road.buildingName() : null;
		return new PlaceResponse(
				Type.ADDRESS,
				buildingName != null ? buildingName : doc.addressName(),
				roadAddress != null && !roadAddress.isBlank() ? roadAddress : doc.addressName(),
				null,
				Double.parseDouble(doc.y()),
				Double.parseDouble(doc.x()));
	}

	private PlaceResponse fromPlace(KakaoKeywordResponse.Document doc) {
		return new PlaceResponse(
				Type.PLACE,
				doc.placeName(),
				doc.roadAddressName() != null && !doc.roadAddressName().isBlank()
						? doc.roadAddressName() : doc.addressName(),
				doc.categoryName(),
				Double.parseDouble(doc.y()),
				Double.parseDouble(doc.x()));
	}
}
