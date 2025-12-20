package com.nfcaab.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = ["com.nfcaab.backend"])
open class NFCAABBackendApplication

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")
    runApplication<NFCAABBackendApplication>(*args)
}
