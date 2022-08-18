package com.willstep.ptutools

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import com.willstep.ptutools.core.PTUCoreInfoService
import com.willstep.ptutools.dataaccess.dto.*
import com.willstep.ptutools.dataaccess.service.DataUpdater
import com.willstep.ptutools.dataaccess.service.FirestoreService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
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
    @GetMapping("/policy")
    fun policy(model: Model): String? {
        return "policy"
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
        DataUpdater().checkPokemonForUpdates(pokemon)

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
        val pokemon = ObjectMapper().readValue(requestBody.myData, Pokemon::class.java)

        DataUpdater().checkPokemonForUpdates(pokemon)

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
    @GetMapping("/pokemon/drive/{fileId}")
    fun pokemonFromGoogleDrive(@PathVariable fileId: String, request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<String>? {
        val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
        val token = request.cookies?.find { it.name == "authToken" }?.value

        val context = Context()
        val variables = mutableMapOf<String, Any>(
            "jsVersion" to environment.getProperty("js.version")!!,
            "cssVersion" to environment.getProperty("css.version")!!
        )

        if (token == null) {
            context.setVariables(variables)
            val requestContext = RequestContext(request, response, servletContext, variables)
            val thymeleafRequestContext = SpringWebMvcThymeleafRequestContext(requestContext, request)
            context.setVariable("thymeleafRequestContext", thymeleafRequestContext)
            return ResponseEntity.ok(htmlTemplateEngine.process("login", context))
        }

        val outputStream: OutputStream = ByteArrayOutputStream()
        val driveService: Drive = Drive.Builder(HTTP_TRANSPORT, JacksonFactory.getDefaultInstance(), HttpCredentialsAdapter(
            GoogleCredentials.create(
                AccessToken(token, null)
            ))
        )
            .setApplicationName("PokéSheets")
            .build()

        try {
            driveService.files().get(fileId)
                .executeMediaAndDownloadTo(outputStream)
        } catch (e: IllegalStateException) {
            context.setVariables(variables)
            val requestContext = RequestContext(request, response, servletContext, variables)
            val thymeleafRequestContext = SpringWebMvcThymeleafRequestContext(requestContext, request)
            context.setVariable("thymeleafRequestContext", thymeleafRequestContext)
            return ResponseEntity.ok(htmlTemplateEngine.process("login", context))
        }

        val pokemon = ObjectMapper().readValue(outputStream.toString(), Pokemon::class.java);
        pokemon.googleDriveFileId = fileId

        DataUpdater().checkPokemonForUpdates(pokemon)

        variables["pokemon"] = pokemon
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
    @GetMapping("/pokemon/pokeedge")
    fun getPokeEdgeFragment(@RequestParam pokeEdge: PokeEdge?, @RequestParam index: Int): ResponseEntity<String> {
        val context = Context()
        context.setVariable("edge", pokeEdge ?: PokeEdge())
        context.setVariable("index", index)
        val fragmentsSelectors: Set<String> = setOf("pokeedge")

        return ResponseEntity.ok(htmlTemplateEngine.process("fragments/characterFormFragments", fragmentsSelectors, context))
    }
    @GetMapping("/pokemon/note")
    fun getNoteFragment(@RequestParam note: Note?, @RequestParam index: Int): ResponseEntity<String> {
        val context = Context()
        context.setVariable("note", note ?: Note())
        context.setVariable("index", index)
        val fragmentsSelectors: Set<String> = setOf("note")

        return ResponseEntity.ok(htmlTemplateEngine.process("fragments/characterFormFragments", fragmentsSelectors, context))
    }

    @GetMapping("/pokemon/move/search")
    fun getMoveSearchResults(@RequestParam term: String, @RequestParam stabTypes: List<String>): ResponseEntity<String> {
        // Capitalize each word
        val query = term.split(" ").joinToString(" ") { it.capitalize() }.trimEnd();

        // Do search
        val results = FirestoreService().getCollection("moves")
            .whereGreaterThanOrEqualTo("name", query)
            .whereLessThanOrEqualTo("name", query + '\uf8ff')
            .orderBy("name")
            .limit(10).get().get().map { it.toObject(Move::class.java) }

        // Check STAB
        results.forEach { PTUCoreInfoService().checkMoveStab(it, stabTypes) }

        // Render results
        val context = Context()
        context.setVariable("moves", results)
        val fragmentsSelectors: Set<String> = setOf("moveSearchResults")

        return ResponseEntity.ok(htmlTemplateEngine.process("fragments/characterFormFragments", fragmentsSelectors, context))
    }

    @GetMapping("/pokemon/moveset")
    fun getMoveLearnset(@RequestParam moveLearnset: PokedexEntry.MoveLearnset, @RequestParam stabTypes: List<String>): ResponseEntity<String> {

        val levelUpMoveMap = FirestoreService().getDocuments("moves", "name", moveLearnset.getLevelUpMoveNames(), true)
            .associate { it.get("name") to it.toObject(Move::class.java) }
        val levelUpMoves = moveLearnset.levelUpMoves.map { Pair(levelUpMoveMap[it.moveName], it.learnedLevel) }.sortedBy { it.second }
        val machineMoves = FirestoreService().getDocuments("moves", "name", moveLearnset.machineMoves, true)
            .map { it.toObject(Move::class.java) }
        val eggMoves = FirestoreService().getDocuments("moves", "name", moveLearnset.eggMoves, true)
            .map { it.toObject(Move::class.java) }
        val tutorMoves = FirestoreService().getDocuments("moves", "name", moveLearnset.tutorMoves, true)
            .map { it.toObject(Move::class.java) }

        levelUpMoves.forEach { PTUCoreInfoService().checkMoveStab(it.first!!, stabTypes) }
        machineMoves.forEach { PTUCoreInfoService().checkMoveStab(it, stabTypes) }
        eggMoves.forEach { PTUCoreInfoService().checkMoveStab(it, stabTypes) }
        tutorMoves.forEach { PTUCoreInfoService().checkMoveStab(it, stabTypes) }

        val context = Context()
        context.setVariable("levelUpMoves", levelUpMoves)
        context.setVariable("machineMoves", machineMoves)
        context.setVariable("eggMoves", eggMoves)
        context.setVariable("tutorMoves", tutorMoves)
        val fragmentsSelectors: Set<String> = setOf("moveLearnset")

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