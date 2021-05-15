package com.willstep.ptutools

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import com.willstep.ptutools.dataaccess.dto.Ability
import com.willstep.ptutools.dataaccess.dto.Move
import com.willstep.ptutools.dataaccess.dto.Pokemon
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.RequestContext
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import org.thymeleaf.spring5.context.webmvc.SpringWebMvcThymeleafRequestContext
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.templateresolver.ITemplateResolver
import java.io.ByteArrayOutputStream
import java.io.OutputStream
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
        return "index"
    }
    @PostMapping("/pokemonFragment")
    fun pokemonFragment(@RequestBody pokemon: Pokemon, request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<String>? {
        val variables = mapOf<String, Any>("pokemon" to pokemon)
        val context = Context()
        context.setVariables(variables)
        val requestContext = RequestContext(request, response, servletContext, variables)
        val thymeleafRequestContext = SpringWebMvcThymeleafRequestContext(requestContext, request)
        context.setVariable("thymeleafRequestContext", thymeleafRequestContext)

        return ResponseEntity.ok(htmlTemplateEngine.process("fragments/characterPokemon", context))
    }
    @GetMapping("/pokemon/new")
    fun newPokemon(request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<String>? {
        val variables = mapOf<String, Any>("pokemon" to Pokemon())
        val context = Context()
        context.setVariables(variables)
        val requestContext = RequestContext(request, response, servletContext, variables)
        val thymeleafRequestContext = SpringWebMvcThymeleafRequestContext(requestContext, request)
        context.setVariable("thymeleafRequestContext", thymeleafRequestContext)

        return ResponseEntity.ok(htmlTemplateEngine.process("fragments/characterPokemon", context))
    }
    @PostMapping("/pokemon")
    fun pokemon(@ModelAttribute requestBody: JsonAsString, request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<String>? {
        val variables = mapOf<String, Any>(
            "pokemon" to ObjectMapper().readValue(requestBody.myData, Pokemon::class.java),
            "jsVersion" to environment.getProperty("js.version")!!,
            "cssVersion" to environment.getProperty("css.version")!!
        )
        val context = Context()
        context.setVariables(variables)
        val requestContext = RequestContext(request, response, servletContext, variables)
        val thymeleafRequestContext = SpringWebMvcThymeleafRequestContext(requestContext, request)
        context.setVariable("thymeleafRequestContext", thymeleafRequestContext)

        return ResponseEntity.ok(htmlTemplateEngine.process("pokemon", context))
    }
    @GetMapping("/pokemon/drive/{fileId}")
    fun pokemonFromGoogleDrive(@PathVariable fileId: String, request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<String>? {
        val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()

        val token = request.cookies?.find { it.name == "authToken" }?.value

        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val outputStream: OutputStream = ByteArrayOutputStream()
        val driveService: Drive = Drive.Builder(HTTP_TRANSPORT, JacksonFactory.getDefaultInstance(), HttpCredentialsAdapter(
            GoogleCredentials.create(
                AccessToken(token, null)
            ))
        )
            .setApplicationName("PTU Exodus")
            .build()
        driveService.files().get(fileId)
            .executeMediaAndDownloadTo(outputStream)

        val pokemon = ObjectMapper().readValue(outputStream.toString(), Pokemon::class.java);
        pokemon.googleDriveFileId = fileId

        val variables = mapOf<String, Any>(
            "pokemon" to pokemon,
            "jsVersion" to environment.getProperty("js.version")!!,
            "cssVersion" to environment.getProperty("css.version")!!
        )
        val context = Context()
        context.setVariables(variables)
        val requestContext = RequestContext(request, response, servletContext, variables)
        val thymeleafRequestContext = SpringWebMvcThymeleafRequestContext(requestContext, request)
        context.setVariable("thymeleafRequestContext", thymeleafRequestContext)

        return ResponseEntity.ok(htmlTemplateEngine.process("pokemon", context))
    }
    @GetMapping("/pokemon/move")
    fun getMoveFragment(@RequestParam move: Move?, @RequestParam index: Int): ResponseEntity<String> {
        val context = Context()
        context.setVariable("move", move ?: Move())
        context.setVariable("index", index)
        val fragmentsSelectors: Set<String> = setOf("move")

        return ResponseEntity.ok(htmlTemplateEngine.process("fragments/characterFormFragments", fragmentsSelectors, context))
    }
    @GetMapping("/pokemon/ability")
    fun getAbilityFragment(@RequestParam ability: Ability?, @RequestParam index: Int): ResponseEntity<String> {
        val context = Context()
        context.setVariable("ability", ability ?: Ability())
        context.setVariable("index", index)
        val fragmentsSelectors: Set<String> = setOf("ability")

        return ResponseEntity.ok(htmlTemplateEngine.process("fragments/characterFormFragments", fragmentsSelectors, context))
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