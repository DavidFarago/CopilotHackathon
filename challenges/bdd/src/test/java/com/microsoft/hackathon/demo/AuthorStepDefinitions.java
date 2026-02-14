package com.microsoft.hackathon.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.hackathon.demo.model.Author;
import com.microsoft.hackathon.demo.repository.AuthorRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.OptionalLong;

public class AuthorStepDefinitions {

    private static final String BASE_URL = "http://localhost:8080/authors";

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ResponseEntity<String> lastResponse;
    private Long currentAuthorId;

    @Given("the author repository is empty")
    public void theAuthorRepositoryIsEmpty() {
        authorRepository.deleteAll();
        currentAuthorId = null;
        lastResponse = null;
    }

    @Given("a nonexistent author id")
    public void aNonexistentAuthorId() {
        OptionalLong maxId = authorRepository.findAll()
                .stream()
                .mapToLong(Author::getId)
                .max();
        currentAuthorId = maxId.isPresent() ? maxId.getAsLong() + 1 : 1L;
    }

    @Given("an author exists with name {string}")
    public void anAuthorExistsWithName(String name) {
        Author saved = authorRepository.save(new Author(name));
        currentAuthorId = saved.getId();
    }

    @When("I create an author with name {string}")
    public void iCreateAnAuthorWithName(String name) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("{\"name\":\"" + name + "\"}", headers);
        lastResponse = restTemplate.postForEntity(BASE_URL, request, String.class);
        if (lastResponse.getStatusCode().is2xxSuccessful() && lastResponse.getBody() != null) {
            currentAuthorId = extractId(lastResponse.getBody());
        } else {
            currentAuthorId = null;
        }
    }

    @When("I request all authors")
    public void iRequestAllAuthors() {
        lastResponse = restTemplate.getForEntity(BASE_URL, String.class);
    }

    @When("I request that author by id")
    public void iRequestThatAuthorById() {
        Assert.assertNotNull("Author ID must be set before requesting by ID", currentAuthorId);
        lastResponse = restTemplate.getForEntity(BASE_URL + "/" + currentAuthorId, String.class);
    }

    @When("I update that author name to {string}")
    public void iUpdateThatAuthorNameTo(String name) {
        Assert.assertNotNull("Author ID must be set before updating", currentAuthorId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("{\"name\":\"" + name + "\"}", headers);
        lastResponse = restTemplate.exchange(BASE_URL + "/" + currentAuthorId, HttpMethod.PUT, request, String.class);
    }

    @When("I delete that author")
    public void iDeleteThatAuthor() {
        Assert.assertNotNull("Author ID must be set before deleting", currentAuthorId);
        lastResponse = restTemplate.exchange(BASE_URL + "/" + currentAuthorId, HttpMethod.DELETE, HttpEntity.EMPTY, String.class);
    }

    @Then("the response status should be {int}")
    public void theResponseStatusShouldBe(Integer statusCode) {
        Assert.assertNotNull(lastResponse);
        Assert.assertEquals((int) statusCode, lastResponse.getStatusCode().value());
    }

    @And("the response should contain an author id")
    public void theResponseShouldContainAnAuthorId() {
        Long id = extractId(lastResponse.getBody());
        Assert.assertNotNull(id);
        Assert.assertTrue(id > 0);
    }

    @And("the response should contain author name {string}")
    public void theResponseShouldContainAuthorName(String name) {
        Assert.assertEquals(name, extractName(lastResponse.getBody()));
    }

    @And("the response should contain {int} authors")
    public void theResponseShouldContainAuthors(Integer count) {
        Assert.assertEquals((int) count, extractArraySize(lastResponse.getBody()));
    }

    @And("the response should include an author named {string}")
    public void theResponseShouldIncludeAnAuthorNamed(String name) {
        Assert.assertTrue(responseArrayContainsName(lastResponse.getBody(), name));
    }

    @And("requesting that author by id should return {int}")
    public void requestingThatAuthorByIdShouldReturn(Integer statusCode) {
        Assert.assertNotNull("Author ID must be set before requesting by ID", currentAuthorId);
        ResponseEntity<String> response = restTemplate.getForEntity(BASE_URL + "/" + currentAuthorId, String.class);
        Assert.assertEquals((int) statusCode, response.getStatusCode().value());
    }

    private Long extractId(String body) {
        try {
            JsonNode jsonNode = objectMapper.readTree(body);
            JsonNode idNode = jsonNode.get("id");
            return idNode == null || idNode.isNull() ? null : idNode.asLong();
        } catch (Exception exception) {
            throw new AssertionError("Failed to parse id from response body", exception);
        }
    }

    private String extractName(String body) {
        try {
            JsonNode jsonNode = objectMapper.readTree(body);
            JsonNode nameNode = jsonNode.get("name");
            return nameNode == null || nameNode.isNull() ? null : nameNode.asText();
        } catch (Exception exception) {
            throw new AssertionError("Failed to parse name from response body", exception);
        }
    }

    private int extractArraySize(String body) {
        try {
            JsonNode jsonNode = objectMapper.readTree(body);
            return jsonNode.isArray() ? jsonNode.size() : 0;
        } catch (Exception exception) {
            throw new AssertionError("Failed to parse array response body", exception);
        }
    }

    private boolean responseArrayContainsName(String body, String expectedName) {
        try {
            JsonNode jsonNode = objectMapper.readTree(body);
            if (!jsonNode.isArray()) {
                return false;
            }
            for (JsonNode authorNode : jsonNode) {
                JsonNode nameNode = authorNode.get("name");
                if (nameNode != null && expectedName.equals(nameNode.asText())) {
                    return true;
                }
            }
            return false;
        } catch (Exception exception) {
            throw new AssertionError("Failed to parse array response body", exception);
        }
    }
}
