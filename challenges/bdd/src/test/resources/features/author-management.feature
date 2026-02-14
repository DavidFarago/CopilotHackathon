Feature: Author management via REST API
  As a user
  I want to interact with the Author REST API endpoints
  So that I can manage authors in the system

  Background:
    Given the author repository is empty

  Scenario: Create a new author
    When I create an author with name "Jane Austen"
    Then the response status should be 200
    And the response should contain an author id
    And the response should contain author name "Jane Austen"

  Scenario: Retrieve all authors
    Given an author exists with name "George Orwell"
    And an author exists with name "Virginia Woolf"
    When I request all authors
    Then the response status should be 200
    And the response should contain 2 authors
    And the response should include an author named "George Orwell"
    And the response should include an author named "Virginia Woolf"

  Scenario: Retrieve an author by id
    Given an author exists with name "Toni Morrison"
    When I request that author by id
    Then the response status should be 200
    And the response should contain author name "Toni Morrison"

  Scenario: Return 404 when retrieving an author that does not exist
    Given a nonexistent author id
    When I request that author by id
    Then the response status should be 404

  Scenario: Update an existing author
    Given an author exists with name "Mark Twain"
    When I update that author name to "Samuel Clemens"
    Then the response status should be 200
    And the response should contain author name "Samuel Clemens"

  Scenario: Return 404 when updating an author that does not exist
    Given a nonexistent author id
    When I update that author name to "Unknown Author"
    Then the response status should be 404

  Scenario: Delete an existing author
    Given an author exists with name "Octavia Butler"
    When I delete that author
    Then the response status should be 200
    And requesting that author by id should return 404

  Scenario: Return 404 when deleting an author that does not exist
    Given a nonexistent author id
    When I delete that author
    Then the response status should be 404
