package esvar.ua.diploma.service;

import esvar.ua.diploma.dto.GenerationRequest;
import esvar.ua.diploma.dto.GenerationResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DiplomaGeneratorService {

    private final OpenAiClient openAiClient;

    public DiplomaGeneratorService(OpenAiClient openAiClient) {
        this.openAiClient = openAiClient;
    }

    public GenerationResponse generate(GenerationRequest request) {
        String systemPrompt = buildSystemPrompt();
        Map<String, String> sections = buildSections();
        Map<String, String> generated = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : sections.entrySet()) {
            String sectionTitle = entry.getKey();
            String userPrompt = formatUserPrompt(sectionTitle, entry.getValue(), request);
            String content = openAiClient.generateText(systemPrompt, userPrompt).block();
            generated.put(sectionTitle, normalize(content));
        }

        StringBuilder fullText = new StringBuilder();
        generated.forEach((title, text) -> {
            fullText.append(title).append("\n\n");
            fullText.append(text.trim()).append("\n\n");
        });

        String path = persistToFile(fullText.toString());
        return new GenerationResponse(fullText.toString(), path);
    }

    private String buildSystemPrompt() {
        return "Ти — асистент, що пише академічні тексти українською мовою без списків. " +
                "Усі відповіді мають бути розгорнутими абзацами, зберігати логіку дипломної роботи, " +
                "дотримуватися вказаного обсягу і включати таблиці лише там, де це зазначено.";
    }

    private Map<String, String> buildSections() {
        Map<String, String> parts = new LinkedHashMap<>();
        parts.put("ВСТУП", "Сформуй вступ до дипломної роботи з актуальністю, метою, завданнями та коротким описом методів дослідження.");

        parts.put("РОЗДІЛ 1. Теоретичні засади", "Три абзаци. Поясни контекст теми, сучасний стан досліджень, базові поняття. Без таблиць.");
        parts.put("1.1. Огляд літератури", "Три абзаци з критичним оглядом основних джерел та їх внеском у тему.");
        parts.put("1.2. Аналітична база", "Окресли ключові концепти та підходи. Додай одну таблицю у форматі Markdown з заголовком і трьома рядками даних.");
        parts.put("1.3. Висновки до розділу", "Підсумуй сильні та слабкі сторони наявних досліджень, сформулюй прогалини.");

        parts.put("РОЗДІЛ 2. Методологія", "Опиши загальну логіку методології дослідження трьома абзацами.");
        parts.put("2.1. Методичні підходи", "Розкрий обрані методи та їх обґрунтування.");
        parts.put("2.2. Інструменти та дані", "Опиши дані, інструменти та процедури. Додай одну таблицю у форматі Markdown з заголовком і трьома рядками.");
        parts.put("2.3. Організація дослідження", "Опиши етапи реалізації методології, забезпечення якості даних.");

        parts.put("РОЗДІЛ 3. Результати та впровадження", "Три абзаци про ключові результати та їх інтерпретацію.");
        parts.put("3.1. Основні результати", "Сформулюй результати та їх значення для теми.");
        parts.put("3.2. Практична апробація", "Опиши застосування результатів. Додай одну таблицю у форматі Markdown з заголовком і трьома рядками.");
        parts.put("3.3. Перспективи розвитку", "Опиши подальші напрямки дослідження і можливі поліпшення.");

        parts.put("ВИСНОВКИ", "Підсумуй головні висновки, наукову новизну та практичну цінність роботи.");
        return parts;
    }

    private String formatUserPrompt(String sectionTitle, String task, GenerationRequest request) {
        return "Тема дипломної роботи: " + request.getTopic() + "\n" +
                "Спеціальність: " + request.getSpecialty() + "\n" +
                "Бажаний обсяг (сторінки): " + request.getPages() + "\n" +
                "Напиши розділ: '" + sectionTitle + "'. " + task + " " +
                "Стиль — український академічний, лише зв'язні абзаци, без списків. Якщо потрібна таблиця, використовуй Markdown.";
    }

    private String normalize(String content) {
        return StringUtils.hasText(content) ? content.trim() : "";
    }

    private String persistToFile(String text) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
        Path folder = Path.of("output");
        try {
            Files.createDirectories(folder);
            Path file = folder.resolve("diploma_" + timestamp + ".txt");
            Files.writeString(file, text);
            return file.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Не вдалося зберегти файл", e);
        }
    }
}
