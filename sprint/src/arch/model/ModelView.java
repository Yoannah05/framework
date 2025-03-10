package arch.model;

import java.util.HashMap;

public class ModelView {
    private String url;
    private HashMap<String, FieldData> formData;
    private HashMap<String, Object> data;

    // Classe interne pour gérer la valeur et les erreurs associées à un champ
    public static class FieldData {
        private Object value;
        private String error;

        public FieldData(Object value) {
            this.value = value;
            this.error = null;
        }

        public FieldData(Object value, String error) {
            this.value = value;
            this.error = error;
        }

        // Getter pour la valeur
        public Object getValue() {
            return value;
        }

        // Setter pour la valeur
        public void setValue(Object value) {
            this.value = value;
        }

        // Getter pour l'erreur
        public String getError() {
            return error;
        }

        // Setter pour l'erreur
        public void setError(String error) {
            this.error = error;
        }

        @Override
        public String toString() {
            return "FieldData{" +
                    "value=" + value +
                    ", error='" + error + '\'' +
                    '}';
        }
    }

    // Constructeur par défaut
    public ModelView() {
        this.data = new HashMap<>();
        this.formData = new HashMap<>();
    }

    // Constructeur avec paramètre url
    public ModelView(String url) {
        this.url = url;
        this.data = new HashMap<>();
        this.formData = new HashMap<>();
    }

    // Constructeur avec paramètres url et data
    public ModelView(String url, HashMap<String, Object> data) {
        this.url = url;
        this.data = data;
        this.formData = new HashMap<>();
    }

    // Getter pour l'URL
    public String getUrl() {
        return url;
    }

    // Setter pour l'URL
    public void setUrl(String url) {
        this.url = url;
    }

    // Getter pour le data
    public HashMap<String, Object> getData() {
        return data;
    }

    // Setter pour le data
    public void setData(HashMap<String, Object> data) {
        this.data = data;
    }

    // Getter pour le formData
    public HashMap<String, FieldData> getFormData() {
        return formData;
    }

    // Setter pour le formData
    public void setFormData(HashMap<String, FieldData> formData) {
        this.formData = formData;
    }

    // Méthode pour ajouter un élément au HashMap data
    public void addObject(String key, Object value) {
        this.data.put(key, value);
    }

    // Méthode pour récupérer un élément du HashMap data
    public Object getData(String key) {
        return this.data.get(key);
    }

    // Méthode pour ajouter un champ au formData avec sa valeur
    public void addFormData(String key, Object value) {
        this.formData.put(key, new FieldData(value));
    }

    // Méthode pour ajouter un champ au formData avec sa valeur et une erreur
    public void addFormData(String key, Object value, String error) {
        this.formData.put(key, new FieldData(value, error));
    }

    // Méthode pour récupérer un champ du formData
    public FieldData getFormData(String key) {
        return this.formData.get(key);
    }

    @Override
    public String toString() {
        return "ModelView{" +
                "url='" + url + '\'' +
                ", data=" + data +
                ", formData=" + formData +
                '}';
    }
}
