package com.willstep.ptutools

import com.willstep.ptutools.dataaccess.service.UploadDataTool
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Controller
class PtuToolsTemplateController {
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
        return "generator"
    }
}