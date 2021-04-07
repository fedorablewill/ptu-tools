package com.willstep.ptutools

import com.fasterxml.jackson.databind.ObjectMapper
import com.willstep.ptutools.dataaccess.dto.Pokemon
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.servlet.support.RequestContext
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import org.thymeleaf.spring5.context.webmvc.SpringWebMvcThymeleafRequestContext
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.templateresolver.ITemplateResolver
import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Controller
class PtuToolsTemplateController {
    @Autowired
    lateinit var htmlTemplateEngine: TemplateEngine

    @Autowired
    lateinit var servletContext: ServletContext

    @Autowired
    lateinit var environment: Environment

    @GetMapping("/index")
    fun index(model: Model): String? {
        return "index"
    }
    @GetMapping("/damageRoller")
    fun damageBaseRoller(model: Model): String? {
        return "damageRoller"
    }
    @GetMapping("/generator")
    fun generator(model: Model): String? {
        model.addAttribute("requestBody", JsonAsString())
        return "generator"
    }
    @GetMapping("/pokemon")
    fun uploadPokemon(model: Model): String? {
        return "uploadCharacter"
    }
    @PostMapping("/pokemonFragment")
    fun pokemonFragment(@RequestBody pokemon: Pokemon, request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<String>? {
        val variables = mapOf<String, Any>("pokemon" to pokemon)
        val context = Context()
        context.setVariables(variables)
        val requestContext = RequestContext(request, response, servletContext, variables)
        val thymeleafRequestContext = SpringWebMvcThymeleafRequestContext(requestContext, request)
        context.setVariable("thymeleafRequestContext", thymeleafRequestContext)

        return ResponseEntity.ok(htmlTemplateEngine.process("fragment-characterPokemon", context))
    }
    @PostMapping("/pokemon")
    fun pokemon(@ModelAttribute requestBody: JsonAsString, request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<String>? {
        val variables = mapOf<String, Any>(
            "pokemon" to ObjectMapper().readValue(requestBody.myData, Pokemon::class.java),
            "jsValue" to environment.getProperty("js.version")!!
        )
        val context = Context()
        context.setVariables(variables)
        val requestContext = RequestContext(request, response, servletContext, variables)
        val thymeleafRequestContext = SpringWebMvcThymeleafRequestContext(requestContext, request)
        context.setVariable("thymeleafRequestContext", thymeleafRequestContext)

        return ResponseEntity.ok(htmlTemplateEngine.process("pokemon", context))
    }
}

data class JsonAsString(var myData: String = "")



@Bean
fun htmlTemplateEngine(): TemplateEngine {
    val templateEngine = SpringTemplateEngine()
    templateEngine.addTemplateResolver(htmlTemplateResolver())
    return templateEngine
}

private fun htmlTemplateResolver(): ITemplateResolver {
    val templateResolver = ClassLoaderTemplateResolver()
    templateResolver.resolvablePatterns = setOf("html/*")
    templateResolver.prefix = "/templates/"
    templateResolver.suffix = ".html"
    templateResolver.templateMode = TemplateMode.HTML
    templateResolver.characterEncoding = "utf-8"
    templateResolver.isCacheable = false
    return templateResolver
}