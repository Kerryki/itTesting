# testgen-maven-plugin

Plugin Maven qui génère automatiquement des fichiers de tests d'intégration Spring Boot pour vos Use Cases Kotlin. Pour chaque classe `*UseCase` détectée dans vos sources, le plugin produit un fichier `*IntegrationTest.kt` prêt à compléter, utilisant `WebTestClient`.

---

## Prérequis

| Outil | Version minimale |
|-------|-----------------|
| Java | 17 |
| Maven | 3.9+ |
| Kotlin | 1.9+ |
| Spring Boot | 3.x (WebFlux ou WebMVC avec WebTestClient) |

---

## Installation

### 1. Compiler et installer le plugin localement

Clonez ce dépôt, puis installez-le dans votre dépôt Maven local :

```bash
git clone https://github.com/Kerryki/itTesting
cd test-scaffolding-generator

JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-18.0.2.1.jdk/Contents/Home mvn clean install -DskipTests
```

Le plugin est maintenant disponible sous les coordonnées :

```
com.example:testgen-maven-plugin:1.0.0-SNAPSHOT
```

### 2. Déclarer le plugin dans votre projet

Dans le `pom.xml` du projet cible, ajoutez le plugin dans la section `<build>` :

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.example</groupId>
      <artifactId>testgen-maven-plugin</artifactId>
      <version>1.0.0-SNAPSHOT</version>
      <executions>
        <execution>
          <goals>
            <goal>generate</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

Le goal `generate` est attaché par défaut à la phase `generate-test-sources` : il s'exécute automatiquement avant la compilation des tests.

---

## Configuration

Le plugin se configure via un fichier `.testgen.yml` à placer à la **racine du projet cible** (à côté du `pom.xml`).

Ce fichier est **optionnel** — sans lui, les valeurs par défaut s'appliquent.

```yaml
# Répertoire source à scanner (défaut : src/main/kotlin)
sourceDir: src/main/kotlin

# Répertoire de sortie des tests générés (défaut : src/test/kotlin)
outputDir: src/test/kotlin

# Filtrer par package de base (optionnel)
basePackage: com.example.myapp

# Inclure uniquement certains fichiers (globs, optionnel)
includes:
  - "**/*UseCase.kt"

# Exclure certains fichiers (globs, optionnel)
excludes:
  - "**/legacy/**"
  - "**/*DeprecatedUseCase.kt"

# Chemin vers un template Mustache personnalisé (optionnel)
templatePath: src/test/resources/my-custom-template.mustache
```

### Paramètres Maven

Ces paramètres peuvent aussi être passés en ligne de commande ou dans la configuration du plugin :

| Paramètre | Description | Défaut |
|-----------|-------------|--------|
| `skipGeneration` | Désactiver la génération | `false` |

Exemple pour sauter la génération ponctuellement :

```bash
mvn test -Dtestgen.skipGeneration=true
```

---

## Utilisation

### Génération automatique (recommandé)

Lors de `mvn test` ou `mvn verify`, le plugin s'exécute automatiquement à la phase `generate-test-sources` :

```bash
mvn test
```

### Génération manuelle

Pour déclencher uniquement la génération sans lancer les tests :

```bash
mvn testgen:generate
```

---

## Exemple

Le plugin détecte deux formes de Use Case : les classes avec une méthode `execute()` et les **interfaces fonctionnelles** (une seule méthode). L'exemple ci-dessous illustre le cas d'une interface.

### Source analysée — `CreateTaskUseCase.kt`

```kotlin
package com.example.taskmanager.domain.port.`in`

interface CreateTaskUseCase {
    fun createTask(command: CreateTaskCommand): Task
}

data class CreateTaskCommand(
    val title: String,
    val description: String?
)

data class Task(
    val id: String,
    val title: String,
    val done: Boolean
)
```

### Fichier généré — `CreateTaskUseCaseIntegrationTest.kt`

```kotlin
package com.example.taskmanager.domain.port.`in`

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.assertj.core.api.Assertions.assertThat
import org.springframework.test.web.reactive.server.expectBody
import com.example.taskmanager.domain.port.`in`.CreateTaskCommand
import com.example.taskmanager.domain.port.`in`.Task

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class CreateTaskUseCaseIntegrationTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun `should create resource and return 201`() {
        val request = CreateTaskCommand(
            title = "TODO",
            description = "TODO"
        )

        webTestClient
            .post()
            .uri("/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody<Task>()
            .value { body ->
                assertThat(body).isNotNull()
            }
    }
}
```

Les valeurs `"TODO"` sont des marqueurs intentionnels à remplacer par des données réelles avant d'exécuter le test.

---

## Conventions de détection

Le plugin reconnaît un Use Case si :

- Le nom de la classe se termine par `UseCase`
- La classe possède une méthode `execute()` **ou** c'est une interface fonctionnelle avec une seule méthode

### Inférence automatique

| Préfixe de classe | Méthode HTTP | Statut attendu |
|-------------------|-------------|----------------|
| `Create*` | `POST` | `201 Created` |
| `Get*` / `List*` | `GET` | `200 OK` |
| `Update*` | `PUT` | `200 OK` |
| `Delete*` | `DELETE` | `204 No Content` |
| Autre | `POST` | `200 OK` |

---

## Lancer les tests du plugin lui-même

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-18.0.2.1.jdk/Contents/Home mvn clean test
```

> **Note :** le plugin nécessite Java 17 ou 18 pour compiler. Java 26 casse le build en raison d'une incompatibilité de parsing de version dans Kotlin Maven Plugin 1.9.x. Java 21 LTS devrait fonctionner, mais n'a pas été testé.

---

## Limitations connues

- **Valeurs de champs** — les types de base (`String`, `Int`, `Boolean`, etc.) reçoivent une valeur `"TODO"` ou `0`/`false`. Les types personnalisés non résolus (DTO introuvable dans les sources scannées) reçoivent `TODO()` — le code compile, mais le test lève `NotImplementedError` à l'exécution. Remplacer ces valeurs par des instances réelles.
- **Inférence de l'URI** — l'URI est déduite du nom de la classe (`CreateTask` → `/tasks`). Si votre API suit une convention différente, corrigez l'appel `.uri(...)` dans le test généré.
- **Un seul Use Case par fichier** — si un fichier contient plusieurs classes `*UseCase`, seule la première détectée est générée.
- **Types de retour `List<T>`** — le test contient un commentaire `// TODO: assert expected list size` à la place d'une assertion `.hasSize(...)`. La taille attendue doit être précisée manuellement.
