package com.spring_boot.school_department.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

public class LanguageLocaleResolver implements LocaleResolver {

    private static final Locale AZ = Locale.of("az");
    private static final Locale EN = Locale.of("en");
    private static final Locale RU = Locale.of("ru");

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String lang = request.getParameter("lang");

        if (lang == null || lang.isBlank()) {
            lang = request.getHeader("Accept-Language");
        }

        if (lang == null || lang.isBlank()) {
            return AZ;
        }

        // brauzer "en-US,en;q=0.9" kimi gonderir, ona gore yalniz birinci dili goturuk
        String code = lang.split("[,;-]")[0].trim().toLowerCase();

        return switch (code) {
            case "en", "eng" -> EN;
            case "ru", "rus" -> RU;
            default -> AZ;
        };
    }

    // lang her requestde param ve ya  header-den oxunur, ona gore hec yerde saxlamiriq
    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
    }
}
