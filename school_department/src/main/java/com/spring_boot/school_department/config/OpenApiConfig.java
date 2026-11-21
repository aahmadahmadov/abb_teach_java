package com.spring_boot.school_department.config;

import com.spring_boot.school_department.dto.response.ErrorResponse;
import com.spring_boot.school_department.enums.ErrorCode;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI schoolOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("School API")
                        .version("v1")
                        .description("Student, Course, Department, Employee, Author, Book ve User/Passport API.\n\n"
                                + "Dil `?lang=` parametri ve ya `Accept-Language` header-i ile secilir (az / eng / rus).\n\n"
                                + errorCodeTable()));
    }

    @Bean
    public OpenApiCustomizer globalHeadersAndErrors() {
        return openApi -> {
            ModelConverters.getInstance().read(ErrorResponse.class).forEach(openApi.getComponents()::addSchemas);

            openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
                operation.addParametersItem(new HeaderParameter()
                        .name("Accept-Language")
                        .description("Mesajlarin dili")
                        .schema(new StringSchema()._enum(List.of("az", "eng", "rus"))));

                operation.addParametersItem(new HeaderParameter()
                        .name("X-Trace-Id")
                        .description("Gonderilmese server ozu yaradir (herf, reqem, tire, 8-64 simvol)")
                        .schema(new StringSchema().example("test-12345")));

                ApiResponses responses = operation.getResponses();
                responses.addApiResponse("400", errorResponse("Yanlis sorgu ve ya validasiya xetasi"));
                responses.addApiResponse("404", errorResponse("Tapilmadi"));
                responses.addApiResponse("409", errorResponse("Konflikt"));
                responses.addApiResponse("500", errorResponse("Daxili server xetasi"));
            }));
        };
    }

    private ApiResponse errorResponse(String description) {
        Schema<?> schema = new Schema<>().$ref("#/components/schemas/ErrorResponse");

        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json", new MediaType().schema(schema)));
    }

    private String errorCodeTable() {
        StringBuilder table = new StringBuilder("### Xeta kodlari\n\n| Kod | HTTP | Bundle acari |\n|---|---|---|\n");

        for (ErrorCode errorCode: ErrorCode.values()) {
            table.append("| ").append(errorCode.getCode())
                    .append(" | ").append(errorCode.getStatus().value())
                    .append(" | `").append(errorCode.getMessageKey()).append("` |\n");
        }

        return table.toString();
    }
}
