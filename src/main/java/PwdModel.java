/** Represents one password-vault row: name, login, password, category and resource. */
public class PwdModel {
    private String name = "";
    private String login = "";
    private String password = "";
    private String category = "Default Category";
    private String resource = "";

    public PwdModel() {
    }

    public PwdModel(String name, String login, String password, String category, String resource) {
        this.name = safe(name);
        this.login = safe(login);
        this.password = safe(password);
        this.category = safe(category).isBlank() ? "Default Category" : category;
        this.resource = safe(resource);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = safe(name);
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = safe(login);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = safe(password);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = safe(category).isBlank() ? "Default Category" : category;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = safe(resource);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
