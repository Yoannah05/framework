# Configuration du Framework

## Prérequis
- Java 11+ avec Jakarta Servlet API.
- Un serveur compatible (ex: Tomcat).

## Configuration

### 1. Définir le Package des Contrôleurs

Dans votre fichier `web.xml`, spécifiez le package contenant vos contrôleurs en utilisant le paramètre `controllers`. Par exemple :

```xml
<init-param>
    <param-name>controllers</param-name>
    <param-value>com.test.controllers</param-value> 
</init-param>
