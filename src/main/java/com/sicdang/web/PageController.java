package com.sicdang.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.sicdang.config.KakaoProperties;

@Controller
public class PageController {

	private final KakaoProperties kakaoProperties;

	public PageController(KakaoProperties kakaoProperties) {
		this.kakaoProperties = kakaoProperties;
	}

	@GetMapping("/")
	public String index(Model model) {
		model.addAttribute("kakaoJsKey", kakaoProperties.jsKey());
		return "index";
	}
}
