package esvar.ua.diploma.dto;

public class GenerationResponse {

    private String content;
    private String filePath;

    public GenerationResponse(String content, String filePath) {
        this.content = content;
        this.filePath = filePath;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
