package esvar.ua.diploma.controller;

import esvar.ua.diploma.dto.GenerationRequest;
import esvar.ua.diploma.dto.GenerationResponse;
import esvar.ua.diploma.service.DiplomaGeneratorService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final DiplomaGeneratorService generatorService;

    public ApiController(DiplomaGeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public GenerationResponse generate(@Valid @RequestBody GenerationRequest request) {
        return generatorService.generate(request);
    }
}
