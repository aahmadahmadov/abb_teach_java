package com.tms.order_calculation_application;

import org.springframework.boot.SpringApplication;

public class TestOrderCalculationApplication {

    public static void main(String[] args) {
        SpringApplication.from(OrderCalculationApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
