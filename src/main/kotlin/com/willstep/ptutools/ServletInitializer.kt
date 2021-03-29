package com.willstep.ptutools

import org.springframework.boot.web.servlet.support.SpringBootServletInitializer
import org.springframework.boot.builder.SpringApplicationBuilder
import com.willstep.ptutools.PtuToolsApplication

class ServletInitializer : SpringBootServletInitializer() {
    override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {
        println("ServletInitializer called.")
        return application.sources(PtuToolsApplication::class.java)
    }
}