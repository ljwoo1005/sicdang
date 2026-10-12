package com.sicdang.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

import com.sicdang.client.KakaoLocalClient;
import com.sicdang.client.dto.KakaoKeywordResponse.Document;
import com.sicdang.config.KakaoProperties;
import com.sicdang.domain.Cafeteria;
import com.sicdang.domain.CafeteriaRepository;
import com.sicdang.support.GeoUtils;
import com.sicdang.web.dto.CafeteriaResponse;
import com.sicdang.web.dto.NearbyCafeteriasResponse;
import com.sicdang.web.dto.NearbyCafeteriasResponse.Source;

@Service
public class CafeteriaService {

	private static final Logger log = LoggerFactory.getLogger(CafeteriaService.class);

	/** 카카오 카테고리 그룹 코드: 음식점 */
	private static final String CATEGORY_RESTAURANT = "FD6";
	/** 좌표 반올림(약 100m)으로 생기는 오차를 덮기 위한 검색 반경 여유분(m) */
	private static final int ROUNDING_PADDING_METERS = 150;
	private static final int KAKAO_MAX_RADIUS = 20_000;

	private final KakaoLocalClient kakaoLocalClient;
	private final KakaoProperties kakaoProperties;
	private final CafeteriaRepository cafeteriaRepository;
	private final TransactionTemplate transactionTemplate;

	public CafeteriaService(KakaoLocalClient kakaoLocalClient, KakaoProperties kakaoProperties,
			CafeteriaRepository cafeteriaRepository, TransactionTemplate transactionTemplate) {
		this.kakaoLocalClient = kakaoLocalClient;
		this.kakaoProperties = kakaoProperties;
		this.cafeteriaRepository = cafeteriaRepository;
		this.transactionTemplate = transactionTemplate;
	}

	public NearbyCafeteriasResponse findNearby(double lat, double lng, int radius) {
		if (kakaoProperties.hasRestKey()) {
			try {
				List<Cafeteria> found = searchKakao(lat, lng, radius);
				return toResponse(Source.KAKAO, lat, lng, radius, found);
			}
			catch (RestClientException e) {
				log.warn("카카오 로컬 API 호출 실패, 저장된 데이터로 대체합니다: {}", e.getMessage());
			}
		}
		else {
			log.warn("kakao.rest-key 가 설정되지 않아 저장된 데이터로만 조회합니다.");
		}
		return toResponse(Source.DATABASE, lat, lng, radius, findInDatabase(lat, lng, radius));
	}

	private List<Cafeteria> searchKakao(double lat, double lng, int radius) {
		double roundedLat = GeoUtils.roundTo3(lat);
		double roundedLng = GeoUtils.roundTo3(lng);
		int searchRadius = Math.min(radius + ROUNDING_PADDING_METERS, KAKAO_MAX_RADIUS);

		// 여러 키워드 결과를 장소 ID 기준으로 합친다.
		Map<String, Document> merged = new LinkedHashMap<>();
		for (String keyword : kakaoProperties.keywords()) {
			for (Document doc : kakaoLocalClient.searchKeyword(keyword, roundedLat, roundedLng, searchRadius)) {
				if (CATEGORY_RESTAURANT.equals(doc.categoryGroupCode())
						&& kakaoProperties.isCafeteriaCategory(doc.categoryName())) {
					merged.putIfAbsent(doc.id(), doc);
				}
			}
		}
		log.debug("카카오 검색 결과 {}건 (center={},{}, radius={})", merged.size(), roundedLat, roundedLng, searchRadius);
		return upsert(merged.values());
	}

	private List<Cafeteria> upsert(Collection<Document> documents) {
		if (documents.isEmpty()) {
			return List.of();
		}
		return transactionTemplate.execute(status -> {
			Map<String, Cafeteria> existing = cafeteriaRepository
					.findAllById(documents.stream().map(Document::id).toList())
					.stream()
					.collect(Collectors.toMap(Cafeteria::getKakaoPlaceId, Function.identity()));

			List<Cafeteria> cafeterias = documents.stream().map(doc -> {
				Cafeteria c = existing.getOrDefault(doc.id(), new Cafeteria(doc.id()));
				c.updateFromKakao(doc.placeName(), doc.categoryName(), doc.phone(), doc.addressName(),
						doc.roadAddressName(), Double.parseDouble(doc.y()), Double.parseDouble(doc.x()),
						doc.placeUrl());
				return c;
			}).toList();
			return cafeteriaRepository.saveAll(cafeterias);
		});
	}

	private List<Cafeteria> findInDatabase(double lat, double lng, int radius) {
		double dLat = GeoUtils.metersToLatitudeDegrees(radius);
		double dLng = GeoUtils.metersToLongitudeDegrees(radius, lat);
		return cafeteriaRepository.findByLatitudeBetweenAndLongitudeBetween(
				lat - dLat, lat + dLat, lng - dLng, lng + dLng);
	}

	private NearbyCafeteriasResponse toResponse(Source source, double lat, double lng, int radius,
			List<Cafeteria> cafeterias) {
		List<CafeteriaResponse> items = cafeterias.stream()
				.filter(c -> kakaoProperties.isCafeteriaCategory(c.getCategoryName()))
				.map(c -> CafeteriaResponse.of(c,
						GeoUtils.distanceMeters(lat, lng, c.getLatitude(), c.getLongitude())))
				.filter(r -> r.distance() <= radius)
				.sorted(Comparator.comparingInt(CafeteriaResponse::distance))
				.toList();
		return new NearbyCafeteriasResponse(source, lat, lng, radius, items);
	}
}
