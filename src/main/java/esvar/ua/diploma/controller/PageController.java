package esvar.ua.diploma.controller;

import esvar.ua.diploma.dto.GenerationRequest;
import esvar.ua.diploma.dto.GenerationResponse;
import esvar.ua.diploma.service.DiplomaGeneratorService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PageController {

    private final DiplomaGeneratorService generatorService;

    public PageController(DiplomaGeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("request", new GenerationRequest());
        return "index";
    }

    @PostMapping("/")
    public String generate(@Valid @ModelAttribute("request") GenerationRequest request,
                           BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            return "index";
        }
        GenerationResponse response = generatorService.generate(request);
        model.addAttribute("response", response);
        return "result";
    }
}
