package com.sicdang.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class GeoUtilsTest {

	@Test
	void 서울시청에서_서울역까지_약_1_5km() {
		double distance = GeoUtils.distanceMeters(37.5665, 126.9780, 37.5547, 126.9707);
		assertThat(distance).isCloseTo(1_470, within(100.0));
	}

	@Test
	void 같은_좌표는_거리_0() {
		assertThat(GeoUtils.distanceMeters(37.5, 127.0, 37.5, 127.0)).isZero();
	}

	@Test
	void 반경을_위경도로_환산하면_다시_같은_거리() {
		double lat = 37.5;
		double dLat = GeoUtils.metersToLatitudeDegrees(1000);
		double dLng = GeoUtils.metersToLongitudeDegrees(1000, lat);
		assertThat(GeoUtils.distanceMeters(lat, 127.0, lat + dLat, 127.0)).isCloseTo(1000, within(1.0));
		assertThat(GeoUtils.distanceMeters(lat, 127.0, lat, 127.0 + dLng)).isCloseTo(1000, within(1.0));
	}
}
