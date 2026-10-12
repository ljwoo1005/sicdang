package com.sicdang.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카카오 검색으로 수집한 구내식당. 카카오 장소 ID를 PK로 사용해 중복 없이 누적한다.
 * 카카오 API 장애 시 이 테이블에서 주변 식당을 조회한다.
 */
@Entity
@Table(name = "cafeteria", indexes = @Index(name = "idx_cafeteria_lat_lng", columnList = "latitude, longitude"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cafeteria {

	@Id
	@Column(name = "kakao_place_id", length = 32)
	private String kakaoPlaceId;

	@Column(nullable = false)
	private String name;

	private String categoryName;

	@Column(length = 32)
	private String phone;

	private String addressName;

	private String roadAddressName;

	@Column(nullable = false)
	private double latitude;

	@Column(nullable = false)
	private double longitude;

	private String placeUrl;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public Cafeteria(String kakaoPlaceId) {
		this.kakaoPlaceId = kakaoPlaceId;
	}

	public void updateFromKakao(String name, String categoryName, String phone, String addressName,
			String roadAddressName, double latitude, double longitude, String placeUrl) {
		this.name = name;
		this.categoryName = categoryName;
		this.phone = phone;
		this.addressName = addressName;
		this.roadAddressName = roadAddressName;
		this.latitude = latitude;
		this.longitude = longitude;
		this.placeUrl = placeUrl;
	}

	@PrePersist
	void onCreate() {
		createdAt = updatedAt = Instant.now();
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}
}
