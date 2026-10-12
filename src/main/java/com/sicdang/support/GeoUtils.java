package com.sicdang.support;

public final class GeoUtils {

	private static final double EARTH_RADIUS_METERS = 6_371_000;

	private GeoUtils() {
	}

	/** 두 좌표 사이의 거리(m). 하버사인 공식. */
	public static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
						* Math.sin(dLng / 2) * Math.sin(dLng / 2);
		return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	}

	/** 반경(m)을 위도 차이(도)로 환산. */
	public static double metersToLatitudeDegrees(double meters) {
		return Math.toDegrees(meters / EARTH_RADIUS_METERS);
	}

	/** 반경(m)을 해당 위도에서의 경도 차이(도)로 환산. */
	public static double metersToLongitudeDegrees(double meters, double atLatitude) {
		return metersToLatitudeDegrees(meters) / Math.cos(Math.toRadians(atLatitude));
	}

	/** 소수점 셋째 자리(약 100m)로 반올림. 캐시 적중률을 높이기 위해 사용. */
	public static double roundTo3(double value) {
		return Math.round(value * 1000) / 1000.0;
	}
}
