(function () {
	'use strict';

	// 위치 권한 거부 등으로 현재 위치를 못 얻었을 때 사용할 기본 위치 (서울시청)
	const DEFAULT_CENTER = { lat: 37.5665, lng: 126.9780 };

	const statusEl = document.getElementById('status');
	const listEl = document.getElementById('list');
	const radiusEl = document.getElementById('radius');
	const placeFormEl = document.getElementById('place-search');
	const placeQueryEl = document.getElementById('place-query');
	const placeResultsEl = document.getElementById('place-results');

	let map;
	let myLocationMarker;
	let selectedPlaceMarker;
	let radiusCircle;
	let infoOverlay;
	let cafeteriaMarkers = [];
	let placeResults = [];
	let activePlaceIndex = -1;
	let placeSearchTimer;
	let placeSearchSeq = 0;

	function setStatus(message, isError) {
		statusEl.textContent = message;
		statusEl.classList.toggle('error', !!isError);
	}

	function escapeHtml(value) {
		return String(value ?? '').replace(/[&<>"']/g, ch => ({
			'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
		})[ch]);
	}

	function formatDistance(meters) {
		return meters >= 1000 ? (meters / 1000).toFixed(1) + 'km' : meters + 'm';
	}

	function loadKakaoSdk(appKey) {
		return new Promise((resolve, reject) => {
			if (!appKey) {
				reject(new Error('카카오 JavaScript 키가 설정되지 않았습니다. (kakao.js-key)'));
				return;
			}
			const script = document.createElement('script');
			script.src = 'https://dapi.kakao.com/v2/maps/sdk.js?autoload=false&appkey=' + encodeURIComponent(appKey);
			script.onload = () => window.kakao.maps.load(resolve);
			script.onerror = () => reject(new Error('카카오 지도 SDK를 불러오지 못했습니다. 키와 등록된 도메인(http://localhost:9191)을 확인하세요.'));
			document.head.appendChild(script);
		});
	}

	function getCurrentPosition() {
		return new Promise(resolve => {
			if (!navigator.geolocation) {
				resolve(null);
				return;
			}
			navigator.geolocation.getCurrentPosition(
				pos => resolve({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
				() => resolve(null),
				{ enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
			);
		});
	}

	function toLatLng(p) {
		return new kakao.maps.LatLng(p.lat, p.lng);
	}

	function showMyLocation(position) {
		const latLng = toLatLng(position);
		if (!myLocationMarker) {
			myLocationMarker = new kakao.maps.CustomOverlay({
				position: latLng,
				content: '<div style="width:16px;height:16px;border-radius:50%;background:#2f6fed;border:3px solid #fff;box-shadow:0 0 0 2px rgba(47,111,237,.4)"></div>',
				zIndex: 3
			});
			myLocationMarker.setMap(map);
		} else {
			myLocationMarker.setPosition(latLng);
		}
	}

	function drawRadius(center, radius) {
		if (radiusCircle) radiusCircle.setMap(null);
		radiusCircle = new kakao.maps.Circle({
			center: toLatLng(center),
			radius: radius,
			strokeWeight: 1,
			strokeColor: '#2f6fed',
			strokeOpacity: 0.6,
			fillColor: '#2f6fed',
			fillOpacity: 0.06
		});
		radiusCircle.setMap(map);
	}

	function clearCafeterias() {
		cafeteriaMarkers.forEach(m => m.setMap(null));
		cafeteriaMarkers = [];
		listEl.innerHTML = '';
		closeInfo();
	}

	function closeInfo() {
		if (infoOverlay) infoOverlay.setMap(null);
	}

	// 카카오 기본 InfoWindow 는 내용 크기를 미리 재서 고정하므로 줄바꿈 시 글자가 넘친다.
	// 크기가 내용에 맞춰 늘어나도록 CustomOverlay 로 말풍선을 직접 그린다.
	function infoContent(item) {
		const el = document.createElement('div');
		el.className = 'info-window';
		el.innerHTML =
			'<button type="button" class="info-close" aria-label="닫기">×</button>' +
			'<strong>' + escapeHtml(item.name) + '</strong>' +
			'<div>' + escapeHtml(item.roadAddress || item.address) + '</div>' +
			(item.phone ? '<div>' + escapeHtml(item.phone) + '</div>' : '') +
			'<div class="info-meta">거리 ' + formatDistance(item.distance) +
			(item.placeUrl ? ' · <a href="' + escapeHtml(item.placeUrl) + '" target="_blank" rel="noopener">상세보기</a>' : '') +
			'</div>';
		el.querySelector('.info-close').addEventListener('click', closeInfo);
		return el;
	}

	function selectItem(index) {
		clearSelectedPlace();
		const marker = cafeteriaMarkers[index];
		const item = marker.__item;
		infoOverlay.setContent(infoContent(item));
		infoOverlay.setPosition(marker.getPosition());
		infoOverlay.setMap(map);
		map.panTo(marker.getPosition());

		listEl.querySelectorAll('li').forEach((li, i) => li.classList.toggle('active', i === index));
		const li = listEl.children[index];
		if (li) li.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
	}

	function renderCafeterias(items) {
		clearCafeterias();
		items.forEach((item, index) => {
			const marker = new kakao.maps.Marker({ position: toLatLng(item), title: item.name });
			marker.__item = item;
			marker.setMap(map);
			kakao.maps.event.addListener(marker, 'click', () => selectItem(index));
			cafeteriaMarkers.push(marker);

			const li = document.createElement('li');
			li.innerHTML =
				'<span class="distance">' + formatDistance(item.distance) + '</span>' +
				'<div class="name">' + escapeHtml(item.name) + '</div>' +
				'<div class="meta">' + escapeHtml(item.roadAddress || item.address) + '</div>' +
				(item.category ? '<div class="meta">' + escapeHtml(item.category) + '</div>' : '');
			li.addEventListener('click', () => selectItem(index));
			listEl.appendChild(li);
		});
	}

	async function search(center) {
		const radius = Number(radiusEl.value);
		drawRadius(center, radius);
		setStatus('구내식당을 찾는 중…');

		try {
			const params = new URLSearchParams({ lat: center.lat, lng: center.lng, radius: radius });
			const res = await fetch('/api/cafeterias?' + params);
			if (!res.ok) {
				const problem = await res.json().catch(() => ({}));
				throw new Error(problem.detail || ('HTTP ' + res.status));
			}
			const data = await res.json();
			renderCafeterias(data.items);

			let message = '반경 ' + formatDistance(radius) + ' 안에서 구내식당 ' + data.items.length + '곳을 찾았습니다.';
			if (data.source === 'DATABASE') {
				message += ' (카카오 검색을 사용할 수 없어 저장된 데이터로 표시합니다)';
			}
			setStatus(message, false);
		} catch (e) {
			clearCafeterias();
			setStatus('검색에 실패했습니다: ' + e.message, true);
		}
	}

	// ---- 주소·장소 검색 ----

	function closePlaceResults() {
		placeResultsEl.hidden = true;
		placeQueryEl.setAttribute('aria-expanded', 'false');
		activePlaceIndex = -1;
	}

	function renderPlaceResults(items, emptyMessage) {
		placeResults = items;
		activePlaceIndex = -1;
		placeResultsEl.innerHTML = '';

		if (items.length === 0) {
			const li = document.createElement('li');
			li.className = 'empty';
			li.textContent = emptyMessage || '검색 결과가 없습니다.';
			placeResultsEl.appendChild(li);
		}
		items.forEach((item, index) => {
			const li = document.createElement('li');
			li.setAttribute('role', 'option');
			const badge = item.type === 'ADDRESS'
				? '<span class="badge address">주소</span>'
				: '<span class="badge">장소</span>';
			li.innerHTML =
				'<div class="place-name">' + badge + escapeHtml(item.name) + '</div>' +
				'<div class="place-address">' + escapeHtml(item.address) +
				(item.category ? ' · ' + escapeHtml(item.category.split(' > ').pop()) : '') + '</div>';
			// blur 보다 먼저 처리되도록 mousedown 사용
			li.addEventListener('mousedown', e => {
				e.preventDefault();
				selectPlace(index);
			});
			placeResultsEl.appendChild(li);
		});

		placeResultsEl.hidden = false;
		placeQueryEl.setAttribute('aria-expanded', 'true');
	}

	function highlightPlace(index) {
		const items = placeResultsEl.querySelectorAll('li[role="option"]');
		if (items.length === 0) return;
		activePlaceIndex = (index + items.length) % items.length;
		items.forEach((li, i) => li.classList.toggle('active', i === activePlaceIndex));
		items[activePlaceIndex].scrollIntoView({ block: 'nearest' });
	}

	async function searchPlaces(query) {
		const seq = ++placeSearchSeq;
		try {
			const res = await fetch('/api/places?' + new URLSearchParams({ query: query }));
			if (!res.ok) {
				const problem = await res.json().catch(() => ({}));
				throw new Error(problem.detail || ('HTTP ' + res.status));
			}
			const items = await res.json();
			// 타이핑 중 늦게 도착한 이전 요청 결과는 무시
			if (seq === placeSearchSeq) renderPlaceResults(items);
		} catch (e) {
			if (seq === placeSearchSeq) renderPlaceResults([], '검색에 실패했습니다: ' + e.message);
		}
	}

	// 선택한 주소·장소 라벨은 식당 마커를 가리므로, 선택 직후 다른 조작을 하면 지운다.
	function clearSelectedPlace() {
		if (selectedPlaceMarker) {
			selectedPlaceMarker.setMap(null);
			selectedPlaceMarker = null;
		}
	}

	function showSelectedPlace(place) {
		clearSelectedPlace();
		const content = document.createElement('div');
		content.className = 'selected-place';
		content.textContent = '📍 ' + place.name;
		selectedPlaceMarker = new kakao.maps.CustomOverlay({
			position: toLatLng(place),
			content: content,
			yAnchor: 1,
			zIndex: 5
		});
		selectedPlaceMarker.setMap(map);
	}

	function selectPlace(index) {
		const place = placeResults[index];
		if (!place) return;
		closePlaceResults();
		placeQueryEl.value = place.name;
		placeQueryEl.blur();

		showSelectedPlace(place);
		map.setCenter(toLatLng(place));
		search({ lat: place.lat, lng: place.lng });
	}

	function initPlaceSearch() {
		placeQueryEl.addEventListener('input', () => {
			clearTimeout(placeSearchTimer);
			const query = placeQueryEl.value.trim();
			if (query.length < 2) {
				placeSearchSeq++;
				closePlaceResults();
				return;
			}
			placeSearchTimer = setTimeout(() => searchPlaces(query), 300);
		});

		placeQueryEl.addEventListener('keydown', e => {
			if (placeResultsEl.hidden) return;
			if (e.key === 'ArrowDown') {
				e.preventDefault();
				highlightPlace(activePlaceIndex + 1);
			} else if (e.key === 'ArrowUp') {
				e.preventDefault();
				highlightPlace(activePlaceIndex - 1);
			} else if (e.key === 'Escape') {
				closePlaceResults();
			} else if (e.key === 'Enter' && activePlaceIndex >= 0) {
				e.preventDefault();
				selectPlace(activePlaceIndex);
			}
		});

		placeFormEl.addEventListener('submit', e => {
			e.preventDefault();
			clearTimeout(placeSearchTimer);
			const query = placeQueryEl.value.trim();
			if (!query) return;
			// 결과가 이미 떠 있으면 첫 번째 결과로 바로 이동
			if (!placeResultsEl.hidden && placeResults.length > 0) {
				selectPlace(0);
			} else {
				searchPlaces(query);
			}
		});

		placeQueryEl.addEventListener('focus', () => {
			if (placeResults.length > 0 && placeQueryEl.value.trim().length >= 2) {
				placeResultsEl.hidden = false;
				placeQueryEl.setAttribute('aria-expanded', 'true');
			}
		});
		placeQueryEl.addEventListener('blur', closePlaceResults);
	}

	async function moveToMyLocation() {
		clearSelectedPlace();
		setStatus('위치를 확인하는 중…');
		const position = await getCurrentPosition();
		if (position) {
			showMyLocation(position);
		}
		const center = position || DEFAULT_CENTER;
		map.setCenter(toLatLng(center));
		await search(center);
		if (!position) {
			setStatus(statusEl.textContent + ' · 현재 위치를 가져올 수 없어 서울시청 기준으로 표시합니다.', false);
		}
	}

	async function init() {
		try {
			await loadKakaoSdk(window.SICDANG && window.SICDANG.kakaoJsKey);
		} catch (e) {
			setStatus(e.message, true);
			return;
		}

		map = new kakao.maps.Map(document.getElementById('map'), {
			center: toLatLng(DEFAULT_CENTER),
			level: 5
		});
		map.addControl(new kakao.maps.ZoomControl(), kakao.maps.ControlPosition.RIGHT);

		// 지도 영역 크기가 바뀌면(창 크기 변경, 모바일 회전 등) 카카오맵은 relayout 을 해줘야 중심이 어긋나지 않는다.
		const mapEl = document.getElementById('map');
		new ResizeObserver(() => {
			const center = map.getCenter();
			map.relayout();
			map.setCenter(center);
		}).observe(mapEl);
		infoOverlay = new kakao.maps.CustomOverlay({ yAnchor: 1, zIndex: 6, clickable: true });

		kakao.maps.event.addListener(map, 'click', clearSelectedPlace);
		kakao.maps.event.addListener(map, 'dragstart', clearSelectedPlace);

		initPlaceSearch();
		document.getElementById('btn-my-location').addEventListener('click', moveToMyLocation);
		document.getElementById('btn-research').addEventListener('click', () => {
			clearSelectedPlace();
			const c = map.getCenter();
			search({ lat: c.getLat(), lng: c.getLng() });
		});
		radiusEl.addEventListener('change', () => {
			clearSelectedPlace();
			const c = map.getCenter();
			search({ lat: c.getLat(), lng: c.getLng() });
		});

		await moveToMyLocation();
	}

	init();
})();
