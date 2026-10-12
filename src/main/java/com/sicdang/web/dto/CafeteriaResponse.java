package com.sicdang.web.dto;

import com.sicdang.domain.Cafeteria;

public record CafeteriaResponse(
		String id,
		String name,
		String category,
		String phone,
		String address,
		String roadAddress,
		double lat,
		double lng,
		int distance,
		String placeUrl) {

	public static CafeteriaResponse of(Cafeteria c, double distanceMeters) {
		return new CafeteriaResponse(
				c.getKakaoPlaceId(),
				c.getName(),
				c.getCategoryName(),
				c.getPhone(),
				c.getAddressName(),
				c.getRoadAddressName(),
				c.getLatitude(),
				c.getLongitude(),
				(int) Math.round(distanceMeters),
				c.getPlaceUrl());
	}
}
